package org.mule.extension.webcrawler.internal.connection;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

public class WebCrawlerConnectionProvider implements CachedConnectionProvider<WebCrawlerConnection> {

  @Override
  public WebCrawlerConnection connect() throws ConnectionException {
    return new WebCrawlerConnection();
  }

  @Override
  public void disconnect(WebCrawlerConnection webCrawlerConnection) {

  }

  @Override
  public ConnectionValidationResult validate(WebCrawlerConnection webCrawlerConnection) {
    return ConnectionValidationResult.success();
  }
}
