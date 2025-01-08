package org.mule.extension.webcrawler.internal.helper.crawler;

import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.page.SiteMapNode;
import org.mule.extension.webcrawler.internal.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CrawlerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerHelper.class);

    public static SiteMapNode crawl(String url, String originalUrl, int depth, int maxDepth, boolean restrictToPath,
                                    boolean dynamicContent, int delayMillis, Map<Integer, Set<String>> visitedLinksByDepth,
                                    Set<String> visitedLinksGlobal, boolean downloadImages, String downloadPath,
                                    List<String> contentTags, boolean getMetaTags, Constants.CrawlType crawlType) {

        // return if maxDepth reached
        if (depth > maxDepth) {
            return null;
        }

        if (restrictToPath) {
            // Restrict crawling to URLs under the original URL only
            if (!url.startsWith(originalUrl)) {
                LOGGER.info("SKIPPING due to strict crawling: " + url);
                return null;
            }
        }

        // Initialize the set for the current depth if not already present
        visitedLinksByDepth.putIfAbsent(depth, new HashSet<>());

        // Check if this URL has already been visited at this depth
        if (visitedLinksByDepth.get(depth).contains(url)) {
            return null;
        }

        // crawl & extract current page
        try {

            // add delay
            Utils.addDelay(delayMillis);

            // Mark the URL as visited for this depth
            visitedLinksByDepth.get(depth).add(url);

            SiteMapNode node = null;

            // get page as a html document
            Document document = null;
            if (dynamicContent) {
                document = PageHelper.getDocumentDynamic(url);
            }
            else {
                document = PageHelper.getDocument(url);
            }

            // check if url contents have been downloaded before ie applied globally (at all
            // depths). Note, we don't want to do this globally for CrawlType.LINK because
            // we want a link to be unique only at the depth level and not globally (at all
            // depths)
            if (!visitedLinksGlobal.contains(url) && crawlType == Constants.CrawlType.CONTENT) {

                // add url to urlContentFetched to indicate content has been fetched.
                visitedLinksGlobal.add(url);

                // Create Map to hold all data for the current page - this will be serialized to
                // JSON and saved to file
                Map<String, Object> pageData = new HashMap<>();

                LOGGER.info("Fetching content for : " + url);

                String title = document.title();

                pageData.put("url", url);
                pageData.put("title", title);

                // check if need to download images in the current page
                if (downloadImages) {
                    LOGGER.info("Downloading images for : " + url);
                    pageData.put("imageFiles", PageHelper.downloadWebsiteImages(document, downloadPath));
                }

                // get all meta tags from the document
                if (getMetaTags) {
                    // Iterating over each entry in the map
                    for (Map.Entry<String, String> entry : PageHelper.getPageMetaTags(document).entrySet()) {
                        pageData.put(entry.getKey(), entry.getValue());
                    }
                }

                // get page contents
                pageData.put("content", PageHelper.getPageContent(document, contentTags));

                // save gathered data of page to file
                String filename = PageHelper.savePageContents(pageData, downloadPath, title);

                // Create a new node for this URL
                node = new CrawlResult(url, filename);

            } else if (crawlType == Constants.CrawlType.LINK) {
                node = new SiteMapNode(url);
                LOGGER.info("Found url : " + url);
            } else {
                // content previously downloaded, so setting file name as such
                node = new CrawlResult(url, "Duplicate.");
            }

            // If not at max depth, find and crawl the links on the page
            if (depth <= maxDepth) {
                // get all links on the current page
                Set<String> links = new HashSet<>();

                Map<String, Object> linksMap = (Map<String, Object>) PageHelper
                    .getPageInsights(document, null, Constants.PageInsightType.INTERNALLINKS).get("links");

                if (linksMap != null) {
                    links = (Set<String>) linksMap.get("internal"); // Cast to Set<String>
                }

                if (links != null) {
                    for (String nextUrl : links) {

                        // Recursively crawl the link and add as a child
                        SiteMapNode childNode = crawl(nextUrl, originalUrl, depth + 1, maxDepth, restrictToPath, dynamicContent, delayMillis, visitedLinksByDepth, visitedLinksGlobal,
                                                      downloadImages, downloadPath, contentTags, getMetaTags, crawlType);
                        if (childNode != null) {
                            node.addChild(childNode);
                        }
                    }
                }
            }
            return node;
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
        return null;
    }
}
