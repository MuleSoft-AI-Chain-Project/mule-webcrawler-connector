package org.mule.extension.webcrawler.internal.crawler.mule;

import org.apache.maven.model.Site;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.constant.Constants.RegexUrlsFilterLogic;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.util.URLUtils;
import org.mule.extension.webcrawler.internal.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class MuleCrawler extends Crawler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleCrawler.class);

  private static final String CRAWLED_IMAGES_FOLDER = "images/";
  private static final String CRAWLED_DOCUMENTS_FOLDER = "docs/";

  public MuleCrawler(WebCrawlerConfiguration configuration, WebCrawlerConnection connection, String originalUrl, Long waitOnPageLoad,
                     String waitForXPath, boolean extractShadowDom, String shadowHostXPath, int maxDepth, boolean restrictToPath,
                     boolean downloadImages, int maxImageNumber, boolean downloadDocuments, int maxDocumentNumber, String downloadPath,
                     List<String> contentTags, Constants.OutputFormat outputFormat, boolean getMetaTags,
                     RegexUrlsFilterLogic regexUrlsFilterLogic, List<String> regexUrls) {

    super(configuration, connection, originalUrl, waitOnPageLoad, waitForXPath,  extractShadowDom, shadowHostXPath,
          maxDepth, restrictToPath, downloadImages, maxImageNumber, downloadDocuments, maxDocumentNumber, downloadPath,
          contentTags, outputFormat, getMetaTags, regexUrlsFilterLogic, regexUrls);
  }

  @Override
  public SiteNode crawl() {

    siteNodeQueue = new LinkedList<>();
    visitedLinksGlobal = new HashSet<>();

    String rootURLCleaned = URLUtils.removeFragment(rootURL);

    SiteNode rootNode = new SiteNode(rootURLCleaned, 0, connection.getReferrer());
    siteNodeQueue.add(rootNode);
    visitedLinksGlobal.add(rootURLCleaned);

    while(!siteNodeQueue.isEmpty()) {

      try {

        SiteNode currentNode = siteNodeQueue.poll();

        if(configuration.getCrawlerOptions().isEnforceRobotsTxt() && !PageHelper.canCrawl(currentNode.getUrl(), connection.getUserAgent())) {
          LOGGER.debug("SKIPPING url due to robots.txt: " + currentNode.getUrl());
          return null;
        }

        LOGGER.debug("CRAWLING url: " + currentNode.getUrl());

        // add delay
        Utils.addDelay(configuration.getCrawlerOptions().getDelayMillis());

        Document document = PageHelper.getDocument(configuration, connection, currentNode.getUrl(), currentNode.getReferrer(),
                                                   new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

        // Create Map to hold all data for the current page - this will be serialized to
        // JSON and saved to file
        JSONObject pageData = new JSONObject();

        pageData.put("url", currentNode.getUrl());
        pageData.put("title", document.title());

        if (downloadImages) {

          LOGGER.debug("Downloading images for : " + currentNode.getUrl());
          pageData.put("imageFiles", PageHelper.downloadWebsiteImages(document, downloadPath, CRAWLED_IMAGES_FOLDER, maxImageNumber));
        }

        if (downloadDocuments) {

          LOGGER.debug("Downloading documents for : " + currentNode.getUrl());
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
        String filename = PageHelper.savePageContents(pageData, downloadPath, document.title());

        // Update filename in the current node
        currentNode.setFilename(filename);

        // If not at max depth, find and crawl the links on the page
        if (currentNode.getCurrentDepth() < maxDepth) {

          // get all links on the current page
          Set<String> links = getPageLinks(document);
          if (links != null && !links.isEmpty()) {

            LOGGER.debug(String.format("Found %d links on page: %s", links.size(), currentNode.getUrl()));

            for (String childURL : links) {

              String childURLCleaned = URLUtils.removeFragment(childURL);

              // Check if this URL has already been visited at this depth
              if (!visitedLinksGlobal.contains(childURLCleaned)) {

                visitedLinksGlobal.add(childURLCleaned);
                SiteNode childNode = new SiteNode(childURLCleaned, currentNode.getCurrentDepth() + 1, currentNode.getUrl());
                siteNodeQueue.add(childNode);
                currentNode.getChildren().add(childNode);
              }
            }
          }
        }
      } catch (Exception e) {
        LOGGER.error(e.toString());
      }
    }
    return rootNode;
  }

  @Override
  public SiteNode map() {

    siteNodeQueue = new LinkedList<>();
    visitedLinksGlobal = new HashSet<>();

    String rootURLCleaned = URLUtils.removeFragment(rootURL);

    SiteNode rootNode = new SiteNode(rootURLCleaned, 0, connection.getReferrer());
    siteNodeQueue.add(rootNode);
    visitedLinksGlobal.add(rootURLCleaned);

    if(maxDepth == 0) {

      if (!PageHelper.isURLValid(configuration, connection, rootNode.getUrl(), rootNode.getReferrer())) {

        return null;
      }
    }

    while(!siteNodeQueue.isEmpty()) {

      SiteNode currentNode = null;
      try {

        currentNode = siteNodeQueue.poll();

        if(configuration.getCrawlerOptions().isEnforceRobotsTxt() && !PageHelper.canCrawl(currentNode.getUrl(), connection.getUserAgent())) {
          LOGGER.debug("SKIPPING url due to robots.txt: " + currentNode.getUrl());
          return null;
        }

        LOGGER.debug("MAPPING url: " + currentNode.getUrl());

        // add delay
        Utils.addDelay(configuration.getCrawlerOptions().getDelayMillis());

        if(currentNode.getCurrentDepth() == maxDepth) {

          if(PageHelper.isURLValid(configuration, connection, currentNode.getUrl(), currentNode.getReferrer())) {

            // Add as child to parent node only if valid
            SiteNode parentNode = currentNode.getParent();
            if(parentNode != null) parentNode.addChild(currentNode);
          } else {

            LOGGER.debug(String.format("SKIPPING %s due to invalid URL", currentNode.getUrl()));
          }
        }

        // If not at max depth, find and crawl the links on the page
        if (currentNode.getCurrentDepth() < maxDepth) {

          Document document = PageHelper.getDocument(configuration, connection, currentNode.getUrl(), currentNode.getReferrer(),
             new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

          // Add as child to parent node only if valid
          SiteNode parentNode = currentNode.getParent();
          if(parentNode != null) parentNode.addChild(currentNode);

          // get all links on the current page
          Set<String> links = getPageLinks(document);
          if (links != null && !links.isEmpty()) {

            LOGGER.debug(String.format("Found %d links on page: %s", links.size(), currentNode.getUrl()));

            for (String childURL : links) {

              String childURLCleaned = URLUtils.removeFragment(childURL);

              // Check if this URL has already been visited at this depth
              if (!visitedLinksGlobal.contains(childURLCleaned)) {

                visitedLinksGlobal.add(childURLCleaned);
                SiteNode childNode = new SiteNode(childURLCleaned,
                                                  currentNode.getCurrentDepth() + 1,
                                                  currentNode.getUrl(),
                                                  currentNode);

                siteNodeQueue.add(childNode);
              }
            }
          }
        }
      } catch (Exception e) {
        LOGGER.error(e.toString());
        if(currentNode == null || currentNode.getCurrentDepth() == 0) {

            return null;
        }
      }
    }
    return rootNode;
  }

  private Set<String> getPageLinks(Document document) {

    Map<String, Object> pageInsights = (Map<String, Object>)
        PageHelper.getPageInsights(
            document,
            null,
            restrictToPath ? Constants.PageInsightType.INTERNALLINKS : Constants.PageInsightType.ALL,
            regexUrlsFilterLogic,
            regexUrls
        );

    return getPageLinks(pageInsights);
  }

  private Set<String> getPageLinks(Map<String, Object> pageInsights) {

    // get all links on the current page
    Set<String> links = new HashSet<>();

    if(restrictToPath) {

      Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

      if (linksMap != null) {
        links = (Set<String>) linksMap.get("internal"); // Cast to Set<String>
      }
    } else {

      Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

      if (linksMap != null) {
        links.addAll((Set<String>) linksMap.get("internal"));
        links.addAll((Set<String>) linksMap.get("external"));
        links.addAll((Set<String>) linksMap.get("iframe"));
      }
    }
    return links;
  }

  private Set<String> getDocumentLinks(Document document) {

    // get all links on the current page
    Set<String> links = new HashSet<>();

    Map<String, Object> pageInsights = (Map<String, Object>)
        PageHelper.getPageInsights(
            document,
            null,
            Constants.PageInsightType.DOCUMENTLINKS,
            null,
            null
        );

    Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");

    if (linksMap != null) {
      links.addAll((Set<String>) linksMap.get("documents"));
    }
    return links;
  }

  @Override
  public DocumentIterator documentIterator() {
    return new DocumentIterator();
  }

  public class DocumentIterator extends Crawler.DocumentIterator {

    public DocumentIterator () {

        super();

        String rootURLCleaned = URLUtils.removeFragment(rootURL);

        visitedLinksGlobal = new HashSet<>();

        // Mark the URL as visited for this depth
        visitedLinksGlobal.add(rootURLCleaned);

        if(rootURLCleaned != null) {
          siteNodeQueue.add(new SiteNode(rootURLCleaned, 0, connection.getReferrer()));
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

        if(configuration.getCrawlerOptions().isEnforceRobotsTxt() && !PageHelper.canCrawl(currentNode.getUrl(), connection.getUserAgent())) {

          LOGGER.debug(String.format("SKIPPING %s due to robots.txt.", rootURL));
          return null;
        }

        document = PageHelper.getDocument(configuration, connection, currentNode.getUrl(), currentNode.getReferrer(),
            new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

        if(currentNode.getCurrentDepth() < maxDepth) {

          // get all links on the current page
          Set<String> links = getPageLinks(document);
          if (links != null && !links.isEmpty()) {

            LOGGER.debug(String.format("Found %d links on page: %s", links.size(), currentNode.getUrl()));

            for (String childURL : links) {

              String childURLCleaned = URLUtils.removeFragment(childURL);

              // Check if this URL has already been visited at this depth
              if (!visitedLinksGlobal.contains(childURLCleaned)) {

                visitedLinksGlobal.add(childURLCleaned);
                siteNodeQueue.add(new SiteNode(childURLCleaned, currentNode.getCurrentDepth() + 1, currentNode.getUrl()));
              }
            }
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return document;
    }
  }
}
