package org.mule.extension.webcrawler.api.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchResponseAttributes extends ResponseAttributes {

  private final String query;

  public SearchResponseAttributes(HashMap<String, Object> requestAttributes) {

    super(requestAttributes);
    this.query = requestAttributes.containsKey("url") ? (String) requestAttributes.remove("url") : null;
  }

  public String getQuery() {
    return query;
  }
}
