package org.mule.extension.webcrawler.internal.connection;

import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.model.WebCrawlerResponse;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnection implements WebCrawlerConnection {

    private static Logger LOGGER = LoggerFactory.getLogger(HttpConnection.class);

    private HttpClient httpClient;
    private String userAgent;
    private String referrer;
    private int timeout;

    public HttpConnection(HttpClient httpClient, int timeout, String userAgent, String referrer) {

        this.httpClient = httpClient;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.timeout = timeout;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public boolean validate() throws ConnectionException {
        return true;
    }

    @Override
    public CompletableFuture<InputStream> getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {

        LOGGER.debug(String.format("Retrieving page source for url %s using http client (wait %s millisec)", url,
                                   pageLoadOptions.getWaitOnPageLoad()));
        if (pageLoadOptions.getWaitOnPageLoad() != null && pageLoadOptions.getWaitOnPageLoad().longValue() > 0L) {

            throw new RuntimeException("Wait duration is not supported for HttpConnection");
        }

        HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .method("GET")
                .uri(url);

        if (this.userAgent != null) {
            requestBuilder.addHeader("User-Agent", this.userAgent);
        }

        if (currentReferrer != null) {
            requestBuilder.addHeader("Referrer", currentReferrer);
        }

        HttpRequest request = requestBuilder.build();

        HttpRequestOptions options = HttpRequestOptions.builder()
                .responseTimeout(timeout != 0 ? timeout : 10000)
                .build();

        return httpClient.sendAsync(request, options)
                .thenApply(response -> {
                    if (response.getStatusCode() == 200) {
                        return response.getEntity().getContent();
                    } else {
                        throw new RuntimeException(String.format("%s: %s", response.getStatusCode(), response.getReasonPhrase()));
                    }
                })
                .exceptionally(e -> {
                    throw new RuntimeException(e);
                });
    }

    @Override
    public CompletableFuture<Integer> getUrlStatusCode(String url, String currentReferrer) {

        LOGGER.debug(String.format("Checking url status for %s using http client", url));

        HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .method("HEAD")
                .uri(url);

        if (this.userAgent != null) {
            requestBuilder.addHeader("User-Agent", this.userAgent);
        }

        if (currentReferrer != null) {
            requestBuilder.addHeader("Referrer", currentReferrer);
        }

        HttpRequest request = requestBuilder.build();

        HttpRequestOptions options = HttpRequestOptions.builder()
                .responseTimeout(timeout != 0 ? timeout : 10000)
                .build();

        return httpClient.sendAsync(request, options)
                .thenApply(response -> {
                    return response.getStatusCode();
                })
                .exceptionally(e -> {
                    throw new RuntimeException(e);
                });
    }

    /**
     * Sync GET that hands back status, content type, and body together. The single-threaded crawl-website-source worker calls
     * this once per URL — async wrap (used by the legacy {@link #getPageSource}) adds no real concurrency here, so it's collapsed
     * to a synchronous shape.
     *
     * <p>
     * Page-load wait params (wait-on-page-load, wait-for-xpath, shadow-DOM) are silently ignored on the HTTP path — they are
     * browser-only and the user has been told that via {@code @Summary("...not available for HTTP connection")}.
     * </p>
     */
    @Override
    public WebCrawlerResponse fetchPage(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {

        LOGGER.debug("HTTP fetch: {}", url);

        HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .method("GET")
                .uri(url);

        if (this.userAgent != null) {
            requestBuilder.addHeader("User-Agent", this.userAgent);
        }
        if (currentReferrer != null) {
            requestBuilder.addHeader("Referrer", currentReferrer);
        }

        HttpRequestOptions options = HttpRequestOptions.builder()
                .responseTimeout(timeout != 0 ? timeout : 10000)
                .build();

        HttpResponse response;
        try {
            response = httpClient.sendAsync(requestBuilder.build(), options).get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching " + url, ie);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            throw new RuntimeException("Failed to fetch " + url, cause);
        }

        int status = response.getStatusCode();
        String contentType = response.getHeaderValue("Content-Type");
        InputStream body = response.getEntity() != null ? response.getEntity().getContent() : null;
        return new WebCrawlerResponse(status, contentType, body);
    }
}
