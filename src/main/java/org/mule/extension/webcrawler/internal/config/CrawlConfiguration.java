package org.mule.extension.webcrawler.internal.config;

import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerSettingsParameters;
import org.mule.extension.webcrawler.internal.helper.parameter.RequestParameters;
import org.mule.extension.webcrawler.internal.operation.CrawlOperations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.List;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@org.mule.runtime.extension.api.annotation.Configuration(name = "crawlConfig")
@org.mule.runtime.extension.api.annotation.Operations(CrawlOperations.class)
public class CrawlConfiguration {

  @ParameterGroup(name= "Request Parameters")
  private RequestParameters requestParameters;

  @ParameterGroup(name= "Crawler Settings")
  private CrawlerSettingsParameters crawlerSettingsParameters;

  public RequestParameters getRequestParameters() {
    return requestParameters;
  }

  public void setRequestParameters(RequestParameters requestParameters) {
    this.requestParameters = requestParameters;
  }

  public CrawlerSettingsParameters getCrawlerSettingsParameters() {
    return crawlerSettingsParameters;
  }

  public void setCrawlerSettingsParameters(
      CrawlerSettingsParameters crawlerSettingsParameters) {
    this.crawlerSettingsParameters = crawlerSettingsParameters;
  }
}
