package org.mule.extension.webcrawler.internal.connection;

import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.model.WebCrawlerResponse;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public interface WebCrawlerConnection {

    String getUserAgent();

    String getReferrer();

    CompletableFuture<Integer> getUrlStatusCode(String url, String currentReferrer);

    CompletableFuture<InputStream> getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions);

    /**
     * Fetch a page and return the response synchronously, including status code, content type, and body. Used by
     * {@code crawl-website-source} which needs the metadata to filter, log, and route per-page events. The legacy
     * {@link #getPageSource} / {@link #getUrlStatusCode} methods stay in place for the existing crawler tree (used by
     * {@code get-sitemap} / {@code page-*} operations).
     */
    WebCrawlerResponse fetchPage(String url, String currentReferrer, PageLoadOptions pageLoadOptions);

}
