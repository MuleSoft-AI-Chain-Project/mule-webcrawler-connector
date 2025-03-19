package org.mule.extension.webcrawler.internal.config;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class CrawlerOptions {

  @Parameter
  @Alias("delayMillis")
  @DisplayName("Delay between pages (millisecs)")
  @Summary("The delay introduced between each request.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("0")
  @Optional(defaultValue = "0")
  private int delayMillis;

  @Parameter
  @Alias("enforceRobotsTxt")
  @DisplayName("Enforce robots.txt")
  @Summary("If true, enforce checks against the robots.txt file.")
  @Placement(order = 2)
  @Optional(defaultValue = "false")
  private boolean enforceRobotsTxt;

  public int getDelayMillis() {
    return delayMillis;
  }

  public void setDelayMillis(int delayMillis) {
    this.delayMillis = delayMillis;
  }

  public boolean isEnforceRobotsTxt() { return enforceRobotsTxt; }

  public void setEnforceRobotsTxt(boolean enforceRobotsTxt) { this.enforceRobotsTxt = enforceRobotsTxt; }
}
