package org.mule.extension.webcrawler.internal.pagination;

import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.List;
import java.util.Optional;

public class CrawlerPagingProvider implements PagingProvider<WebCrawlerConnection, Result<CursorProvider, ResponseAttributes>> {

  @Override
  public List<Result<CursorProvider, ResponseAttributes>> getPage(WebCrawlerConnection webCrawlerConnection) {
    return List.of();
  }

  @Override
  public Optional<Integer> getTotalResults(WebCrawlerConnection webCrawlerConnection) {
    return Optional.empty();
  }

  @Override
  public boolean useStickyConnections() {
    return true;
  }

  @Override
  public void close(WebCrawlerConnection webCrawlerConnection) throws MuleException {

  }
}
