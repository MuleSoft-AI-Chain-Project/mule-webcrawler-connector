package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v132.network.Network;
import org.openqa.selenium.devtools.v132.network.model.Headers;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WebDriverConnection implements WebCrawlerConnection {

    private static Logger LOGGER = LoggerFactory.getLogger(WebDriverConnection.class);

    private WebDriver driver;
    private String userAgent;
    private String referrer;

    public WebDriverConnection(WebDriver driver, String userAgent, String referrer) {
        this.driver = driver;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    @Override
    public CompletableFuture<InputStream> getPageSource(String url, PageLoadOptions pageLoadOptions) {

        return getPageSource(url, this.referrer, pageLoadOptions);
    }

    @Override
    public CompletableFuture<InputStream> getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {

        LOGGER.debug(String.format("Retrieving page source for url %s using webdrive (wait %s millisec)", url, pageLoadOptions.getWaitOnPageLoad()));
        return CompletableFuture.supplyAsync(() -> {
            // Set the referrer header
            if (currentReferrer != null && !currentReferrer.isEmpty() && !currentReferrer.equalsIgnoreCase(referrer)) {

                try{
                    // Use Chrome DevTools Protocol (CDP) to set the referrer dynamically
                    DevTools devTools = ((ChromeDriver) driver).getDevTools();
                    devTools.createSession();
                    devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                    devTools.send(Network.setExtraHTTPHeaders(new Headers(Map.of("Referer", currentReferrer))));
                } catch (Exception e) {

                    LOGGER.debug("Error while trying to set referer for web driver");
                }
            }
            // Load the dynamic page
            driver.get(url);

            // Wait for the page to load
            waitOnPageLoad(pageLoadOptions.getWaitOnPageLoad(), pageLoadOptions.getWaitForXPath());

            // Retrieve the page source
            String pageSource = driver.getPageSource();
            // Convert the page source to InputStream
            return new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8));
        });
    }

    /**
     * Waits for the page to load by either waiting for a specific element to appear or sleeping for a specified duration.
     *
     * @param waitOnPageLoad The time in milliseconds to wait for the page to load.
     * @param waitForXPath The XPath expression to wait for before proceeding.
     */
    private void waitOnPageLoad(Long waitOnPageLoad, String waitForXPath) {

        if(waitOnPageLoad != null && waitOnPageLoad.longValue() > 0L) {

            if(waitForXPath != null && waitForXPath.compareTo("") != 0) {

                LOGGER.debug(String.format("Wait until %s for %s milliseconds", waitForXPath, waitOnPageLoad));

                try {

                    new FluentWait<>(driver)
                        .withTimeout(Duration.ofMillis(waitOnPageLoad))  // Waits up to 5 seconds
                        .pollingEvery(Duration.ofMillis(500))  // Checks every 500ms
                        .ignoring(NoSuchElementException.class)
                        .until(new Function<WebDriver, Boolean>() {

                            public Boolean apply(WebDriver webDriver) {
                                try {
                                    // Attempt to find the element by XPath
                                    WebElement element = webDriver.findElement(By.xpath(waitForXPath));
                                    return element != null;  // Return true when the element is found in the DOM
                                } catch (NoSuchElementException e) {
                                    // Return false if the element is not found
                                    return false;
                                }
                            }
                        });
                } catch (TimeoutException e) {

                    LOGGER.warn(String.format("Element %s not found within the timeout period %s", waitForXPath, waitOnPageLoad));
                }
            } else {

                LOGGER.debug(String.format("Wait for %s milliseconds", waitOnPageLoad));
                try {

                    Thread.sleep(waitOnPageLoad);
                } catch (InterruptedException e) {

                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Recursively injects all shadow DOMs inside a specified XPath into the Jsoup document.
     *
     * @param document The Jsoup Document instance representing the web page.
     * @param shadowHostXPath The XPath expression used to locate shadow host elements in the web page.
     */
    public void injectAllShadowDOMs(Document document,
                                     String shadowHostXPath) {

        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        if(shadowHostXPath == null) shadowHostXPath = "//*";

        // Find all shadow hosts using the provided XPath
        List<WebElement> shadowHosts = driver.findElements(By.xpath(shadowHostXPath));

        // Loop through each shadow host element
        for (WebElement shadowHost : shadowHosts) {
            // Check if the element has a shadow root
            Boolean hasShadowRoot = (Boolean) jsExecutor.executeScript("return arguments[0].shadowRoot !== null", shadowHost);
            if (hasShadowRoot) {

                // Extract the shadow content as HTML
                String shadowContent = (String) jsExecutor.executeScript("return arguments[0].shadowRoot.innerHTML;", shadowHost);

                // Find the corresponding Jsoup element by XPath (or tag name)
                String tagName = shadowHost.getTagName();
                String jsoupXPath = tagName; // Convert tag name to XPath

                // Select the first matching element in Jsoup
                Element jsoupElement = document.selectFirst(jsoupXPath);
                if (jsoupElement != null) {
                    // Append the shadow content inside the Jsoup element without overwriting
                    jsoupElement.append(shadowContent);
                }

                // Recursively inject shadow DOM content from nested shadow roots
                injectNestedShadowDOMs(jsExecutor, document, shadowHost);
            }
        }
    }

    /**
     * Recursively injects nested shadow DOMs inside an existing shadow root.
     *
     * @param jsExecutor The JavascriptExecutor instance used to execute JavaScript in the web page.
     * @param document The Jsoup Document instance representing the web page.
     * @param shadowHost The WebElement representing the shadow host element.
     */
    private void injectNestedShadowDOMs(JavascriptExecutor jsExecutor,
                                       Document document,
                                       WebElement shadowHost) {

        // Execute JavaScript to get the shadow root and avoid casting issues
        String shadowRootContent = (String) jsExecutor.executeScript(
            "return arguments[0].shadowRoot ? arguments[0].shadowRoot.innerHTML : null;", shadowHost);

        if (shadowRootContent != null) {

            // Find the corresponding Jsoup element by tag name
            String tagName = shadowHost.getTagName();
            Element jsoupElement = document.selectFirst(tagName);
            if (jsoupElement != null) {
                jsoupElement.append(shadowRootContent); // Inject the shadow root content into Jsoup
            }

            // Recursively inject nested shadow roots (if any)
            List<WebElement> nestedElements = (List<WebElement>) jsExecutor.executeScript(
                "return Array.from(arguments[0].shadowRoot.querySelectorAll('*'))", shadowHost);

            for (WebElement nestedElement : nestedElements) {
                // Recursively process deeper shadow roots
                injectNestedShadowDOMs(jsExecutor, document, nestedElement);
            }
        }
    }

    @Override
    public CompletableFuture<Integer> getUrlStatusCode(String url) {
        return getUrlStatusCode(url, this.referrer);
    }

    @Override
    public CompletableFuture<Integer> getUrlStatusCode(String url, String currentReferrer) {

        LOGGER.debug(String.format("Checking status for url %s using webdriver", url));
        return CompletableFuture.supplyAsync(() -> {
            // Set the referrer header
            if (currentReferrer != null && !currentReferrer.isEmpty() && !currentReferrer.equalsIgnoreCase(referrer)) {

                try{
                    // Use Chrome DevTools Protocol (CDP) to set the referrer dynamically
                    DevTools devTools = ((ChromeDriver) driver).getDevTools();
                    devTools.createSession();
                    devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                    devTools.send(Network.setExtraHTTPHeaders(new Headers(Map.of("Referer", currentReferrer))));
                } catch (Exception e) {

                    LOGGER.debug("Error while trying to set referer for web driver");
                }
            }
            // Load the dynamic page
            driver.get(url);

            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object status = js.executeScript("return fetch(arguments[0], { method: 'HEAD' })" +
                                                 ".then(response => response.status)" +
                                                 ".catch(() => 0);", url);
            return status instanceof Long ? ((Long) status).intValue() : 500;
        });
    }
}
