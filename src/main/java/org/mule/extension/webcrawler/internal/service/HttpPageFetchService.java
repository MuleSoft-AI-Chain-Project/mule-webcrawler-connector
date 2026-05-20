package org.mule.extension.webcrawler.internal.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.http.HttpConnection;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class HttpPageFetchService implements PageFetchService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpPageFetchService.class);

  private final HttpConnection connection;

  public HttpPageFetchService(HttpConnection connection) {
    this.connection = connection;
  }

  @Override
  public Document getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions)
      throws IOException, ExecutionException, InterruptedException {

    LOGGER.debug("Retrieving page source for url {} using http client (wait {} ms)",
        url, pageLoadOptions != null ? pageLoadOptions.getWaitOnPageLoad() : null);

    if (pageLoadOptions != null && pageLoadOptions.getWaitOnPageLoad() != null
        && pageLoadOptions.getWaitOnPageLoad() > 0L) {
      throw new IOException("Wait duration is not supported for HttpConnection");
    }

    HttpRequest request = buildRequest("GET", url, currentReferrer);
    HttpRequestOptions options = buildRequestOptions();
    HttpClient httpClient = connection.getHttpClient();

    try (InputStream body = httpClient.sendAsync(request, options)
        .thenApply(response -> {
          if (response.getStatusCode() == 200) {
            return response.getEntity().getContent();
          }
          throw new RuntimeException(response.getStatusCode() + ": " + response.getReasonPhrase());
        }).get()) {
      String html = new String(body.readAllBytes(), StandardCharsets.UTF_8);
      return Jsoup.parse(html, url);
    }
  }

  @Override
  public Integer getUrlStatusCode(String url, String currentReferrer)
      throws ExecutionException, InterruptedException {

    LOGGER.debug("Checking url status for {} using http client", url);

    HttpRequest request = buildRequest("HEAD", url, currentReferrer);
    HttpRequestOptions options = buildRequestOptions();

    return connection.getHttpClient().sendAsync(request, options)
        .thenApply(response -> response.getStatusCode())
        .exceptionally(e -> {
          throw new RuntimeException(e);
        }).get();
  }

  private HttpRequest buildRequest(String method, String url, String currentReferrer) {
    HttpRequestBuilder builder = HttpRequest.builder().method(method).uri(url);
    if (connection.getUserAgent() != null) {
      builder.addHeader("User-Agent", connection.getUserAgent());
    }
    if (currentReferrer != null) {
      builder.addHeader("Referrer", currentReferrer);
    }
    return builder.build();
  }

  private HttpRequestOptions buildRequestOptions() {
    int timeout = connection.getTimeout();
    return HttpRequestOptions.builder()
        .responseTimeout(timeout != 0 ? timeout : 10000)
        .build();
  }
}
