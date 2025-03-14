package org.mule.extension.webcrawler.internal.pagination;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.mule.extension.webcrawler.internal.helper.ResponseHelper.createPageResponse;
import static org.mule.extension.webcrawler.internal.helper.page.PageHelper.getPageContent;

public class CrawlerPagingProvider implements PagingProvider<WebCrawlerConnection, Result<CursorProvider, ResponseAttributes>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerPagingProvider.class);

  private Crawler crawler;
  private Iterator<Document> documentIterator;
  private WebCrawlerConfiguration configuration;
  private String url;
  private Constants.OutputFormat outputFormat;
  private CrawlerTargetPagesParameters targetPagesParameters;
  private StreamingHelper streamingHelper;

  public CrawlerPagingProvider(WebCrawlerConfiguration configuration,
                               String url,
                               Constants.OutputFormat outputFormat,
                               CrawlerTargetPagesParameters targetPagesParameters,
                               StreamingHelper streamingHelper) {

    this.configuration = configuration;
    this.url = url;
    this.outputFormat = outputFormat;
    this.targetPagesParameters = targetPagesParameters;
    this.streamingHelper = streamingHelper;
  }

  @Override
  public List<Result<CursorProvider, ResponseAttributes>> getPage(WebCrawlerConnection connection) {

    try {

      if(crawler == null) {

        crawler = Crawler.builder()
            .configuration(configuration)
            .connection(connection)
            .outputFormat(outputFormat)
            .rootURL(url)
            .maxDepth(targetPagesParameters.getMaxDepth())
            .restrictToPath(targetPagesParameters.isRestrictToPath())
            .regexUrlsFilterLogic(targetPagesParameters.getRegexUrlsFilterLogic())
            .regexUrls(targetPagesParameters.getRegexUrls())
            .build();

        documentIterator = crawler.documentIterator();
      }

      while(documentIterator.hasNext()) {

        try {

          Document document = documentIterator.next();
          if(document == null) {
            continue;
          }

          String pageContent = getPageContent(document, null, outputFormat);

          Map<String, String> pageMap = new HashMap<String, String>();
          pageMap.put("url", document.baseUri());
          pageMap.put("title", document.title());
          pageMap.put("content", pageContent);

          return createPageResponse(
              JSONUtils.convertToJSON(pageMap, true),
              new HashMap<String, Object>() {{
                put("url", url);
              }},
              streamingHelper
          );

        } catch (Exception e) {

          //Look for next page if any on error
          LOGGER.error(String.format("Error while getting page content. %s", e.getMessage()));
        }
      }

      return Collections.emptyList();

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while getting page from %s.", url),
          WebCrawlerErrorType.CRAWL_OPERATIONS_FAILURE,
          e);
    }

  }

  @Override
  public Optional<Integer> getTotalResults(WebCrawlerConnection connection) {
    return Optional.empty();
  }

  @Override
  public boolean useStickyConnections() {
    return true;
  }

  @Override
  public void close(WebCrawlerConnection connection) throws MuleException {

  }
}
