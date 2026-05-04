package org.mule.extension.webcrawler.api.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class FormCookieAuthenticatorTest {

    private FormCookieAuthenticator authenticator;
    private WebDriver driver;
    private WebDriver.Options options;
    private WebElement usernameEl;
    private WebElement passwordEl;
    private WebElement submitEl;

    @BeforeEach
    void setUp() {
        authenticator = new FormCookieAuthenticator();
        // WebDriver needs to also implement JavascriptExecutor for some needsRefresh branches.
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        options = mock(WebDriver.Options.class);
        usernameEl = mock(WebElement.class);
        passwordEl = mock(WebElement.class);
        submitEl = mock(WebElement.class);

        when(driver.manage()).thenReturn(options);
        when(options.getCookies()).thenReturn(new HashSet<>(Collections.singleton(
                new Cookie("SESSION", "abc123"))));
        when(driver.findElement(By.name("username"))).thenReturn(usernameEl);
        when(driver.findElement(By.name("password"))).thenReturn(passwordEl);
        when(driver.findElement(By.cssSelector("input[type='submit']"))).thenReturn(submitEl);
    }

    @Test
    void getIdReturnsExpectedId() {
        assertEquals("formCookieAuth", authenticator.getId());
    }

    @Test
    void missingLoginUrlThrows() {
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");
        assertThrows(RuntimeException.class,
                () -> authenticator.configureAuthentication(driver, config));
    }

    @Test
    void missingUsernameThrows() {
        Map<String, String> config = new HashMap<>();
        config.put("loginUrl", "https://example.com/login");
        config.put("password", "secret");
        assertThrows(RuntimeException.class,
                () -> authenticator.configureAuthentication(driver, config));
    }

    @Test
    void missingPasswordThrows() {
        Map<String, String> config = new HashMap<>();
        config.put("loginUrl", "https://example.com/login");
        config.put("username", "alice");
        assertThrows(RuntimeException.class,
                () -> authenticator.configureAuthentication(driver, config));
    }

    @Test
    void happyPathSubmitsFormAndHarvestsCookies() {
        Map<String, String> config = new HashMap<>();
        config.put("loginUrl", "https://example.com/login");
        config.put("username", "alice");
        config.put("password", "secret");
        config.put("successIndicator", "body"); // skip Thread.sleep path
        // Stub the success-indicator presence check — returns the body element.
        when(driver.findElement(By.cssSelector("body"))).thenReturn(mock(WebElement.class));

        authenticator.configureAuthentication(driver, config);

        verify(driver).get("https://example.com/login");
        verify(usernameEl).sendKeys("alice");
        verify(passwordEl).sendKeys("secret");
        verify(submitEl).click();
        verify(options, atLeastOnce()).getCookies();
        assertTrue(readBoolean(authenticator, "authenticated"));
    }

    @Test
    void canHandleUrlFalseAfterAuthentication() {
        assertTrue(authenticator.canHandleUrl("https://example.com/page"),
                "Pre-auth canHandleUrl should be true");

        // Force authenticated state directly (no form interaction needed for this branch).
        writeBoolean(authenticator, "authenticated", true);
        assertFalse(authenticator.canHandleUrl("https://example.com/page"),
                "Post-auth canHandleUrl should be false");
    }

    @Test
    void needsRefreshTrueBeforeAuthentication() {
        assertTrue(authenticator.needsRefresh(driver, new HashMap<>()),
                "Before configureAuthentication, needsRefresh should be true (not authenticated)");
    }

    @Test
    void needsRefreshTrueWhenDriverChanges() {
        // Simulate authenticated state.
        writeBoolean(authenticator, "authenticated", true);
        writeField(authenticator, "registeredDriver", driver);

        WebDriver otherDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        assertTrue(authenticator.needsRefresh(otherDriver, new HashMap<>()),
                "Different driver instance should require refresh");
    }

    @Test
    void needsRefreshTrueWhenSessionTimeoutExpired() {
        writeBoolean(authenticator, "authenticated", true);
        writeField(authenticator, "registeredDriver", driver);
        // Authenticated far in the past.
        writeField(authenticator, "authenticationTime", Instant.now().minusSeconds(600));

        Map<String, String> config = new HashMap<>();
        config.put("sessionTimeoutMinutes", "1");

        assertTrue(authenticator.needsRefresh(driver, config),
                "Session that expired past sessionTimeoutMinutes should require refresh");
    }

    @Test
    void needsRefreshTrueWhenCriticalCookieMissing() {
        writeBoolean(authenticator, "authenticated", true);
        writeField(authenticator, "registeredDriver", driver);
        writeField(authenticator, "authenticationTime", Instant.now());

        // Override cookie lookup to return null for the critical cookie.
        when(options.getCookieNamed("JSESSIONID")).thenReturn(null);

        Map<String, String> config = new HashMap<>();
        config.put("criticalSessionCookie", "JSESSIONID");

        assertTrue(authenticator.needsRefresh(driver, config),
                "Missing critical cookie should require refresh");
    }

    @Test
    void needsRefreshTrueWhenSessionCheckRedirectsToLogin() {
        writeBoolean(authenticator, "authenticated", true);
        writeField(authenticator, "registeredDriver", driver);
        writeField(authenticator, "authenticationTime", Instant.now());

        when(driver.getCurrentUrl()).thenReturn("https://example.com/dashboard");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        when(js.executeScript(any(String.class)))
                .thenReturn("https://example.com/login");

        Map<String, String> config = new HashMap<>();
        config.put("sessionCheckUrl", "https://example.com/session-check");

        assertTrue(authenticator.needsRefresh(driver, config),
                "session-check redirect to /login should require refresh");
    }

    @Test
    void needsRefreshFalseWhenSessionHealthy() {
        writeBoolean(authenticator, "authenticated", true);
        writeField(authenticator, "registeredDriver", driver);
        writeField(authenticator, "authenticationTime", Instant.now());

        Map<String, String> config = new HashMap<>();
        // No critical cookie, no session check URL, no timeout -> healthy path.
        assertFalse(authenticator.needsRefresh(driver, config));
    }

    @Test
    void cleanupClearsCookiesAndState() {
        writeBoolean(authenticator, "authenticated", true);
        writeField(authenticator, "registeredDriver", driver);
        writeField(authenticator, "authenticationTime", Instant.now());
        Set<Cookie> cookies = new HashSet<>();
        cookies.add(new Cookie("SESSION", "abc"));
        writeField(authenticator, "sessionCookies", cookies);

        authenticator.cleanup();

        assertFalse(readBoolean(authenticator, "authenticated"));
        assertNull(readField(authenticator, "authenticationTime"));
        assertNull(readField(authenticator, "sessionCookies"));
    }

    // --- reflection helpers ---

    private static boolean readBoolean(Object t, String name) {
        try {
            Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (boolean) f.get(t);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object readField(Object t, String name) {
        try {
            Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(t);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void writeBoolean(Object t, String name, boolean value) {
        try {
            Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setBoolean(t, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void writeField(Object t, String name, Object value) {
        try {
            Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(t, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
