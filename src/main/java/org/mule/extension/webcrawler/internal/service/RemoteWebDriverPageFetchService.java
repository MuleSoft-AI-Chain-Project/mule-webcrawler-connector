package org.mule.extension.webcrawler.internal.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.webdriver.RemoteWebDriverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RemoteWebDriverPageFetchService implements PageFetchService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverPageFetchService.class);

  private static final long DEFAULT_WAIT_ON_PAGE_LOAD_MS = 30000L;

  private final RemoteWebDriverConnection connection;

  public RemoteWebDriverPageFetchService(RemoteWebDriverConnection connection) {
    this.connection = connection;
  }

  @Override
  public Document getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions)
      throws IOException, ExecutionException, InterruptedException {

    LOGGER.debug("RemoteWebDriver fetch: {}", url);
    try {
      return connection.withDriver(driver -> {
        driver.get(url);

        long effectiveTimeout = Optional.ofNullable(pageLoadOptions != null ? pageLoadOptions.getWaitOnPageLoad() : null)
            .filter(t -> t > 0)
            .orElse(DEFAULT_WAIT_ON_PAGE_LOAD_MS);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        new FluentWait<>(driver)
            .withTimeout(Duration.ofMillis(effectiveTimeout))
            .pollingEvery(Duration.ofMillis(500))
            .until(d -> "complete".equals(js.executeScript("return document.readyState")));

        if (pageLoadOptions != null
            && pageLoadOptions.getWaitForXPath() != null
            && !pageLoadOptions.getWaitForXPath().isEmpty()) {
          waitForXPathLoad(driver, effectiveTimeout, pageLoadOptions.getWaitForXPath());
        }

        if (pageLoadOptions != null
            && pageLoadOptions.getJavascript() != null
            && !pageLoadOptions.getJavascript().isEmpty()) {
          js.executeScript(pageLoadOptions.getJavascript());
        }

        String pageSource = driver.getPageSource();
        Document document = Jsoup.parse(pageSource != null ? pageSource : "", url);

        if (pageLoadOptions != null && pageLoadOptions.isExtractShadowDom()) {
          injectAllShadowDOMs(driver, document, pageLoadOptions.getShadowHostXPath());
        }
        return document;
      });
    } catch (ConnectionException e) {
      throw new IOException("Remote WebDriver fetch failed for URL: " + url, e);
    } catch (RuntimeException e) {
      throw new IOException("Remote WebDriver fetch failed for URL: " + url, e);
    }
  }

  @Override
  public Integer getUrlStatusCode(String url, String currentReferrer)
      throws ExecutionException, InterruptedException {

    // The remote browser doesn't expose HTTP status to the W3C protocol on plain GETs. Dispatch an in-page
    // fetch() to harvest the status — same approach the embedded driver used.
    try {
      return connection.withDriver(driver -> {
        driver.get(url);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object status = js.executeScript(
            "return fetch(arguments[0], { method: 'HEAD' })"
                + ".then(response => response.status)"
                + ".catch(() => 0);",
            url);
        return status instanceof Long ? ((Long) status).intValue() : 500;
      });
    } catch (Exception e) {
      LOGGER.debug("Status check failed for {}: {}", url, e.getMessage());
      return 500;
    }
  }

  private static void waitForXPathLoad(WebDriver driver, long waitOnPageLoad, String waitForXPath) {
    LOGGER.debug("Wait until {} for {} milliseconds", waitForXPath, waitOnPageLoad);
    try {
      new FluentWait<>(driver)
          .withTimeout(Duration.ofMillis(waitOnPageLoad))
          .pollingEvery(Duration.ofMillis(500))
          .until(d -> {
            try {
              driver.findElement(By.xpath(waitForXPath));
              return true;
            } catch (NoSuchElementException e) {
              return false;
            }
          });
    } catch (TimeoutException e) {
      LOGGER.warn("Element {} not found within the timeout period {}", waitForXPath, waitOnPageLoad);
    }
  }

  /**
   * Recursively injects all shadow DOMs inside a specified XPath into the jsoup document. Per-tag occurrence counts
   * keep multiple instances of the same shadow-host tag (e.g. several {@code <my-card>} components) appended to their
   * own jsoup element rather than collapsing onto the first one.
   */
  private static void injectAllShadowDOMs(WebDriver driver, Document document, String shadowHostXPath) {
    JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
    String hostXPath = shadowHostXPath == null ? "//*" : shadowHostXPath;

    List<WebElement> shadowHosts = driver.findElements(By.xpath(hostXPath));
    Map<String, Integer> tagCursor = new HashMap<>();

    for (WebElement shadowHost : shadowHosts) {
      Boolean hasShadowRoot = (Boolean) jsExecutor.executeScript(
          "return arguments[0].shadowRoot !== null", shadowHost);
      if (Boolean.TRUE.equals(hasShadowRoot)) {
        String shadowContent = (String) jsExecutor.executeScript(
            "return arguments[0].shadowRoot.innerHTML;", shadowHost);

        String tagName = shadowHost.getTagName();
        appendToNthJsoupElement(document, tagName, tagCursor, shadowContent);

        injectNestedShadowDOMs(jsExecutor, document, shadowHost, tagCursor);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void injectNestedShadowDOMs(JavascriptExecutor jsExecutor, Document document, WebElement shadowHost,
                                             Map<String, Integer> tagCursor) {
    String shadowRootContent = (String) jsExecutor.executeScript(
        "return arguments[0].shadowRoot ? arguments[0].shadowRoot.innerHTML : null;",
        shadowHost);

    if (shadowRootContent != null) {
      String tagName = shadowHost.getTagName();
      appendToNthJsoupElement(document, tagName, tagCursor, shadowRootContent);

      List<WebElement> nestedElements = (List<WebElement>) jsExecutor.executeScript(
          "return Array.from(arguments[0].shadowRoot.querySelectorAll('*'))",
          shadowHost);

      for (WebElement nestedElement : nestedElements) {
        injectNestedShadowDOMs(jsExecutor, document, nestedElement, tagCursor);
      }
    }
  }

  private static void appendToNthJsoupElement(Document document, String tagName, Map<String, Integer> tagCursor,
                                              String content) {
    int idx = tagCursor.getOrDefault(tagName, 0);
    Elements matches = document.select(tagName);
    Element target = idx < matches.size() ? matches.get(idx) : document.selectFirst(tagName);
    if (target != null) {
      target.append(content);
    }
    tagCursor.put(tagName, idx + 1);
  }
}
