package org.mule.extension.webcrawler.api.metadata;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Attributes attached to each event emitted by the {@code crawl-website-source} Source. One event per fetched page; the event
 * payload carries the page body, these attributes carry the page metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CrawledContentAttributes implements Serializable {

    private static final long serialVersionUID = 2L;

    private final String url;
    private final int statusCode;
    private final String contentType;
    private final int depth;
    /**
     * Meta tags scraped from the page, set when the Source's {@code getMetaTags=true} flag is on. JSON-array string when present,
     * {@code null} otherwise. Kept here (vs. the event payload) so the payload remains the page body.
     *
     * <p>
     * <b>Shape:</b> the value is a string, not a structured Java object — the connector keeps the boundary string-typed so the
     * meta-tag schema can evolve without recompiling consumers. To navigate it in DataWeave, parse it back into JSON, e.g.
     * {@code attributes.metaTags as Object {class: "json"}} (or use {@code read(attributes.metaTags, "application/json")} for a
     * non-coercive read).
     * </p>
     */
    private final String metaTags;

    public CrawledContentAttributes(String url, int statusCode, String contentType, int depth) {
        this(url, statusCode, contentType, depth, null);
    }

    public CrawledContentAttributes(String url, int statusCode, String contentType, int depth, String metaTags) {
        this.url = url;
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.depth = depth;
        this.metaTags = metaTags;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public int getDepth() {
        return depth;
    }

    public String getMetaTags() {
        return metaTags;
    }
}
