package org.mule.extension.webcrawler.api;

import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.idealized.target.model.SessionID;
import org.openqa.selenium.devtools.v139.network.Network;
import org.openqa.selenium.devtools.v139.network.model.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HTTPHeaderBasedAuthenticator implements CustomAuthenticator {

    public static final Logger LOGGER = LoggerFactory.getLogger(HTTPHeaderBasedAuthenticator.class);

    protected Instant configuredTime;
    protected boolean configured = false;
    protected Map<String, String> currentHeaders = new HashMap<>();

    private SessionID sessionID;

    /**
     * Subclasses implement this to provide the headers needed for authentication
     */
    public abstract Map<String, String> generateAuthHeaders(WebDriver driver, Map<String, String> config) throws Exception;

    /**
     * Subclasses implement this to perform any custom cleanup
     */
    public void performCustomCleanup() {};

    /**
     * Subclasses can override this to provide custom refresh logic
     */
    public boolean shouldRefresh(Map<String, String> config) {
        return false;
    }

    @Override
    public final void configureAuthentication(WebDriver webDriver, Map<String, String> config) {
        try {
            // Generate the authentication headers
            Map<String, String> authHeaders = generateAuthHeaders(webDriver, config);

            if (authHeaders == null || authHeaders.isEmpty()) {
                LOGGER.warn("No authentication headers generated");
            } else {

                currentHeaders.clear();
                currentHeaders.putAll(authHeaders);

                // Apply headers using CDP
                if (applyHeadersWithCDP(webDriver, currentHeaders)) {
                    LOGGER.info("{} authentication configured using Chrome DevTools Protocol", getId());
                } else {
                    throw new RuntimeException("Unable to configure authentication headers");
                }

                configuredTime = Instant.now();
                configured = true;
                sessionID = ((ChromeDriver) webDriver).getDevTools().getCdpSession();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to configure " + getId() + " authentication: " + e.getMessage(), e);
        }
    }

    private boolean applyHeadersWithCDP(WebDriver driver, Map<String, String> headers) {
        try {

            DevTools devTools = ((ChromeDriver) driver).getDevTools();
            devTools.createSessionIfThereIsNotOne();

            // Required for setting HTTP headers
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

            //devTools.send(Fetch.enable(Optional.empty(), Optional.empty()));

            // Convert headers to the format expected by CDP
            Map<String, Object> cdpHeaders = new HashMap<>();
            headers.forEach(cdpHeaders::put);

            // Set custom headers using CDP
            devTools.send(Network.setExtraHTTPHeaders(new Headers(cdpHeaders)));

            LOGGER.debug("Applied {} headers using CDP: {}", headers.size(), headers.keySet());
            LOGGER.debug("Headers: {}", headers);
            return true;
        } catch (Exception e) {
            LOGGER.debug("CDP header injection failed: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public final boolean needsRefresh(WebDriver driver, Map<String, String> config) {
        if (sessionID != null && sessionID != ((ChromeDriver) driver).getDevTools().getCdpSession()) {
            LOGGER.debug("WebDriver instance has changed, refresh needed.");
            return true;
        }

        if (!configured) {
            return true;
        }

        // Check subclass-specific refresh logic
        return shouldRefresh(config);
    }

    @Override
    public final void cleanup() {
        configured = false;
        configuredTime = null;
        sessionID = null;
        currentHeaders.clear();

        performCustomCleanup();

        LOGGER.debug("{} authenticator cleaned up", getId());
    }

    // Utility methods for common refresh patterns
    public boolean isExpired(long expirySeconds, long bufferSeconds) {
        if (configuredTime == null) return true;

        Duration timeSinceConfig = Duration.between(configuredTime, Instant.now());
        return timeSinceConfig.getSeconds() >= (expirySeconds - bufferSeconds);
    }

    public boolean isExpiredByConfig(Map<String, String> config, String configKey, long defaultBufferSeconds) {
        String expiryStr = config.get(configKey);
        if (expiryStr != null) {
            try {
                long expirySeconds = Long.parseLong(expiryStr);
                return isExpired(expirySeconds, defaultBufferSeconds);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid {} value: {}", configKey, expiryStr);
            }
        }
        return false;
    }
}
