package org.mule.extension.webcrawler.api.authenticator;

import org.mule.extension.webcrawler.api.CustomAuthenticator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class FormCookieAuthenticator implements CustomAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormCookieAuthenticator.class);
    private boolean authenticated = false;
    private Instant authenticationTime;
    private Set<Cookie> sessionCookies;
    private WebDriver registeredDriver;


    @Override
    public String getId() {
        return "formCookieAuth";
    }

    @Override
    public void configureAuthentication(WebDriver driver, Map<String, String> config) {
        try {
            String loginUrl = config.get("loginUrl");
            String username = config.get("username");
            String password = config.get("password");
            String usernameField = config.getOrDefault("usernameField", "username");
            String passwordField = config.getOrDefault("passwordField", "password");
            String submitButton = config.getOrDefault("submitButton", "input[type='submit']");
            String successIndicator = config.get("successIndicator"); // Optional

            if (loginUrl == null || username == null || password == null) {
                throw new RuntimeException("Missing required configuration in FormCookieAuthenticator.");
            }

            // Navigate to login page
            driver.get(loginUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Fill username
            WebElement usernameElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.name(usernameField))
            );
            usernameElement.clear();
            usernameElement.sendKeys(username);

            // Fill password
            WebElement passwordElement = driver.findElement(By.name(passwordField));
            passwordElement.clear();
            passwordElement.sendKeys(password);

            // Submit form
            WebElement submitElement = driver.findElement(By.cssSelector(submitButton));
            submitElement.click();

            // Wait for login completion
            if (successIndicator != null) {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(successIndicator)));
            } else {
                Thread.sleep(2000); // Simple wait if no success indicator provided
            }

            // Store session cookies and authentication time
            sessionCookies = driver.manage().getCookies();
            authenticationTime = Instant.now();
            authenticated = true;
            registeredDriver = driver;
            LOGGER.info("Form-based authentication completed successfully with {} cookies",
                    sessionCookies.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Form/Cookie authentication: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canHandleUrl(String url) {
        // Only handle if not yet authenticated, or if we need to re-authenticate
        return !authenticated;
    }

    @Override
    public boolean needsRefresh(WebDriver driver, Map<String, String> config) {
        if (registeredDriver != null && registeredDriver != driver) {
            LOGGER.debug("WebDriver instance has changed, refresh needed.");
            return true;
        }

        if (!authenticated) {
            return true;
        }

        // Check session timeout
        String sessionTimeoutStr = config.get("sessionTimeoutMinutes");
        if (sessionTimeoutStr != null) {
            try {
                long timeoutMinutes = Long.parseLong(sessionTimeoutStr);
                Duration timeSinceAuth = Duration.between(authenticationTime, Instant.now());
                boolean sessionExpired = timeSinceAuth.toMinutes() >= timeoutMinutes;

                if (sessionExpired) {
                    LOGGER.info("Session timeout reached, re-authentication needed");
                    return true;
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid sessionTimeoutMinutes value: {}", sessionTimeoutStr);
            }
        }

        // Check if critical session cookies are still present
        String criticalCookie = config.get("criticalSessionCookie");
        if (criticalCookie != null) {
            Cookie cookie = driver.manage().getCookieNamed(criticalCookie);
            if (cookie == null) {
                LOGGER.info("Critical session cookie '{}' missing, re-authentication needed", criticalCookie);
                return true;
            }
        }

        // Check if we can access a protected page to validate session
        String sessionCheckUrl = config.get("sessionCheckUrl");
        if (sessionCheckUrl != null) {
            try {
                String currentUrl = driver.getCurrentUrl();
                driver.get(sessionCheckUrl);

                // Check if redirected to login page or got unauthorized response
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Object status = js.executeScript("return document.readyState === 'complete' ? window.location.href : null;");

                if (status != null && status.toString().contains("login")) {
                    LOGGER.info("Session validation failed, redirected to login page");
                    driver.get(currentUrl); // Return to original page
                    return true;
                }

                driver.get(currentUrl); // Return to original page
            } catch (Exception e) {
                LOGGER.warn("Session validation check failed: {}", e.getMessage());
                return true;
            }
        }

        return false;
    }

    @Override
    public void cleanup() {
        authenticated = false;
        authenticationTime = null;
        registeredDriver = null;
        if (sessionCookies != null) {
            sessionCookies.clear();
            sessionCookies = null;
        }
        LOGGER.debug("Form/Cookie authenticator cleaned up");
    }
}
