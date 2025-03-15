package org.mule.extension.webcrawler.internal.operation;

import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetContentParameters;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.extension.webcrawler.internal.metadata.CrawlWebSiteStreamingOutputTypeMetadataResolver;
import org.mule.extension.webcrawler.internal.pagination.CrawlerPagingProvider;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class CrawlOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrawlOperations.class);

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
  @Alias("crawl-website-full-scan")
  @DisplayName("[Crawl] Website (Full Scan)")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/CrawlWebSiteFullScan.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      crawlWebsiteFullScan(
      @Config WebCrawlerConfiguration configuration,
      @ConfigOverride
          @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
          @Placement(order = 1, tab = "Page Load Options") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
      @ConfigOverride
          @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
      @Connection WebCrawlerConnection connection,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Output format") @Placement(order = 2) Constants.OutputFormat outputFormat,
      @DisplayName("Download location") @Placement(order = 3) @Example("/users/mulesoft/downloads") String downloadPath,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters,
      @ParameterGroup(name = "Target Content") CrawlerTargetContentParameters targetContentParameters) {

    try {

      LOGGER.debug("\n\n" + targetPagesParameters.toString() + "\n");
      LOGGER.debug("\n\n" + targetContentParameters.toString() + "\n");

      Crawler crawler = Crawler.builder()
          .configuration(configuration)
          .connection(connection)
          .outputFormat(outputFormat)
          .rootURL(url)
          .downloadPath(downloadPath)
          .maxDepth(targetPagesParameters.getMaxDepth())
          .restrictToPath(targetPagesParameters.isRestrictToPath())
          .contentTags(targetContentParameters.getTags())
          .getMetaTags(targetContentParameters.isGetMetaTags())
          .downloadImages(targetContentParameters.isDownloadImages())
          .maxImageNumber(targetContentParameters.getMaxImageNumber())
          .downloadDocuments(targetContentParameters.isDownloadDocuments())
          .maxDocumentNumber(targetContentParameters.getMaxDocumentNumber())
          .regexUrlsFilterLogic(targetPagesParameters.getRegexUrlsFilterLogic())
          .regexUrls(targetPagesParameters.getRegexUrls())
          .build();

      LOGGER.debug("Start website crawling");

      Crawler.SiteNode rootNode = crawler.crawl();

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(rootNode, true),
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

  @MediaType(value = ANY, strict = false)
  @Alias("crawl-website-streaming")
  @DisplayName("[Crawl] Website (Streaming)")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputResolver(output = CrawlWebSiteStreamingOutputTypeMetadataResolver.class)
  public PagingProvider<WebCrawlerConnection, Result<CursorProvider, ResponseAttributes>>
  crawlWebsiteStreaming(
      @Config WebCrawlerConfiguration configuration,
      @ConfigOverride
          @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
          @Placement(order = 1, tab = "Page Load Options") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
      @ConfigOverride
          @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Output format") @Placement(order = 2) Constants.OutputFormat outputFormat,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters,
      StreamingHelper streamingHelper) {

    try {

      return new CrawlerPagingProvider(configuration, url, outputFormat, targetPagesParameters, streamingHelper);

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
   * Retrieve internal links as a site map from the specified url and depth.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("crawl-links-as-sitemap")
  @DisplayName("[Crawl] Get links as sitemap")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/CrawlGetLinksAsSitemap.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
  getSiteMap(
      @Config WebCrawlerConfiguration configuration,
      @ConfigOverride
          @Alias("waitOnPageLoad") @DisplayName("Wait on page load (millisecs)") @Summary("The time to wait on page load (not available for HTTP connection)")
          @Placement(order = 1, tab = "Page Load Options") @Expression(ExpressionSupport.SUPPORTED) @Example("1000") @Optional Long waitOnPageLoad,
      @ConfigOverride
          @Alias("waitForXPath") @DisplayName("Wait for XPath") @Summary("The XPath to wait for (not available for HTTP connection)")
          @Placement(order = 2, tab = "Page Load Options") @Expression(ExpressionSupport.SUPPORTED) @Example("//body") @Optional String waitForXPath,
      @Connection WebCrawlerConnection connection,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters) {

    try{

      LOGGER.info("Generate sitemap");

      Crawler crawler = Crawler.builder()
          .configuration(configuration)
          .connection(connection)
          .rootURL(url)
          .restrictToPath(targetPagesParameters.isRestrictToPath())
          .maxDepth(targetPagesParameters.getMaxDepth())
          .regexUrlsFilterLogic(targetPagesParameters.getRegexUrlsFilterLogic())
          .regexUrls(targetPagesParameters.getRegexUrls())
          .build();

      Crawler.SiteNode root = crawler.map();

      return ResponseHelper.createResponse(
          JSONUtils.convertToJSON(root, true),
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
}
