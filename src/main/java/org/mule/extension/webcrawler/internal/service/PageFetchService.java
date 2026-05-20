package org.mule.extension.webcrawler.internal.service;

import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Fetches a page (and its status) from the configured connection. Implementations parse the response, applying any
 * connection-specific post-processing such as shadow-DOM injection for the WebDriver path.
 */
public interface PageFetchService {

  Document getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions)
      throws IOException, ExecutionException, InterruptedException;

  Integer getUrlStatusCode(String url, String currentReferrer)
      throws ExecutionException, InterruptedException;

  default boolean isURLValid(String url, String currentReferrer) {
    try {
      return getUrlStatusCode(url, currentReferrer) == 200;
    } catch (ExecutionException | InterruptedException e) {
      return false;
    }
  }
}
