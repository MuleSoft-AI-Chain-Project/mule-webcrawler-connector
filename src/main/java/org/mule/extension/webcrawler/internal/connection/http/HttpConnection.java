package org.mule.extension.webcrawler.internal.connection.http;

import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.http.api.client.HttpClient;

public class HttpConnection implements WebCrawlerConnection {

  private final HttpClient httpClient;
  private final String userAgent;
  private final String referrer;
  private final int timeout;

  public HttpConnection(HttpClient httpClient, int timeout, String userAgent, String referrer) {
    this.httpClient = httpClient;
    this.userAgent = userAgent;
    this.referrer = referrer;
    this.timeout = timeout;
  }

  public HttpClient getHttpClient() {
    return httpClient;
  }

  public int getTimeout() {
    return timeout;
  }

  @Override
  public String getUserAgent() {
    return userAgent;
  }

  @Override
  public String getReferrer() {
    return referrer;
  }

  public boolean validate() throws ConnectionException {
    return true;
  }
}
