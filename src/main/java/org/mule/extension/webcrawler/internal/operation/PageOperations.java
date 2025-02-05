package org.mule.extension.webcrawler.internal.operation;

import org.json.JSONArray;
import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
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
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
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
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      getMetaTags(
      @Config WebCrawlerConfiguration configuration,
      @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) {

    try {

      LOGGER.info("Get meta tags");

      Document document;

      if(!configuration.getCrawlerSettingsParameters().isDynamicContent()) {

        document = PageHelper.getDocument(
            url,
            configuration.getRequestParameters().getUserAgent(),
            configuration.getRequestParameters().getReferrer());
      } else {

        document = PageHelper.getDocumentDynamic(url);
      }

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
          @Config WebCrawlerConfiguration configuration,
          @DisplayName("Page or image URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @Alias("maxImageNumber") @DisplayName("Max number of images")
              @Summary("Maximum number of images to download. Default 0 means no limit.")
              @Placement(order = 2) @Expression(ExpressionSupport.SUPPORTED) @Example("10")
              @Optional int maxImageNumber,
          @DisplayName("Download location") @Placement(order = 3) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray imagesJSONArray = new JSONArray();

      try {
        // url provided is a website url, so download all images from this document
        Document document;

        if(!configuration.getCrawlerSettingsParameters().isDynamicContent()) {

          document = PageHelper.getDocument(
              url,
              configuration.getRequestParameters().getUserAgent(),
              configuration.getRequestParameters().getReferrer());
        } else {

          document = PageHelper.getDocumentDynamic(url);
        }

        imagesJSONArray = PageHelper.downloadWebsiteImages(document, downloadPath, maxImageNumber);

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
      @Config WebCrawlerConfiguration configuration,
      @DisplayName("Page or document URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @Alias("maxDocumentNumber") @DisplayName("Max number of documents")
          @Summary("Maximum number of documents to download. Default 0 means no limit.")
          @Placement(order = 2) @Expression(ExpressionSupport.SUPPORTED) @Example("10")
          @Optional int maxDocumentNumber,
      @DisplayName("Download Location") @Placement(order = 3) @Example("/users/mulesoft/downloads") String downloadPath) {

    try {

      JSONArray documentsJSONArray = new JSONArray();

      try {
        // url provided is a website url, so download all images from this document
        Document document;

        if(!configuration.getCrawlerSettingsParameters().isDynamicContent()) {

          document = PageHelper.getDocument(
              url,
              configuration.getRequestParameters().getUserAgent(),
              configuration.getRequestParameters().getReferrer());
        } else {

          document = PageHelper.getDocumentDynamic(url);
        }

        documentsJSONArray = PageHelper.downloadFiles(document, downloadPath, maxDocumentNumber);

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
          @Config WebCrawlerConfiguration configuration,
          @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @ParameterGroup(name="Target content") PageTargetContentParameters targetContentParameters) {

    try {

      LOGGER.info("Analyze page");

      Document document;

      if(!configuration.getCrawlerSettingsParameters().isDynamicContent()) {

        document = PageHelper.getDocument(
            url,
            configuration.getRequestParameters().getUserAgent(),
            configuration.getRequestParameters().getReferrer());
      } else {

        document = PageHelper.getDocumentDynamic(url);
      }

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(
              PageHelper.getPageInsights(document, targetContentParameters.getTags(), Constants.PageInsightType.ALL)
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
          @Config WebCrawlerConfiguration configuration,
          @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
          @ParameterGroup(name="Target content") PageTargetContentParameters targetContentParameters) {

    try {

      LOGGER.info("Get page content");

      Map<String, String> contents = new HashMap<String, String>();

      Document document;

      if(!configuration.getCrawlerSettingsParameters().isDynamicContent()) {

        document = PageHelper.getDocument(
            url,
            configuration.getRequestParameters().getUserAgent(),
            configuration.getRequestParameters().getReferrer());
      } else {

        document = PageHelper.getDocumentDynamic(url);
      }

      String content = PageHelper.getPageContent(document,
                                                 targetContentParameters.getTags(),
                                                 configuration.getCrawlerSettingsParameters().isRawHtml());

      contents.put("url", document.baseUri());
      contents.put("title", document.title());
      contents.put("content", content);

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
