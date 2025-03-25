package org.mule.extension.webcrawler.internal.connection;

import org.mule.extension.webcrawler.internal.config.PageLoadOptions;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public interface WebCrawlerConnection {

  String getUserAgent();
  String getReferrer();

  CompletableFuture<Integer> getUrlStatusCode(String url, String currentReferrer);
  CompletableFuture<Integer> getUrlStatusCode(String url);

  CompletableFuture<InputStream> getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions);
  CompletableFuture<InputStream> getPageSource(String url, PageLoadOptions pageLoadOptions);
}
