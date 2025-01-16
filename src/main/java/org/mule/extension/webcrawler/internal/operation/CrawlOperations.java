package org.mule.extension.webcrawler.internal.operation;

import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.CrawlConfiguration;
import org.mule.extension.webcrawler.internal.config.PageConfiguration;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetsParameters;
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
  @Alias("crawl-website")
  @DisplayName("[Crawl] Website")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/CrawlWebSite.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      crawlWebsite(
      @Config CrawlConfiguration configuration,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Download Location") @Placement(order = 9) @Example("/users/mulesoft/downloads") String downloadPath,
      @ParameterGroup(name = "Targets") CrawlerTargetsParameters targetsParameters,
      @DisplayName("Restrict Crawl under URL")@Placement(order = 2)@Example("False") boolean restrictToPath,
      @DisplayName("Dynamic Content Retrieval") @Placement(order = 3) @Example("False") boolean dynamicContent,
      @DisplayName("Maximum Depth") @Placement(order = 4) @Example("2") int maxDepth,
      @DisplayName("Delay (millisecs)") @Placement(order = 5) @Example("0") int delayMillis) {

    try {

      LOGGER.debug("Initialize crawler");

      Crawler crawler = Crawler.builder()
          .userAgent(configuration.getRequestParameters().getUserAgent())
          .referrer(configuration.getRequestParameters().getReferrer())
          .rootURL(url)
          .maxDepth(maxDepth)
          .restrictToPath(restrictToPath)
          .dynamicContent(dynamicContent)
          .delayMillis(delayMillis)
          .downloadPath(downloadPath)
          .contentTags(targetsParameters.getTags())
          .getMetaTags(targetsParameters.isGetMetaTags())
          .downloadImages(targetsParameters.isDownloadImages())
          .downloadDocuments(targetsParameters.isDownloadDocuments())
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
   * Retrieve internal links as a site map from the specified url and depth.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("crawl-links-as-sitemap")
  @DisplayName("[Crawl] Get links as sitemap")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/CrawlGetLinksAsSitemap.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
  getSiteMap(
      @Config CrawlConfiguration configuration,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Maximum Depth") @Placement(order = 2) @Example("2") int maxDepth,
      @DisplayName("Delay (millisecs)") @Placement(order = 3) @Example("0") int delayMillis) {

    try{

      LOGGER.info("Generate sitemap");

      Crawler crawler = Crawler.builder()
          .userAgent(configuration.getRequestParameters().getUserAgent())
          .referrer(configuration.getRequestParameters().getReferrer())
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
}
