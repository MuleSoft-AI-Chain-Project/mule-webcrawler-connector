package org.mule.extension.webcrawler.internal.config;

import org.mule.extension.webcrawler.internal.connection.http.HttpConnectionProvider;
import org.mule.extension.webcrawler.internal.connection.webdriver.WebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerSettingsParameters;
import org.mule.extension.webcrawler.internal.operation.CrawlOperations;
import org.mule.extension.webcrawler.internal.operation.PageOperations;
import org.mule.extension.webcrawler.internal.operation.SearchOperations;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@org.mule.runtime.extension.api.annotation.Configuration(name = "config")
@ConnectionProviders({HttpConnectionProvider.class, WebDriverConnectionProvider.class})
@org.mule.runtime.extension.api.annotation.Operations({CrawlOperations.class, PageOperations.class, SearchOperations.class})
public class WebCrawlerConfiguration {

  @ParameterGroup(name= "Crawler Settings")
  private CrawlerSettingsParameters crawlerSettingsParameters;

  @Parameter
  @Alias("waitDuration")
  @DisplayName("Wait duration (millisecs)")
  @Summary("The time to wait on page load (not available for HTTP connection)")
  @Placement(order = 1, tab = "Wait on Page Load")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("1000")
  @Optional
  private Long waitDuration;

  @Parameter
  @Alias("waitUntilXPath")
  @DisplayName("Wait until XPath")
  @Summary("The XPath to wait for (not available for HTTP connection)")
  @Placement(order = 2, tab = "Wait on Page Load")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("//body")
  @Optional
  private String waitUntilXPath;

  public CrawlerSettingsParameters getCrawlerSettingsParameters() {
    return crawlerSettingsParameters;
  }

  public void setCrawlerSettingsParameters(
      CrawlerSettingsParameters crawlerSettingsParameters) {
    this.crawlerSettingsParameters = crawlerSettingsParameters;
  }

  public Long getWaitDuration() {
    return waitDuration;
  }

  public void setWaitDuration(Long waitDuration) {
    this.waitDuration = waitDuration;
  }

  public String getWaitUntilXPath() {
    return waitUntilXPath;
  }

  public void setWaitUntilXPath(String waitUntilXPath) {
    this.waitUntilXPath = waitUntilXPath;
  }
}
