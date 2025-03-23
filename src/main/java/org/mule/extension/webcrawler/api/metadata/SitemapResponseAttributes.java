package org.mule.extension.webcrawler.api.metadata;

import java.util.HashMap;

public class SitemapResponseAttributes extends ResponseAttributes {

  private final String url;
  private final Integer depth;
  private final Integer count;

  public SitemapResponseAttributes(HashMap<String, Object> requestAttributes) {
    super(requestAttributes);
    this.url = requestAttributes.containsKey("url") ? (String) requestAttributes.remove("url") : null;
    this.depth = requestAttributes.containsKey("depth") ? (Integer) requestAttributes.remove("depth") : null;
    this.count = requestAttributes.containsKey("count") ? (Integer) requestAttributes.remove("count") : null;
  }

  public Integer getDepth() {
    return depth;
  }

  public Integer getCount() {
    return count;
  }

  public String getUrl() {
    return url;
  }
}
