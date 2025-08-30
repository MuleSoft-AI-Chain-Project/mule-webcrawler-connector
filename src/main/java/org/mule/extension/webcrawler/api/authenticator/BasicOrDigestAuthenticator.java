package org.mule.extension.webcrawler.api.authenticator;

import java.net.URI;
import java.util.Map;
import java.util.function.Predicate;

import org.mule.extension.webcrawler.api.CustomAuthenticator;
import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicOrDigestAuthenticator implements CustomAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicOrDigestAuthenticator.class);
    private boolean authRegistered = false;
    private String registeredHostPattern;
    private WebDriver registeredDriver;

    @Override
    public String getId() {
        return "basicOrDigestAuth";
    }

    @Override
    public void configureAuthentication(WebDriver driver, Map<String, String> config) {
        try {
            String username = config.get("username");
            String password = config.get("password");
            String hostPattern = config.getOrDefault("hostPattern", ".*");

            if (username == null || password == null) {
                throw new RuntimeException("Missing 'username' or 'password' in BasicOrDigestAuthenticator configuration.");
            }

            if (!authRegistered && driver instanceof HasAuthentication) {
                Predicate<URI> uriPredicate = uri -> uri.getHost().matches(hostPattern);
                ((HasAuthentication) driver).register(uriPredicate, UsernameAndPassword.of(username, password));
                authRegistered = true;
                registeredHostPattern = hostPattern;
                registeredDriver = driver;
                LOGGER.info("Basic or Digest Authentication handler registered for host pattern: {}", hostPattern);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Basic or Digest authentication: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean needsRefresh(WebDriver driver, Map<String, String> config) {
        if (registeredDriver != driver) {
            LOGGER.debug("WebDriver instance has changed, refresh needed.");
            return true;
        }

        // Digest auth doesn't typically need refresh, but check if host pattern changed
        if (!authRegistered) {
            return true;
        }

        String currentHostPattern = config.getOrDefault("hostPattern", ".*");
        if (!currentHostPattern.equals(registeredHostPattern)) {
            LOGGER.info("Host pattern changed from '{}' to '{}', refresh needed",
                    registeredHostPattern, currentHostPattern);
            return true;
        }

        return false;
    }

    @Override
    public void cleanup() {
        authRegistered = false;
        registeredHostPattern = null;
        registeredDriver = null;
        LOGGER.debug("Basic or Digest authenticator cleaned up");
        // Note: Selenium doesn't provide a way to unregister authentication handlers
        // The driver would need to be recreated to truly clean up
    }
}
