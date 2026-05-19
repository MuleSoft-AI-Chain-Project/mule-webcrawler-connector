package org.mule.extension.webcrawler.internal.model;

import java.io.InputStream;

/**
 * Wire-level response wrapper produced by
 * {@link org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection#fetchPage}. Carries the minimum information the
 * {@code crawl-website-source} Source needs to filter, log, and emit a page event.
 *
 * <p>
 * Internal type — never exposed to users. The Source extracts what it needs and emits
 * {@link org.mule.extension.webcrawler.api.metadata.CrawledContentAttributes} as the user-visible projection.
 * </p>
 */
public final class WebCrawlerResponse {

    private final int statusCode;
    private final String contentType;
    private final InputStream body;

    public WebCrawlerResponse(int statusCode, String contentType, InputStream body) {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getBody() {
        return body;
    }
}
