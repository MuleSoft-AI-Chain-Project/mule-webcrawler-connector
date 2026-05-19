package org.mule.extension.webcrawler.internal.crawler;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * One node in a sitemap tree — a single page with its depth, optional referrer/parent linkage, and any discovered child pages.
 *
 * <p>
 * Plain data class lifted out of the deleted abstract {@code Crawler} base. Used by
 * {@link org.mule.extension.webcrawler.internal.operation.CrawlOperations#getSiteMap getSiteMap} to build the in-memory tree, and
 * by {@link SitemapGenerator} to format it as XML.
 * </p>
 */
public class SiteNode {

    private String url;
    @JsonIgnore
    private int currentDepth;
    @JsonIgnore
    private String referrer;
    private String filename;
    private List<SiteNode> children;
    @JsonIgnore
    private SiteNode parent;

    public SiteNode(String url, int currentDepth, String referrer) {
        this.url = url;
        this.currentDepth = currentDepth;
        this.referrer = referrer;
        this.children = new ArrayList<>();
    }

    public SiteNode(String url, int currentDepth, String referrer, SiteNode parent) {
        this.url = url;
        this.currentDepth = currentDepth;
        this.referrer = referrer;
        this.children = new ArrayList<>();
        this.parent = parent;
    }

    public SiteNode(String url, int currentDepth, String referrer, String filename) {
        this.url = url;
        this.currentDepth = currentDepth;
        this.filename = filename;
        this.referrer = referrer;
        this.children = new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<SiteNode> getChildren() {
        return children;
    }

    public void addChild(SiteNode child) {
        this.children.add(child);
    }

    public SiteNode getParent() {
        return parent;
    }
}
