package org.mule.extension.webcrawler.api.authenticator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mule.extension.webcrawler.api.HTTPHeaderBasedAuthenticator;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class OAuthBearerTokenAuthenticator extends HTTPHeaderBasedAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthBearerTokenAuthenticator.class);

    private Long tokenExpiresIn;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getId() {
        return "bearerTokenAuth";
    }

    @Override
    public Map<String, String> generateAuthHeaders(WebDriver webDriver, Map<String, String> config) throws Exception {
        String headerName = config.getOrDefault("headerName", "Authorization");
        String prefix = config.getOrDefault("prefix", "Bearer ");

        String oauthToken = getBearerAccessToken(config);

        Map<String, String> headers = new HashMap<>();
        headers.put(headerName, prefix + oauthToken);

        LOGGER.info("OAuth bearer authentication configured with header: {}", headerName);
        return headers;
    }

    @Override
    public boolean shouldRefresh(Map<String, String> config) {
        // Check token expiry if we know the expiration time
        if (tokenExpiresIn != null && isExpired(tokenExpiresIn, 300)) { // 5 minute buffer
            LOGGER.info("Token approaching expiry, refresh needed");
            return true;
        }

        // Check configured expiry
        if (isExpiredByConfig(config, "tokenExpirySeconds", 60)) {
            LOGGER.info("Configured token expiry approaching, refresh needed");
            return true;
        }

        // Check periodic refresh
        String refreshIntervalStr = config.get("refreshIntervalHours");
        if (refreshIntervalStr != null) {
            try {
                long refreshHours = Long.parseLong(refreshIntervalStr);
                if (configuredTime != null) {
                    Duration timeSinceConfig = Duration.between(configuredTime, Instant.now());
                    if (timeSinceConfig.toHours() >= refreshHours) {
                        LOGGER.info("Periodic refresh interval reached, refresh needed");
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid refreshIntervalHours value: {}", refreshIntervalStr);
            }
        }

        return false;
    }

    @Override
    public void performCustomCleanup() {
        tokenExpiresIn = null;
    }

    private String getBearerAccessToken(Map<String, String> config) throws Exception {
        String clientId = config.get("clientId");
        String clientSecret = config.get("clientSecret");

        if (clientId == null || clientSecret == null) {
            throw new RuntimeException("Missing required OAuth configuration: clientId and clientSecret");
        }

        String tokenUrl = config.getOrDefault("tokenUrl", "http://127.0.0.1:8087/oauth2/token");
        String scope = config.get("scope");
        String requestBody = "grant_type=client_credentials";
        if (scope != null && !scope.isEmpty()) {
            requestBody += "&scope=" + scope;
        }

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OAuth token exchange failed: " + response.body());
        }

        JsonNode jsonResponse = objectMapper.readTree(response.body());

        if (jsonResponse.has("error")) {
            throw new RuntimeException("OAuth error: " + jsonResponse.get("error_description").asText());
        }

        String accessToken = jsonResponse.get("access_token").asText();
        if (jsonResponse.has("expires_in")) {
            tokenExpiresIn = jsonResponse.get("expires_in").asLong();
        }

        LOGGER.info("Successfully generated access token which expires in: {}", tokenExpiresIn);

        return accessToken;
    }
}
