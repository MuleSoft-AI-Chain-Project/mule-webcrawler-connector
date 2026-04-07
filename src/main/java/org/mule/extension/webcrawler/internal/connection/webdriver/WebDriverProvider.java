package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.openqa.selenium.WebDriver;

/**
 * Interface for WebDriver providers (local or remote).
 * Allows WebDriverConnection to work with different provider implementations.
 */
public interface WebDriverProvider {

    /**
     * Creates a new WebDriver instance.
     * @return WebDriver instance
     */
    WebDriver createNewWebDriver();
}
