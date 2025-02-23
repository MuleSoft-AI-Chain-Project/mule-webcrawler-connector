package org.mule.extension.webcrawler.internal.operation;

import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetContentParameters;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.extension.webcrawler.internal.pagination.CrawlerPagingProvider;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.api.streaming.CursorProvider;
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
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

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
  @OutputJsonType(schema = "api/metadata/CrawlWebSite.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      crawlWebsiteFullScan(
      @Config WebCrawlerConfiguration configuration,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Download location") @Placement(order = 2) @Example("/users/mulesoft/downloads") String downloadPath,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters,
      @ParameterGroup(name = "Target Content") CrawlerTargetContentParameters targetContentParameters) {

    try {

      LOGGER.debug("\n\n" + targetPagesParameters.toString() + "\n");
      LOGGER.debug("\n\n" + targetContentParameters.toString() + "\n");

      Crawler crawler = Crawler.builder()
          .userAgent(configuration.getRequestParameters().getUserAgent())
          .rootReferrer(configuration.getRequestParameters().getReferrer())
          .delayMillis(configuration.getCrawlerSettingsParameters().getDelayMillis())
          .dynamicContent(configuration.getCrawlerSettingsParameters().isDynamicContent())
          .rawHtml(configuration.getCrawlerSettingsParameters().isRawHtml())
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

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("crawl-website-streaming")
  @DisplayName("[Crawl] Website (Streaming)")
  @Throws(WebCrawlerErrorTypeProvider.class)

  public PagingProvider<WebCrawlerConnection, Result<CursorProvider, ResponseAttributes>>
  crawlWebsiteFullScan(
      @Config WebCrawlerConfiguration configuration,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters) {

    try {

      return new CrawlerPagingProvider();

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
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters) {

    try{

      LOGGER.info("Generate sitemap");

      Crawler crawler = Crawler.builder()
          .userAgent(configuration.getRequestParameters().getUserAgent())
          .rootReferrer(configuration.getRequestParameters().getReferrer())
          .delayMillis(configuration.getCrawlerSettingsParameters().getDelayMillis())
          .dynamicContent(configuration.getCrawlerSettingsParameters().isDynamicContent())
          .rootURL(url)
          .restrictToPath(targetPagesParameters.isRestrictToPath())
          .maxDepth(targetPagesParameters.getMaxDepth())
          .regexUrlsFilterLogic(targetPagesParameters.getRegexUrlsFilterLogic())
          .regexUrls(targetPagesParameters.getRegexUrls())
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
}
