package org.mule.extension.webcrawler.internal.operation;

import org.json.JSONArray;
import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.Configuration;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.json.JSONObject;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.search.SerperDev;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class Operations {

  private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);

  /**
   * Crawl a website at a specified depth and fetch contents. Specify tags and
   * classes in the configuration to fetch contents from those elements only.
   *
   * @throws IOException
   */

  /*
   * JSoup limitiations / web crawl challenges
   * - some sites prevent robots - use of User-Agent may be required but not
   * always guaranteed to work
   * - JavaScript generated content is not read by jsoup
   * - some sites require cookies or sessions to be present
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Crawl-website")
  @DisplayName("[Crawl] Website")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/CrawlWebSite.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      crawlWebsite(@Config Configuration configuration,
          @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @DisplayName("Restrict Crawl under URL") @Placement(order = 2) @Example("False") boolean restrictToPath,
          @DisplayName("Dynamic Content Retrieval") @Placement(order = 3) @Example("False") boolean dynamicContent,
          @DisplayName("Maximum Depth") @Placement(order = 4) @Example("2") int maxDepth,
          @DisplayName("Delay (millisecs)") @Placement(order = 5) @Example("0") int delayMillis,
          @DisplayName("Retrieve Meta Tags") @Placement(order = 6) @Example("False") boolean getMetaTags,
          @DisplayName("Download Images") @Placement(order = 7) @Example("False") boolean downloadImages,
          @DisplayName("Download Documents") @Placement(order = 8) @Example("False") boolean downloadDocuments,
          @DisplayName("Download Location") @Placement(order = 9) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      LOGGER.debug("Initialize crawler");

      Crawler crawler = Crawler.builder()
          .rootURL(url)
          .maxDepth(maxDepth)
          .restrictToPath(restrictToPath)
          .dynamicContent(dynamicContent)
          .delayMillis(delayMillis)
          .downloadImages(downloadImages)
          .downloadDocuments(downloadDocuments)
          .downloadPath(downloadPath)
          .contentTags(configuration.getTags())
          .getMetaTags(getMetaTags)
          .build();

      LOGGER.debug("Start website crawling");

      Crawler.CrawlNode rootNode = crawler.crawl();

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(rootNode),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while crawling website '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Fetch the meta tags from a web page.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Get-page-meta-tags")
  @DisplayName("[Page] Get meta tags")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetMetaTags.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getMetaTags(
          @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Get meta tags");

      Document document = PageHelper.getDocument(url);

      return ResponseHelper.createResponse(
          PageHelper.getPageMetaTags(document).toString(),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while getting page meta tags from '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Retrieve internal links as a site map from the specified url and depth.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Generate-sitemap")
  @DisplayName("[Page] Get links as sitemap")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetLinksAsSitemap.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getSiteMap(
          @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @DisplayName("Maximum Depth") @Placement(order = 2) @Example("2") int maxDepth,
          @DisplayName("Delay (millisecs)") @Placement(order = 3) @Example("0") int delayMillis) {

    try{

      LOGGER.info("Generate sitemap");

      Crawler crawler = Crawler.builder()
          .rootURL(url)
          .maxDepth(maxDepth)
          .delayMillis(delayMillis)
          .build();

      Crawler.MapNode root = crawler.map();

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(root),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while generating sitemap for '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Download all images from a web page, or download a single image at the
   * specified link.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Download-image")
  @DisplayName("[Page] Download image")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageDownloadImage.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      downloadWebsiteImages(
          @DisplayName("Page Or Image URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @DisplayName("Download Location") @Placement(order = 2) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray imagesJSONArray = new JSONArray();

      try {
        // url provided is a website url, so download all images from this document
        Document document = PageHelper.getDocument(url);
        imagesJSONArray = PageHelper.downloadWebsiteImages(document, downloadPath);

      } catch (UnsupportedMimeTypeException e) {
        // url provided is direct link to image, so download single image

        imagesJSONArray.put(PageHelper.downloadSingleImage(url, downloadPath));
      }

      return ResponseHelper.createResponse(
          imagesJSONArray.toString(),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while downloading image from '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Download all documents from a web page, or download a single document at the
   * specified link.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Download-document")
  @DisplayName("[Page] Download document")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageDownloadDocument.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
  downloadWebsiteDocuments(
      @DisplayName("Page Or Document URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Download Location") @Placement(order = 2) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray documentsJSONArray = new JSONArray();

      try {
        // url provided is a website url, so download all images from this document
        Document document = PageHelper.getDocument(url);
        documentsJSONArray = PageHelper.downloadFiles(document, downloadPath);

      } catch (UnsupportedMimeTypeException e) {
        // url provided is direct link to image, so download single image

        documentsJSONArray.put(PageHelper.downloadFile(url, downloadPath));
      }

      return ResponseHelper.createResponse(
          documentsJSONArray.toString(),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while downloading document from '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Get insights from a web page including links, word count, number of
   * occurrences of elements. Restrict insights to specific elements in the
   * configuration.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Get-page-insights")
  @DisplayName("[Page] Get insights")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetInsights.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getPageInsights(
          @Config Configuration configuration,
          @DisplayName("Page Url") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Analyze page");

      Document document = PageHelper.getDocument(url);

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(
              PageHelper.getPageInsights(document, configuration.getTags(), Constants.PageInsightType.ALL)
          ),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while getting page insights from '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Get contents of a web page. Content is returned in the resulting payload.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Get-page-content")
  @DisplayName("[Page] Get content")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetContent.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getPageContent(
          @Config Configuration configuration,
          @DisplayName("Page Url") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Get page content");

      Map<String, String> contents = new HashMap<String, String>();

      Document document = PageHelper.getDocument(url);

      contents.put("url", document.baseUri());
      contents.put("title", document.title());
      contents.put("content", PageHelper.getPageContent(document, configuration.getTags()));

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(contents),
          new HashMap<String, Object>() {{
            put("url", url);
          }}
      );

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while getting page content from '%s'.", url),
          WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Perform a Google search using the SERP API.
   *
   * @throws IOException
   */
  @MediaType(value = MediaType.APPLICATION_JSON, strict = false)
  @Alias("Google-search")
  @DisplayName("[Search] Google")
  @Throws(WebCrawlerErrorTypeProvider.class)
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      googleSearch(
          @DisplayName("Search Query") @Placement(order = 1) @Example("apple inc") String query,
          @DisplayName("API Key") @Placement(order = 2) @Example("your_api_key_here") String apiKey) throws IOException {

    try {

      LOGGER.info("Performing Google search for query: " + query);

      String responseBody = SerperDev.search(query, apiKey);

      JSONObject jsonResponse = new JSONObject(responseBody);

      return ResponseHelper.createResponse(
          jsonResponse.toString(),
          new HashMap<String, Object>() {{
            put("query", query);
          }}
      );

    } catch (ModuleException me) {

      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while searching '%s'.", query),
          WebCrawlerErrorType.SEARCH_OPERATIONS_FAILURE,
          e);
    }
  }
}
