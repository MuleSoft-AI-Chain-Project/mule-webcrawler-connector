package org.mule.extension.webcrawler.internal.connection.webdriver;

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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WebDriverConnection implements WebCrawlerConnection {

    private static Logger LOGGER = LoggerFactory.getLogger(WebDriverConnection.class);

    private WebDriver driver;
    private String userAgent;
    private String referrer;
    private long waitDuration;
    private String waitUntilXPath;

    public WebDriverConnection(WebDriver driver, String userAgent, String referrer, long waitDuration, String waitUntilXPath) {
        this.driver = driver;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.waitDuration = waitDuration;
        this.waitUntilXPath = waitUntilXPath;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }
    @Override
    public CompletableFuture<InputStream> getPageSource(String url) {

        return getPageSource(url, this.referrer);
    }

    @Override
    public CompletableFuture<InputStream> getPageSource(String url, String currentReferrer) {

        LOGGER.debug(String.format("Retrieving page source for url %s using webdriver", url));
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

            if(waitDuration > 0L) {

                if(waitUntilXPath != null && waitUntilXPath.compareTo("") != 0) {

                    LOGGER.debug(String.format("Wait until %s for %s milliseconds", waitUntilXPath, waitDuration));

                    try {

                        new FluentWait<>(driver)
                            .withTimeout(Duration.ofMillis(waitDuration))  // Waits up to 5 seconds
                            .pollingEvery(Duration.ofMillis(500))  // Checks every 500ms
                            .ignoring(NoSuchElementException.class)
                            .until(new Function<WebDriver, Boolean>() {

                                public Boolean apply(WebDriver webDriver) {
                                    try {
                                        // Attempt to find the element by XPath
                                        WebElement element = webDriver.findElement(By.xpath(waitUntilXPath));
                                        return element != null;  // Return true when the element is found in the DOM
                                    } catch (NoSuchElementException e) {
                                        // Return false if the element is not found
                                        return false;
                                    }
                                }
                            });
                    } catch (TimeoutException e) {

                        LOGGER.warn(String.format("Element %s not found within the timeout period %s", waitUntilXPath, waitDuration));
                    }
                } else {

                    LOGGER.debug(String.format("Wait for %s milliseconds", waitDuration));
                    try {

                        Thread.sleep(waitDuration);
                    } catch (InterruptedException e) {

                        throw new RuntimeException(e);
                    }
                }
            }

            // Retrieve the page source
            String pageSource = driver.getPageSource();
            // Convert the page source to InputStream
            return new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8));
        });
    }
}
