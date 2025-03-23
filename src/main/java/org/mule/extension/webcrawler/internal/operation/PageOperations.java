package org.mule.extension.webcrawler.internal.operation;

import org.json.JSONArray;
import org.mule.extension.webcrawler.api.metadata.PageResponseAttributes;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.PageTargetContentParameters;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
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
public class PageOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageOperations.class);

  /**
   * Fetch the meta tags from a web page.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("page-meta-tags")
  @DisplayName("[Page] Get meta tags")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetMetaTags.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, PageResponseAttributes>
      getMetaTags(
      @Config WebCrawlerConfiguration configuration,
      @ConfigOverride
          @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
          @Placement(order = 1, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
      @ConfigOverride
          @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
      @ConfigOverride
          @Alias("extractShadowDom") @DisplayName("Extract Shadow DOM") @Summary("Extract the Shadow DOM content (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Optional boolean extractShadowDom,
      @ConfigOverride
          @Alias("shadowHostXPath") @DisplayName("Shadow Host(s) XPath") @Summary("Shadow host(s) to extract by XPath (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//results") @Optional String shadowHostXPath,
      @Connection WebCrawlerConnection connection,
      @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Get meta tags");

      if(configuration.getCrawlerOptions().isEnforceRobotsTxt() &&
          !PageHelper.canCrawl(url, connection.getUserAgent())) {

        throw new ModuleException(
            String.format("The URL '%s' is not allowed to be crawled based on robots.txt.", url),
            WebCrawlerErrorType.CRAWL_ON_PAGE_DISALLOWED_ERROR);
      }

      Document document = PageHelper.getDocument(configuration, connection, url,
         new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

      LOGGER.debug(String.format("Returning page meta tags for url %s", url));

      return ResponseHelper.createPageResponse(
          PageHelper.getPageMetaTags(document).toString(),
          new HashMap<String, Object>() {{
            put("url", url);
            put("title", document.title());
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
   * Download all images from a web page, or download a single image at the
   * specified link.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("page-download-image")
  @DisplayName("[Page] Download image")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageDownloadImage.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, PageResponseAttributes>
      downloadWebsiteImages(
          @Config WebCrawlerConfiguration configuration,
          @ConfigOverride
              @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
              @Placement(order = 1, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
          @ConfigOverride
              @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
          @ConfigOverride
              @Alias("extractShadowDom") @DisplayName("Extract Shadow DOM") @Summary("Extract the Shadow DOM content (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Optional boolean extractShadowDom,
          @ConfigOverride
              @Alias("shadowHostXPath") @DisplayName("Shadow Host(s) XPath") @Summary("Shadow host(s) to extract by XPath (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//results") @Optional String shadowHostXPath,
          @Connection WebCrawlerConnection connection,
          @DisplayName("Page or image URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @Alias("maxImageNumber") @DisplayName("Max number of images")
              @Summary("Maximum number of images to download. Default 0 means no limit.")
              @Placement(order = 2) @Expression(ExpressionSupport.SUPPORTED) @Example("10")
              @Optional int maxImageNumber,
          @DisplayName("Download location") @Placement(order = 3) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray imagesJSONArray = new JSONArray();

      Document document = null;

      try {

        if(configuration.getCrawlerOptions().isEnforceRobotsTxt() &&
            !PageHelper.canCrawl(url, connection.getUserAgent())) {

          throw new ModuleException(
              String.format("The URL '%s' is not allowed to be crawled based on robots.txt.", url),
              WebCrawlerErrorType.CRAWL_ON_PAGE_DISALLOWED_ERROR);
        }

        document = PageHelper.getDocument(configuration, connection, url,
            new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));


        imagesJSONArray = PageHelper.downloadWebsiteImages(document, downloadPath, maxImageNumber);

      } catch (UnsupportedMimeTypeException e) {
        // url provided is direct link to image, so download single image

        imagesJSONArray.put(PageHelper.downloadSingleImage(url, downloadPath));
      }

      HashMap<String, Object> attributes = new HashMap<String, Object>() {{
        put("url", url);
      }};

      if(document != null) attributes.put("title", document.title());

      return ResponseHelper.createPageResponse(
          imagesJSONArray.toString(),
          attributes
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
  @Alias("page-download-document")
  @DisplayName("[Page] Download document")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageDownloadDocument.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, PageResponseAttributes>
  downloadWebsiteDocuments(
      @Config WebCrawlerConfiguration configuration,
      @ConfigOverride
          @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
          @Placement(order = 1, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
      @ConfigOverride
          @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
      @ConfigOverride
          @Alias("extractShadowDom") @DisplayName("Extract Shadow DOM") @Summary("Extract the Shadow DOM content (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Optional boolean extractShadowDom,
      @ConfigOverride
          @Alias("shadowHostXPath") @DisplayName("Shadow Host(s) XPath") @Summary("Shadow host(s) to extract by XPath (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//results") @Optional String shadowHostXPath,
      @Connection WebCrawlerConnection connection,
      @DisplayName("Page or document URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @Alias("maxDocumentNumber") @DisplayName("Max number of documents")
          @Summary("Maximum number of documents to download. Default 0 means no limit.")
          @Placement(order = 2) @Expression(ExpressionSupport.SUPPORTED) @Example("10")
          @Optional int maxDocumentNumber,
      @DisplayName("Download location") @Placement(order = 3) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      if(configuration.getCrawlerOptions().isEnforceRobotsTxt() &&
          !PageHelper.canCrawl(url, connection.getUserAgent())) {

        throw new ModuleException(
            String.format("The URL '%s' is not allowed to be crawled based on robots.txt.", url),
            WebCrawlerErrorType.CRAWL_ON_PAGE_DISALLOWED_ERROR);
      }

      JSONArray documentsJSONArray = new JSONArray();

      Document document = null;

      try {
        document = PageHelper.getDocument(configuration, connection, url,
            new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

        documentsJSONArray = PageHelper.downloadFiles(document, downloadPath, maxDocumentNumber);

      } catch (UnsupportedMimeTypeException e) {
        // url provided is direct link to image, so download single image

        documentsJSONArray.put(PageHelper.downloadFile(url, downloadPath));
      }

      HashMap<String, Object> attributes = new HashMap<String, Object>() {{
        put("url", url);
      }};

      if(document != null) attributes.put("title", document.title());

      return ResponseHelper.createPageResponse(
          documentsJSONArray.toString(),
          attributes
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
  @Alias("page-insights")
  @DisplayName("[Page] Get insights")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetInsights.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, PageResponseAttributes>
      getPageInsights(
          @Config WebCrawlerConfiguration configuration,
          @ConfigOverride
              @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
              @Placement(order = 1, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
          @ConfigOverride
              @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
          @ConfigOverride
              @Alias("extractShadowDom") @DisplayName("Extract Shadow DOM") @Summary("Extract the Shadow DOM content (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Optional boolean extractShadowDom,
          @ConfigOverride
              @Alias("shadowHostXPath") @DisplayName("Shadow Host(s) XPath") @Summary("Shadow host(s) to extract by XPath (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//results") @Optional String shadowHostXPath,
          @Connection WebCrawlerConnection connection,
          @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @ParameterGroup(name="Target Content") PageTargetContentParameters targetContentParameters) {

    try {

      LOGGER.info("Analyze page");

      if(configuration.getCrawlerOptions().isEnforceRobotsTxt() &&
          !PageHelper.canCrawl(url, connection.getUserAgent())) {

        throw new ModuleException(
            String.format("The URL '%s' is not allowed to be crawled based on robots.txt.", url),
            WebCrawlerErrorType.CRAWL_ON_PAGE_DISALLOWED_ERROR);
      }

      Document document = PageHelper.getDocument(configuration, connection, url,
          new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

      return ResponseHelper.createPageResponse(
          JSONUtils.convertToJSON(
              PageHelper.getPageInsights(document, targetContentParameters.getTags(), Constants.PageInsightType.ALL)
          ),
          new HashMap<String, Object>() {{
            put("url", url);
            put("title", document.title());
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
  @Alias("page-content")
  @DisplayName("[Page] Get content")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetContent.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, PageResponseAttributes>
      getPageContent(
          @Config WebCrawlerConfiguration configuration,
          @ConfigOverride
              @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
              @Placement(order = 1, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
          @ConfigOverride
              @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
          @ConfigOverride
              @Alias("extractShadowDom") @DisplayName("Extract Shadow DOM") @Summary("Extract the Shadow DOM content (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Optional boolean extractShadowDom,
          @ConfigOverride
              @Alias("shadowHostXPath") @DisplayName("Shadow Host(s) XPath") @Summary("Shadow host(s) to extract by XPath (not available for HTTP connection)")
              @Placement(order = 2, tab = "Page Load Options (WebDriver)") @Expression(ExpressionSupport.SUPPORTED) @Example("//results") @Optional String shadowHostXPath,
          @Connection WebCrawlerConnection connection,
          @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @DisplayName("Output format") @Placement(order = 2) Constants.OutputFormat outputFormat,
          @ParameterGroup(name="Target Content") PageTargetContentParameters targetContentParameters) {

    try {

      LOGGER.info("Get page content");

      if(configuration.getCrawlerOptions().isEnforceRobotsTxt() &&
          !PageHelper.canCrawl(url, connection.getUserAgent())) {

        throw new ModuleException(
            String.format("The URL '%s' is not allowed to be crawled based on robots.txt.", url),
            WebCrawlerErrorType.CRAWL_ON_PAGE_DISALLOWED_ERROR);
      }

      Map<String, String> contents = new HashMap<String, String>();

      Document document = PageHelper.getDocument(configuration, connection, url,
          new PageLoadOptions(waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath));

      String content = PageHelper.getPageContent(document,
                                                 targetContentParameters.getTags(),
                                                 outputFormat);

      contents.put("url", document.baseUri());
      contents.put("title", document.title());
      contents.put("content", content);

      return ResponseHelper.createPageResponse(
          JSONUtils.convertToJSON(contents),
          new HashMap<String, Object>() {{
            put("url", url);
            put("title", document.title());
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
}
