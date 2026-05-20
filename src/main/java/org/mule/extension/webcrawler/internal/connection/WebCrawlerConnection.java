package org.mule.extension.webcrawler.internal.connection;

import org.mule.extension.webcrawler.internal.config.PageLoadOptions;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface WebCrawlerConnection {

  String getUserAgent();
  String getReferrer();

  Integer getUrlStatusCode(String url, String currentReferrer) throws ExecutionException, InterruptedException;

  InputStream getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions) throws ExecutionException, InterruptedException;

}
