package org.mule.extension.webcrawler.internal.helper.parameter;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.List;

public class PageTargetsParameters {

  @Parameter
  @DisplayName("Tag List")
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
