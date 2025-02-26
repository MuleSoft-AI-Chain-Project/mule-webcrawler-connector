package org.mule.extension.webcrawler.internal.connection;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public interface WebCrawlerConnection {

  String getUserAgent();
  String getReferrer();

  CompletableFuture<InputStream> getPageSource(String url, String currentReferrer);
  CompletableFuture<InputStream> getPageSource(String url);
}
