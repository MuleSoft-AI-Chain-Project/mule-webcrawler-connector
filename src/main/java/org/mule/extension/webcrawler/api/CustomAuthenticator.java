package org.mule.extension.webcrawler.api;

import org.openqa.selenium.WebDriver;
import java.util.Map;

public interface CustomAuthenticator {

    /**
     * Unique identifier for this authentication method.
     * @return A string ID, e.g., "myCustomOAuth", "enterpriseSSO"
     */
    String getId();

    /**
     * Performs the custom authentication logic using Selenium.
     * @param driver The Selenium WebDriver instance.
     * @param config A map of configuration properties from the Mule connector.
     */
    void configureAuthentication(WebDriver driver, Map<String, String> config);

    /**
     * Optional: Add a method to check if this authenticator is applicable for a given URL.
     * @param url The URL to check applicability against.
     * @return true if this authenticator can handle the given URL, false otherwise.
     * Default implementation always returns true, assuming applicability is managed externally
     * or this authenticator is generally applicable.
     */
    default boolean canHandleUrl(String url) {
        return true; // Default: assume it can handle any URL unless overridden
    }

    // Optional: Method to check if authentication needs to be refreshed
    default boolean needsRefresh(WebDriver driver, Map<String, String> config) {
        return false;
    }

    // Optional: Cleanup method for stateful authenticators
    default void cleanup() {
        // Default implementation does nothing
    }
}
