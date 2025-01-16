package org.mule.extension.webcrawler.internal.operation;

import org.json.JSONArray;
import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.PageConfiguration;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.PageTargetsParameters;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
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
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getMetaTags(
      @Config PageConfiguration configuration,
      @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Get meta tags");

      Document document = PageHelper.getDocument(
          url,
          configuration.getRequestParameters().getUserAgent(),
          configuration.getRequestParameters().getReferrer());

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
   * Download all images from a web page, or download a single image at the
   * specified link.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("page-download-image")
  @DisplayName("[Page] Download image")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageDownloadImage.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      downloadWebsiteImages(
          @Config PageConfiguration configuration,
          @DisplayName("Page Or Image URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @DisplayName("Download Location") @Placement(order = 2) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray imagesJSONArray = new JSONArray();

      try {
        // url provided is a website url, so download all images from this document
        Document document = PageHelper.getDocument(
            url,
            configuration.getRequestParameters().getUserAgent(),
            configuration.getRequestParameters().getReferrer());

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
  @Alias("page-download-document")
  @DisplayName("[Page] Download document")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageDownloadDocument.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
  downloadWebsiteDocuments(
      @Config PageConfiguration configuration,
      @DisplayName("Page Or Document URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Download Location") @Placement(order = 2) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray documentsJSONArray = new JSONArray();

      try {
        // url provided is a website url, so download all images from this document
        Document document = PageHelper.getDocument(
            url,
            configuration.getRequestParameters().getUserAgent(),
            configuration.getRequestParameters().getReferrer());

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
  @Alias("page-insights")
  @DisplayName("[Page] Get insights")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetInsights.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getPageInsights(
          @Config PageConfiguration configuration,
          @DisplayName("Page Url") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Analyze page");

      Document document = PageHelper.getDocument(
          url,
          configuration.getRequestParameters().getUserAgent(),
          configuration.getRequestParameters().getReferrer());

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
  @Alias("page-content")
  @DisplayName("[Page] Get content")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/PageGetContent.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getPageContent(
          @Config PageConfiguration configuration,
          @DisplayName("Page Url") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @ParameterGroup(name="Targets") PageTargetsParameters pageTargetsParameters) {

    try {

      LOGGER.info("Get page content");

      Map<String, String> contents = new HashMap<String, String>();

      Document document = PageHelper.getDocument(
          url,
          configuration.getRequestParameters().getUserAgent(),
          configuration.getRequestParameters().getReferrer());

      contents.put("url", document.baseUri());
      contents.put("title", document.title());
      contents.put("content", PageHelper.getPageContent(document, pageTargetsParameters.getTags()));

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
}
