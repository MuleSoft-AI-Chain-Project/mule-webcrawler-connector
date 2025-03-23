package org.mule.extension.webcrawler.api.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PageResponseAttributes extends ResponseAttributes {

  private final String url;
  private final String title;

  public PageResponseAttributes(HashMap<String, Object> requestAttributes) {

    super(requestAttributes);
    this.url = requestAttributes.containsKey("url") ? (String) requestAttributes.remove("url") : null;
    this.title = requestAttributes.containsKey("title") ? (String) requestAttributes.remove("title") : null;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }
}
