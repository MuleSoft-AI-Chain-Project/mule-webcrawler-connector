package org.mule.extension.webcrawler.internal.helper.parameter;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class CrawlerTargetPagesParameters {

  @Parameter
  @Alias("restrictToPath")
  @DisplayName("Restrict Crawl under URL")
  @Summary("If true only internal pages are crawled.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("True")
  private boolean restrictToPath;

  @Parameter
  @Alias("maxDepth")
  @DisplayName("Maximum Depth")
  @Summary("The maximum level of depth that can be reached while crawling a website.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("2")
  private int maxDepth;

  public boolean isRestrictToPath() {
    return restrictToPath;
  }

  public void setRestrictToPath(boolean restrictToPath) {
    this.restrictToPath = restrictToPath;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  @Override
  public String toString() {
    return "CrawlerTargetPagesParameters{" +
        "restrictToPath=" + restrictToPath +
        ", maxDepth=" + maxDepth +
        '}';
  }
}
