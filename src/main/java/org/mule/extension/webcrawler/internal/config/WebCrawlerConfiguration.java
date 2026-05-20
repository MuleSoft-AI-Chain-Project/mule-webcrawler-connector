package org.mule.extension.webcrawler.internal.config;

import org.mule.extension.webcrawler.internal.connection.http.HttpConnectionProvider;
import org.mule.extension.webcrawler.internal.connection.webdriver.RemoteWebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.operation.CrawlOperations;
import org.mule.extension.webcrawler.internal.operation.PageOperations;
import org.mule.extension.webcrawler.internal.operation.SearchOperations;
import org.mule.extension.webcrawler.internal.source.CrawlerSource;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.Sources;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name = "config")
@ConnectionProviders({HttpConnectionProvider.class, RemoteWebDriverConnectionProvider.class})
@Operations({CrawlOperations.class, PageOperations.class, SearchOperations.class})
@Sources(CrawlerSource.class)
public class WebCrawlerConfiguration {

  @ParameterGroup(name = "Crawler Options")
  private CrawlerOptions crawlerOptions;

  @ParameterGroup(name = "Page Load Options (WebDriver)")
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
