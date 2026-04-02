package org.mule.extension.webcrawler.api.authenticator;

import org.mule.extension.webcrawler.api.HTTPHeaderBasedAuthenticator;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

public class ApiKeyAuthenticator extends HTTPHeaderBasedAuthenticator {

    @Override
    public String getId() {
        return "apiKey";
    }

    @Override
    public Map<String, String> generateAuthHeaders(WebDriver driver, Map<String, String> config) throws Exception {
        String apiKey = config.get("apiKey");
        String keyName = config.get("keyName");
        String location = config.getOrDefault("location", "header");

        if (apiKey == null || keyName == null) {
            throw new RuntimeException("Missing 'apiKey' or 'keyName' configuration");
        }

        if (!"header".equalsIgnoreCase(location)) {
            throw new RuntimeException("Only header-based API key authentication supported in this implementation");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(keyName, apiKey);

        LOGGER.info("API Key authentication configured in header: {}", keyName);
        return headers;
    }
}

