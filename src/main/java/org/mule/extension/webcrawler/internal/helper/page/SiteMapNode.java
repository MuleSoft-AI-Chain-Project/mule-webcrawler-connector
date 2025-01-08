package org.mule.extension.webcrawler.internal.helper.page;

import java.util.ArrayList;
import java.util.List;

public class SiteMapNode {

    private String url;
    private List<SiteMapNode> children;

    public SiteMapNode(String url) {
        this.url = url;
        this.children = new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    public List<SiteMapNode> getChildren() {
        return children;
    }

    public void addChild(SiteMapNode child) {
        this.children.add(child);
    }
}

