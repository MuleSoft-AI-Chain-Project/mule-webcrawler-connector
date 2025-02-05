package org.mule.extension.webcrawler.internal.helper.parameter;

import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class CrawlerSettingsParameters {

  @Parameter
  @Alias("delayMillis")
  @DisplayName("Delay (millisecs)")
  @Summary("The delay introduced between each request.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("0")
  @Optional(defaultValue = "0")
  private int delayMillis;

  @Parameter
  @Alias("dynamicContent")
  @DisplayName("Dynamic Content Retrieval")
  @Summary("If true use selenium driver to crawl dynamically generated content from the page.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean dynamicContent;

  @Parameter
  @Alias("rawHtml")
  @DisplayName("RAW Html")
  @Summary("If true extract raw html from pages and not parsed text content.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean rawHtml;

  public int getDelayMillis() {
    return delayMillis;
  }

  public void setDelayMillis(int delayMillis) {
    this.delayMillis = delayMillis;
  }

  public boolean isDynamicContent() { return dynamicContent; }

  public void setDynamicContent(boolean dynamicContent) { this.dynamicContent = dynamicContent; }

  public boolean isRawHtml() { return rawHtml;}

  public void setRawHtml(boolean rawHtml) { this.rawHtml = rawHtml; }
}
