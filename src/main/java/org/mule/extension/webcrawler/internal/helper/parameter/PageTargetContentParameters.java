package org.mule.extension.webcrawler.internal.helper.parameter;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;

public class PageTargetContentParameters {


  @Parameter
  @Alias("tags")
  @DisplayName("Tag list")
  @Summary("List of html tags for which content must be retrieved.")
  @Placement(order = 1)
  @Optional
  private List<String> tags;

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}
