package org.mule.extension.webcrawler.internal.config;

import org.mule.extension.webcrawler.internal.operation.CrawlOperations;
import org.mule.extension.webcrawler.internal.operation.PageOperations;
import org.mule.extension.webcrawler.internal.operation.SearchOperations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.List;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@org.mule.runtime.extension.api.annotation.Operations({CrawlOperations.class, PageOperations.class, SearchOperations.class})
public class Configuration {

  @Parameter
  @Optional
  @DisplayName("Tag List")
  private List<String> tags;

  // Getters and Setters
  public List<String> getTags() {
    return this.tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}
