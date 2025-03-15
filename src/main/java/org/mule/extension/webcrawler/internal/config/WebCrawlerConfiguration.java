package org.mule.extension.webcrawler.internal.config;

import org.mule.extension.webcrawler.internal.connection.http.HttpConnectionProvider;
import org.mule.extension.webcrawler.internal.connection.webdriver.WebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.operation.CrawlOperations;
import org.mule.extension.webcrawler.internal.operation.PageOperations;
import org.mule.extension.webcrawler.internal.operation.SearchOperations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@org.mule.runtime.extension.api.annotation.Configuration(name = "config")
@ConnectionProviders({HttpConnectionProvider.class, WebDriverConnectionProvider.class})
@org.mule.runtime.extension.api.annotation.Operations({CrawlOperations.class, PageOperations.class, SearchOperations.class})
public class WebCrawlerConfiguration {

  @ParameterGroup(name= "Crawler Options")
  private CrawlerOptions crawlerOptions;

  @ParameterGroup(name= "Page Load Options (WebDriver)")
  private PageLoadOptions pageLoadOptions;

  public CrawlerOptions getCrawlerOptions() {
    return crawlerOptions;
  }

  public void setCrawlerOptions(CrawlerOptions crawlerOptions) {
    this.crawlerOptions = crawlerOptions;
  }

  public PageLoadOptions getPageLoadOptions() {
    return pageLoadOptions;
  }

  public void setPageLoadOptions(PageLoadOptions pageLoadOptions) {
    this.pageLoadOptions = pageLoadOptions;
  }
}
