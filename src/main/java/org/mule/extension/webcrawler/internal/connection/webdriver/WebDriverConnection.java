package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.connection.http.HttpConnection;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v132.network.Network;
import org.openqa.selenium.devtools.v132.network.model.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
            // Retrieve the page source
            String pageSource = driver.getPageSource();
            // Convert the page source to InputStream
            return new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8));
        });
    }
}
