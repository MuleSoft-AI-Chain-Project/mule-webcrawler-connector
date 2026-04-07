package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.mule.extension.webcrawler.api.CustomAuthenticator;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class WebDriverConnection implements WebCrawlerConnection {

    private static Logger LOGGER = LoggerFactory.getLogger(WebDriverConnection.class);

    private final Map<String, CustomAuthenticator> authenticators = new HashMap<>();

    private WebDriver driver;
    private String userAgent;
    private String referrer;
    private WebDriverProvider connectionProvider; // Reference to the provider

    public WebDriverConnection(WebDriver driver, String userAgent, String referrer, WebDriverProvider connectionProvider) {
        this.driver = driver;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.connectionProvider = connectionProvider;
        ServiceLoader<CustomAuthenticator> loader = ServiceLoader.load(CustomAuthenticator.class);
        for (CustomAuthenticator authenticator : loader) {
            authenticators.put(authenticator.getId(), authenticator);
            LOGGER.info("Discovered custom authenticator: " + authenticator.getId() + " in " + authenticator.getClass());
        }
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

    @Override
    public InputStream getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {
        LOGGER.debug(String.format("Retrieving page source for url %s using webdrive (wait %s millisec)", url, pageLoadOptions.getWaitOnPageLoad(),
                pageLoadOptions.getAuthenticationMethodId()));
        if (pageLoadOptions.getAuthenticationMethodId() != null && !pageLoadOptions.getAuthenticationMethodId().isBlank()) {
            CustomAuthenticator authenticator = authenticators.get(pageLoadOptions.getAuthenticationMethodId());

            if (authenticator != null) {
                if (authenticator.canHandleUrl(url)) {
                    // Check if refresh is needed before configuring
                    if (authenticator.needsRefresh(driver, pageLoadOptions.getAuthenticationConfiguration())) {
                        authenticator.configureAuthentication(driver, pageLoadOptions.getAuthenticationConfiguration());
                        LOGGER.info("Custom authenticator configuration {} invoked successfully", authenticator.getClass());
                    }
                }
            }
        }

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
    public Integer getUrlStatusCode(String url, String currentReferrer) {

        LOGGER.debug(String.format("Checking status for url %s using webdriver", url));
        // Load the dynamic page
        driver.get(url);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object status = js.executeScript("return fetch(arguments[0], { method: 'HEAD' })" +
                                             ".then(response => response.status)" +
                                             ".catch(() => 0);", url);
        return status instanceof Long ? ((Long) status).intValue() : 500;
    }
}
