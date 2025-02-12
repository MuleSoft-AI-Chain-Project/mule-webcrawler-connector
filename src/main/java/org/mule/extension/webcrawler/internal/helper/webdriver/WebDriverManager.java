package org.mule.extension.webcrawler.internal.helper.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

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

    private static WebDriver setupWebDriver(String userAgent) throws IOException, InterruptedException {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode
        options.addArguments("--disable-gpu"); // Disable GPU acceleration (optional)
        options.addArguments("--no-sandbox"); // Recommended for headless mode in Docker or CI environments
        options.addArguments("--disable-dev-shm-usage"); // Recommended for limited resources
        options.addArguments("--allow-running-insecure-content"); // Allow HTTP content on HTTPS pages
        if(!userAgent.isEmpty()) options.addArguments("--user-agent=\"" + userAgent + "\"");

        if (CloudHubChromeConfigurer.isCloudHubDeployment()) {
            CloudHubChromeConfigurer.setup();
            String tempDir = Files.createTempDirectory("chrome-profile").toString();
            options.setBinary(CloudHubChromeConfigurer.CHROME_LIB_WRAPPER_SCRIPT);
            options.addArguments("--user-data-dir=" + tempDir);
        }

        driver = new ChromeDriver(options);
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
