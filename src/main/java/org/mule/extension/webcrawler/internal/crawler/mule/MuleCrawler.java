package org.mule.extension.webcrawler.internal.crawler.mule;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MuleCrawler extends Crawler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleCrawler.class);

  private static final String CRAWLED_IMAGES_FOLDER = "images/";
  private static final String CRAWLED_DOCUMENTS_FOLDER = "docs/";

  public MuleCrawler(String userAgent, String referrer, String originalUrl, int maxDepth, boolean restrictToPath,
                     boolean dynamicContent, int delayMillis, boolean downloadImages, boolean downloadDocuments,
                     String downloadPath, List<String> contentTags, boolean getMetaTags) {

    super(userAgent, referrer, originalUrl, maxDepth, restrictToPath, dynamicContent, delayMillis, downloadImages,
          downloadDocuments, downloadPath, contentTags, getMetaTags);
  }

  @Override
  public CrawlNode crawl() {

    visitedLinksGlobal = new HashSet<>();
    visitedLinksByDepth = new HashMap<>();
    return crawl(rootURL, 0, rootReferrer);
  }

  private CrawlNode crawl(String url, int currentDepth, String referrer) {

    // return if maxDepth reached
    if (currentDepth > maxDepth) {
      return null;
    }

    if (restrictToPath) {
      // Restrict crawling to URLs under the original URL only
      if (!url.startsWith(rootURL)) {

        LOGGER.debug("SKIPPING due to strict crawling: " + url);
        return null;
      }
    }

    // Initialize the set for the current depth if not already present
    visitedLinksByDepth.putIfAbsent(currentDepth, new HashSet<>());

    // Check if this URL has already been visited at this depth
    if (visitedLinksByDepth.get(currentDepth).contains(url)) {
      return null;
    }

    // crawl & extract current page
    try {

      // add delay
      Utils.addDelay(delayMillis);

      // Mark the URL as visited for this depth
      visitedLinksByDepth.get(currentDepth).add(url);

      CrawlNode crawlNode = null;

      // get page as a html document
      Document document = null;
      if (dynamicContent) {
        document = PageHelper.getDocumentDynamic(url);
      }
      else {
        document = PageHelper.getDocument(url, userAgent, referrer);
      }

      // check if url contents have been downloaded before ie applied globally (at all
      // depths). Note, we don't want to do this globally for CrawlType.LINK because
      // we want a link to be unique only at the depth level and not globally (at all
      // depths)
      if (!visitedLinksGlobal.contains(url)) {

        // add url to urlContentFetched to indicate content has been fetched.
        visitedLinksGlobal.add(url);

        // Create Map to hold all data for the current page - this will be serialized to
        // JSON and saved to file
        JSONObject pageData = new JSONObject();

        LOGGER.debug("Fetching content for : " + url);

        String title = document.title();

        pageData.put("url", url);
        pageData.put("title", title);

        // check if need to download images in the current page
        if (downloadImages) {

          LOGGER.debug("Downloading images for : " + url);
          pageData.put("imageFiles", PageHelper.downloadWebsiteImages(document, downloadPath, CRAWLED_IMAGES_FOLDER));
        }

        if (downloadDocuments) {

          LOGGER.debug("Downloading documents for : " + url);
          pageData.put("documentFiles", PageHelper.downloadFiles(document, downloadPath, CRAWLED_DOCUMENTS_FOLDER));
        }

        // get all meta tags from the document
        if (getMetaTags) {

          JSONArray pageMetaTags = PageHelper.getPageMetaTags(document);
          pageData.put("metaTags", pageMetaTags);
        }

        // get page contents
        pageData.put("content", PageHelper.getPageContent(document, contentTags));

        // save gathered data of page to file
        String filename = PageHelper.savePageContents(pageData, downloadPath, title);

        // Create a new pageNode for this URL
        crawlNode = new CrawlNode(url, filename);

      } else {
        // content previously downloaded, so setting file name as such
        crawlNode = new CrawlNode(url, "Duplicate.");
      }

      // If not at max depth, find and crawl the links on the page
      if (currentDepth <= maxDepth) {
        // get all links on the current page
        Set<String> links = new HashSet<>();

        HashMap<String, Object> pageInsights = PageHelper.getPageInsights(document, null, Constants.PageInsightType.INTERNALLINKS);
        HashMap<String, Object> linksMap = (HashMap<String, Object>) pageInsights.get("links");

        if (linksMap != null) {
          links = (Set<String>) linksMap.get("internal"); // Cast to Set<String>
        }

        if (links != null) {
          for (String childURL : links) {

            // Recursively crawl the link and add as a child
            CrawlNode childPageNode = crawl(childURL, currentDepth + 1, url);
            if (childPageNode != null) {

              crawlNode.addChild(childPageNode);
            }
          }
        }
      }
      return crawlNode;

    } catch (Exception e) {
      LOGGER.error(e.toString());
    }
    return null;
  }

  @Override
  public MapNode map() {

    visitedLinksGlobal = new HashSet<>();
    visitedLinksByDepth = new HashMap<>();
    return map(rootURL, 0);
  }

  private MapNode map(String url, int currentDepth) {

    // return if maxDepth reached
    if (currentDepth > maxDepth) {
      return null;
    }

    // Initialize the set for the current depth if not already present
    visitedLinksByDepth.putIfAbsent(currentDepth, new HashSet<>());

    // Check if this URL has already been visited at this depth
    if (visitedLinksByDepth.get(currentDepth).contains(url)) {
      return null;
    }

    // map current page
    try {

      // add delay
      Utils.addDelay(delayMillis);

      // Mark the URL as visited for this depth
      visitedLinksByDepth.get(currentDepth).add(url);

      MapNode node = null;

      // get page as a html document
      Document document = PageHelper.getDocument(url, userAgent, rootReferrer);

      node = new MapNode(url);
      LOGGER.debug("Found url: " + url);

      // If not at max depth, find and crawl the links on the page
      if (currentDepth <= maxDepth) {
        // get all links on the current page
        Set<String> links = new HashSet<>();

        if(restrictToPath) {

          Map<String, Object> pageInsights = (Map<String, Object>)
              PageHelper.getPageInsights(document, null, Constants.PageInsightType.INTERNALLINKS);
          Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

          if (linksMap != null) {
            links = (Set<String>) linksMap.get("internal"); // Cast to Set<String>
          }
        } else {

          Map<String, Object> pageInsights = (Map<String, Object>)
              PageHelper.getPageInsights(document, null, Constants.PageInsightType.ALL);
          Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

          if (linksMap != null) {
            links.addAll((Set<String>) linksMap.get("internal"));
            links.addAll((Set<String>) linksMap.get("external"));
          }
        }

        if (links != null) {
          for (String childURL : links) {

            MapNode childNode;

            // Recursively crawl the link and add as a child
            childNode = currentDepth < maxDepth ?
                map(childURL, currentDepth +1) :
                new MapNode(childURL);

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
