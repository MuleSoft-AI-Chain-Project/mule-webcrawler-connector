package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.helper.webdriver.CloudHubChromeConfigurer;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v135.fetch.Fetch;
import org.openqa.selenium.devtools.v135.page.Page;
import org.openqa.selenium.devtools.v135.runtime.Runtime;
import org.openqa.selenium.devtools.v135.overlay.Overlay;
import org.openqa.selenium.devtools.v135.log.Log;
import org.openqa.selenium.devtools.v135.network.Network;
import org.openqa.selenium.devtools.v135.network.model.Headers;
import org.openqa.selenium.devtools.v135.performance.Performance;
import org.openqa.selenium.devtools.v135.security.Security;
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

public class WebDriverConnection implements WebCrawlerConnection {

    private static Logger LOGGER = LoggerFactory.getLogger(WebDriverConnection.class);

    private WebDriver driver;
    private String userAgent;
    private String referrer;
    private WebDriverConnectionProvider connectionProvider; // Reference to the provider
    private DevTools devTools;

    public WebDriverConnection(WebDriver driver, String userAgent, String referrer, WebDriverConnectionProvider connectionProvider) {
        this.driver = driver;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.connectionProvider = connectionProvider;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    // Method to restart the driver for each new crawl
    public synchronized void restartDriver() {
        LOGGER.info("Restarting WebDriver for new crawl");
        try {
            if (this.driver != null) {
                this.driver.quit();
            }
        } catch (Exception e) {
            LOGGER.error("Error while quitting the old WebDriver: " + e.getMessage(), e);
        } finally {
            this.driver = null;
        }
        this.driver = connectionProvider.createNewWebDriver();
    }

    private void configureDevTools() {
        devTools = ((ChromeDriver) this.driver).getDevTools();
        devTools.createSession();

        // Required for setting headers
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        // Disable cache
        devTools.send(Network.setCacheDisabled(true));
        // Disable unnecessary domains for speed
        devTools.send(Log.disable());
        devTools.send(Performance.disable());
        devTools.send(Page.disable());
        devTools.send(Runtime.disable());
        // devTools.send(DOM.disable());
        devTools.send(Overlay.disable());
        devTools.send(Security.disable());
        devTools.send(Fetch.disable());
    }


    @Override
    public CompletableFuture<InputStream> getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {
        LOGGER.debug(String.format("Retrieving page source for url %s using webdrive (wait %s millisec)", url, pageLoadOptions.getWaitOnPageLoad()));
        return CompletableFuture.supplyAsync(() -> {
            // Set the referrer header
            // These CDP calls are very expensive when running in CH2 containers; so skipping as needed (should really be a configuration option)
            if (!CloudHubChromeConfigurer.isCloudHubDeployment() && currentReferrer != null && !currentReferrer.isEmpty() && !currentReferrer.equalsIgnoreCase(referrer)) {
                try{
                    if (devTools == null || devTools.getCdpSession() == null) {
                        configureDevTools();
                    }
                    Map<String, Object> headers = Map.of(
                            "User-Agent", userAgent,
                            "Referer", currentReferrer
                    );
                    devTools.send(Network.setExtraHTTPHeaders(new Headers(headers)));
                } catch (Exception e) {

                    LOGGER.debug("Error while trying to set referer for web driver");
                }
            }
            // Load the dynamic page
            driver.get(url);

            Long effectiveTimeout = Optional.ofNullable(pageLoadOptions.getWaitOnPageLoad())
                        .filter(t -> t > 0) // Keep only if greater than 0
                        .orElse(30000L);    // Default 30 seconds if waitOnPageLoad is null or 0

            // Wait for document.readyState to be complete no matter if XPath is provided or not
            JavascriptExecutor js = (JavascriptExecutor) driver;
            new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(effectiveTimeout))
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> js.executeScript("return document.readyState").equals("complete"));

            // Wait for given XPath to load
            if (pageLoadOptions.getWaitForXPath() != null && pageLoadOptions.getWaitForXPath().compareTo("") != 0) {
                waitForXPathLoad(effectiveTimeout, pageLoadOptions.getWaitForXPath());
            }

            if (pageLoadOptions.getJavascript() != null && !pageLoadOptions.getJavascript().isEmpty()) {
                LOGGER.debug(String.format("Executing javascript %s", pageLoadOptions.getJavascript()));
                executeScript(pageLoadOptions.getJavascript());
            }

            // Retrieve the page source
            String pageSource = driver.getPageSource();
            // Convert the page source to InputStream
            return new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8));
        });
    }

    private void waitForXPathLoad(Long waitOnPageLoad, String waitForXPath) {
        LOGGER.debug(String.format("Wait until %s for %s milliseconds", waitForXPath, waitOnPageLoad));
        try {
            new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(waitOnPageLoad))
                .pollingEvery(Duration.ofMillis(500))
                .until(d -> {
                    boolean finalElementPresent = false;
                    try {
                        driver.findElement(By.xpath(waitForXPath));
                        finalElementPresent = true;
                    } catch (NoSuchElementException e) {
                        // Ignore, element might not be present yet
                    }
                    return finalElementPresent;
                });
        } catch (TimeoutException e) {

            LOGGER.warn(String.format("Element %s not found within the timeout period %s", waitForXPath, waitOnPageLoad));
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

    /**
     * Executes the provided JavaScript code using the WebDriver's JavaScriptExecutor.
     *
     * @param script The JavaScript code to execute.
     */
    private void executeScript(String script) {

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(script);
    }

    @Override
    public CompletableFuture<Integer> getUrlStatusCode(String url, String currentReferrer) {

        LOGGER.debug(String.format("Checking status for url %s using webdriver", url));
        return CompletableFuture.supplyAsync(() -> {
            // Set the referrer header
            // These CDP calls are very expensive when running in CH2 containers; so skipping as needed (should really be a configuration option)
            if (!CloudHubChromeConfigurer.isCloudHubDeployment() && currentReferrer != null && !currentReferrer.isEmpty() && !currentReferrer.equalsIgnoreCase(referrer)) {
                try{
                    if (devTools == null || devTools.getCdpSession() == null) {
                        configureDevTools();
                    }
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
