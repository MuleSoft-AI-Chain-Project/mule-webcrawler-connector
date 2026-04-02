package org.mule.extension.webcrawler.api.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mule.extension.webcrawler.api.HTTPHeaderBasedAuthenticator;
import org.openqa.selenium.WebDriver;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class JwtAuthenticator extends HTTPHeaderBasedAuthenticator {

    @Override
    public String getId() {
        return "jwtAuth";
    }

    @Override
    public Map<String, String> generateAuthHeaders(WebDriver driver, Map<String, String> config) throws Exception {

        // Either get the JWT from config or by other custom means
        // See org.mule.extension.webcrawler.api.authenticator.OAuthBearerTokenAuthenticator for example
        String jwt = config.get("jwt");
        String headerName = config.getOrDefault("headerName", "Authorization");
        String prefix = config.getOrDefault("prefix", "Bearer ");

        if (jwt == null) {
            throw new RuntimeException("Missing 'jwt' configuration");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(headerName, prefix + jwt);

        LOGGER.info("JWT authentication configured with header: {}", headerName);
        return headers;
    }

}
