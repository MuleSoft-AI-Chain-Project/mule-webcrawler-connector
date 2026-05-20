package org.mule.extension.webcrawler.internal.service.factory;

import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.connection.http.HttpConnection;
import org.mule.extension.webcrawler.internal.connection.webdriver.RemoteWebDriverConnection;
import org.mule.extension.webcrawler.internal.service.HttpPageFetchService;
import org.mule.extension.webcrawler.internal.service.PageFetchService;
import org.mule.extension.webcrawler.internal.service.RemoteWebDriverPageFetchService;

public final class PageFetchServiceFactory {

  private PageFetchServiceFactory() {}

  public static PageFetchService getService(WebCrawlerConnection connection) {
    if (connection instanceof RemoteWebDriverConnection) {
      return new RemoteWebDriverPageFetchService((RemoteWebDriverConnection) connection);
    }
    return new HttpPageFetchService((HttpConnection) connection);
  }
}
