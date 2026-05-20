package org.mule.extension.webcrawler.internal.operation;

import org.mule.extension.webcrawler.api.metadata.SitemapResponseAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputXmlType;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;

import static org.mule.runtime.extension.api.annotation.param.MediaType.*;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class CrawlOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrawlOperations.class);

  /**
   * Retrieve internal links as a site map from the specified url and depth.
   */
  @MediaType(value = APPLICATION_XML, strict = false)
  @Alias("get-sitemap")
  @DisplayName("[Crawl] Get sitemap")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputXmlType(schema = "api/metadata/sitemap.xsd", qname = "{http://www.sitemaps.org/schemas/sitemap/0.9}urlset")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, SitemapResponseAttributes>
  getSiteMap(
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
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @ParameterGroup(name = "Target Pages") CrawlerTargetPagesParameters targetPagesParameters) {

    try{

      LOGGER.info("Generate sitemap");

      Crawler crawler = Crawler.builder()
          .configuration(configuration)
          .waitOnPageLoad(waitOnPageLoad)
          .waitForXPath(waitForXPath)
          .extractShadowDom(extractShadowDom)
          .shadowHostXPath(shadowHostXPath)
          .connection(connection)
          .rootURL(url)
          .restrictToPath(targetPagesParameters.isRestrictToPath())
          .maxDepth(targetPagesParameters.getMaxDepth())
          .regexUrlsFilterLogic(targetPagesParameters.getRegexUrlsFilterLogic())
          .regexUrls(targetPagesParameters.getRegexUrls())
          .build();

      Crawler.SiteNode root = crawler.map();
      String sitemapXmlString = root != null ? Crawler.SitemapGenerator.generateSitemapXml(root) : "";
      int count = sitemapXmlString.split("<url>", -1).length - 1;

      return ResponseHelper.createSitemapResponse(
          sitemapXmlString,
          new HashMap<String, Object>() {{
            put("url", url);
            put("count", count);
            put("depth", targetPagesParameters.getMaxDepth());
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
