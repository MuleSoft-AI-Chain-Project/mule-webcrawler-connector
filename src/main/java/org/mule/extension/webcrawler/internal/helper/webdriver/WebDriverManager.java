package org.mule.extension.webcrawler.internal.helper.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.JavascriptExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebDriverManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverManager.class);

    private static WebDriver driver;

    private WebDriverManager() {}

    public static WebDriver getDriver(String userAgent) throws IOException, InterruptedException {
        if (driver == null) {
            synchronized (WebDriverManager.class) {
                if (driver == null) {
                    driver = setupWebDriver(userAgent);
                }
            }
        }
        return driver;
    }

    private static WebDriver setupWebDriver(String userAgent) {

        ChromeOptions options = new ChromeOptions();

        if (CloudHubChromeConfigurer.isCloudHubDeployment()) {
            CloudHubChromeConfigurer.setup();
            options.setBinary(CloudHubChromeConfigurer.CHROME_LIB_WRAPPER_SCRIPT);
        } else {
            options.addArguments("--headless"); // NON CH only; CloudHub already uses headless chrome
        }

        options.addArguments("--disable-gpu"); // Disable GPU acceleration
        options.addArguments("--no-sandbox"); // Recommended for headless mode in Docker or CI environments
        options.addArguments("--disable-dev-shm-usage"); // Recommended for limited resources
        options.addArguments("--allow-running-insecure-content"); // Allow HTTP content on HTTPS pages
        if(!userAgent.isEmpty()) options.addArguments("--user-agent=" + userAgent);

        driver = new ChromeDriver(options);

        String actualUserAgent = (String) ((JavascriptExecutor) driver).executeScript("return navigator.userAgent;");
        LOGGER.info("User Agent: {}", actualUserAgent);

        return driver;
    }

    public static void quitDriver() {
        if (driver != null) {
            LOGGER.debug("Quitting Selenium WebDriver");
            driver.quit();
            driver = null;
        }
    }
}
