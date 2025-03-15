package org.mule.extension.webcrawler.internal.crawler.mule;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.constant.Constants.RegexUrlsFilterLogic;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class MuleCrawler extends Crawler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleCrawler.class);

  private static final String CRAWLED_IMAGES_FOLDER = "images/";
  private static final String CRAWLED_DOCUMENTS_FOLDER = "docs/";

  public MuleCrawler(WebCrawlerConfiguration configuration, WebCrawlerConnection connection, String originalUrl, Long waitDuration,
                     String waitUntilXPath, int maxDepth, boolean restrictToPath, boolean downloadImages, int maxImageNumber,
                     boolean downloadDocuments, int maxDocumentNumber, String downloadPath, List<String> contentTags,
                     Constants.OutputFormat outputFormat, boolean getMetaTags, RegexUrlsFilterLogic regexUrlsFilterLogic, List<String> regexUrls) {

    super(configuration, connection, originalUrl, waitDuration, waitUntilXPath, maxDepth, restrictToPath, downloadImages,
          maxImageNumber, downloadDocuments, maxDocumentNumber, downloadPath, contentTags, outputFormat, getMetaTags,
          regexUrlsFilterLogic, regexUrls);
  }

  @Override
  public SiteNode crawl() {

    visitedLinksGlobal = new HashSet<>();
    visitedLinksByDepth = new HashMap<>();
    return crawl(rootURL, 0, connection.getReferrer());
  }

  private SiteNode crawl(String url, int currentDepth, String referrer) {

    // return if maxDepth reached
    if (currentDepth > maxDepth) {
      return null;
    }

    if(configuration.getCrawlerOptions().isEnforceRobotsTxt() && !PageHelper.canCrawl(url, connection.getUserAgent())) {
      LOGGER.debug("SKIPPING due to robots.txt: " + url);
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
      Utils.addDelay(configuration.getCrawlerOptions().getDelayMillis());

      // Mark the URL as visited for this depth
      visitedLinksByDepth.get(currentDepth).add(url);

      SiteNode siteNode = null;

      // get page as a html document
      Document document = PageHelper.getDocument(configuration, connection, url, referrer, waitDuration, waitUntilXPath);

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
          pageData.put("imageFiles", PageHelper.downloadWebsiteImages(document, downloadPath, CRAWLED_IMAGES_FOLDER, maxImageNumber));
        }

        if (downloadDocuments) {

          LOGGER.debug("Downloading documents for : " + url);
          pageData.put("documentFiles", PageHelper.downloadFiles(document, downloadPath, CRAWLED_DOCUMENTS_FOLDER, maxDocumentNumber));
        }

        // get all meta tags from the document
        if (getMetaTags) {

          JSONArray pageMetaTags = PageHelper.getPageMetaTags(document);
          pageData.put("metaTags", pageMetaTags);
        }

        // get page contents
        pageData.put("content", PageHelper.getPageContent(document, contentTags, outputFormat));

        // save gathered data of page to file
        String filename = PageHelper.savePageContents(pageData, downloadPath, title);

        // Create a new pageNode for this URL
        siteNode = new SiteNode(url, currentDepth, filename);

      } else {
        // content previously downloaded, so setting file name as such
        siteNode = new SiteNode(url, currentDepth, "Duplicate.");
      }

      // If not at max depth, find and crawl the links on the page
      if (currentDepth <= maxDepth) {

        // get all links on the current page
        Set<String> links = getPageLinks(document);

        if (links != null) {
          for (String childURL : links) {

            // Recursively crawl the link and add as a child
            SiteNode childSiteNode = crawl(childURL, currentDepth + 1, url);
            if (childSiteNode != null) {

              siteNode.addChild(childSiteNode);
            }
          }
        }
      }
      return siteNode;

    } catch (Exception e) {
      LOGGER.error(e.toString());
    }
    return null;
  }

  @Override
  public SiteNode map() {

    visitedLinksGlobal = new HashSet<>();
    visitedLinksByDepth = new HashMap<>();
    return map(rootURL, 0, connection.getReferrer());
  }

  private SiteNode map(String url, int currentDepth, String referrer) {

    // return if maxDepth reached
    if (currentDepth > maxDepth) {
      return null;
    }

    if(configuration.getCrawlerOptions().isEnforceRobotsTxt() && !PageHelper.canCrawl(url, connection.getUserAgent())) {
      LOGGER.debug("SKIPPING due to robots.txt: " + url);
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

    // map current page
    try {

      // add delay
      Utils.addDelay(configuration.getCrawlerOptions().getDelayMillis());

      // Mark the URL as visited for this depth
      visitedLinksByDepth.get(currentDepth).add(url);

      SiteNode node = null;

      // get page as a html document
      Document document = PageHelper.getDocument(configuration, connection, url, referrer, waitDuration, waitUntilXPath);

      node = new SiteNode(url, currentDepth, referrer);
      LOGGER.debug("Found url: " + url);

      // If not at max depth, find and crawl the links on the page
      if (currentDepth <= maxDepth) {

        // get all links on the current page
        Set<String> links = getPageLinks(document);

        if (links != null) {

          LOGGER.debug(String.format("Found %d links on page: %s", links.size(), url));

          for (String childURL : links) {

            SiteNode childNode;

            // Recursively crawl the link and add as a child
            childNode = currentDepth < maxDepth ?
                map(childURL, currentDepth +1, url) :
                new SiteNode(childURL, currentDepth +1, url);

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

  private Set<String> getPageLinks(Document document) {

    // get all links on the current page
    Set<String> links = new HashSet<>();

    if(restrictToPath) {

      Map<String, Object> pageInsights = (Map<String, Object>)
          PageHelper.getPageInsights(document, null, Constants.PageInsightType.INTERNALLINKS, regexUrlsFilterLogic, regexUrls);
      Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

      if (linksMap != null) {
        links = (Set<String>) linksMap.get("internal"); // Cast to Set<String>
      }
    } else {

      Map<String, Object> pageInsights = (Map<String, Object>)
          PageHelper.getPageInsights(document, null, Constants.PageInsightType.ALL, regexUrlsFilterLogic, regexUrls);
      Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

      if (linksMap != null) {
        links.addAll((Set<String>) linksMap.get("internal"));
        links.addAll((Set<String>) linksMap.get("external"));
        links.addAll((Set<String>) linksMap.get("iframe"));
      }
    }
    return links;
  }

  @Override
  public DocumentIterator documentIterator() {
    return new DocumentIterator();
  }

  public class DocumentIterator extends Crawler.DocumentIterator {

    private final Queue<SiteNode> siteNodeQueue = new LinkedList<>();

    public DocumentIterator () {

        super();

        visitedLinksGlobal = new HashSet<>();

        if(rootURL != null) {
          siteNodeQueue.add(new SiteNode(rootURL, 0, connection.getReferrer()));
        } else {
          throw new IllegalArgumentException("Root URL cannot be null.");
        }
    }

    // Override hasNext to check if there are files left to process
    @Override
    public boolean hasNext() {

      return !siteNodeQueue.isEmpty();
    }

    // Override next to return the next document
    @Override
    public Document next() {

      if (!hasNext()) {
        throw new NoSuchElementException("No more documents to iterate.");
      }
      SiteNode currentNode = siteNodeQueue.poll();
      Document document = null;
      try {


        // Check if this URL has already been visited at this depth
        if (visitedLinksGlobal.contains(currentNode.getUrl())) {

          LOGGER.debug(String.format("SKIPPING %s since already visited.", rootURL));
          return null;
        }

        if(configuration.getCrawlerOptions().isEnforceRobotsTxt() && !PageHelper.canCrawl(currentNode.getUrl(), connection.getUserAgent())) {

          LOGGER.debug(String.format("SKIPPING %s due to robots.txt.", rootURL));
          return null;
        }

        document = PageHelper.getDocument(configuration, connection, currentNode.getUrl(), currentNode.getReferrer(), waitDuration, waitUntilXPath);

        if(currentNode.getCurrentDepth() < maxDepth) {

          // get all links on the current page
          Set<String> links = getPageLinks(document);
          if (links != null && !links.isEmpty()) {

            LOGGER.debug(String.format("Found %d links on page: %s", links.size(), currentNode.getUrl()));

            for (String childURL : links) {

              siteNodeQueue.add(new SiteNode(childURL, currentNode.getCurrentDepth() + 1, currentNode.getUrl()));
            }
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      visitedLinksGlobal.add(currentNode.getUrl());
      return document;
    }
  }
}
