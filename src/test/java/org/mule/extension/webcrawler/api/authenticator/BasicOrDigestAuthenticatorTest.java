package org.mule.extension.webcrawler.api.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Credentials;
import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.WebDriver;

class BasicOrDigestAuthenticatorTest {

    private BasicOrDigestAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new BasicOrDigestAuthenticator();
    }

    @Test
    void getIdReturnsExpectedId() {
        assertEquals("basicOrDigestAuth", authenticator.getId());
    }

    @Test
    void missingUsernameThrows() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("password", "secret");
        // no username
        assertThrows(RuntimeException.class, () -> authenticator.configureAuthentication(driver, config));
    }

    @Test
    void missingPasswordThrows() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        // no password
        assertThrows(RuntimeException.class, () -> authenticator.configureAuthentication(driver, config));
    }

    @Test
    void happyPathRegistersCredentials() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        HasAuthentication hasAuth = (HasAuthentication) driver;
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");
        config.put("hostPattern", ".*example\\.com");

        authenticator.configureAuthentication(driver, config);

        verify(hasAuth, times(1)).register(any(Predicate.class), any(Supplier.class));
        assertTrue(readField(authenticator, "authRegistered"));
    }

    @Test
    void idempotentReCallDoesNotDoubleRegister() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        HasAuthentication hasAuth = (HasAuthentication) driver;
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");

        authenticator.configureAuthentication(driver, config);
        authenticator.configureAuthentication(driver, config);

        verify(hasAuth, times(1)).register(any(Predicate.class), any(Supplier.class));
    }

    @Test
    void needsRefreshTrueWhenDriverChanges() {
        WebDriver driver1 = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        WebDriver driver2 = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");

        authenticator.configureAuthentication(driver1, config);
        assertTrue(authenticator.needsRefresh(driver2, config),
                "New driver instance should require refresh");
    }

    @Test
    void needsRefreshTrueWhenHostPatternChanges() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");
        config.put("hostPattern", ".*example\\.com");

        authenticator.configureAuthentication(driver, config);

        Map<String, String> newConfig = new HashMap<>(config);
        newConfig.put("hostPattern", ".*other\\.com");
        assertTrue(authenticator.needsRefresh(driver, newConfig),
                "Changed hostPattern should require refresh");
    }

    @Test
    void needsRefreshFalseWhenUnchanged() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");
        config.put("hostPattern", ".*example\\.com");

        authenticator.configureAuthentication(driver, config);
        assertFalse(authenticator.needsRefresh(driver, config),
                "Unchanged driver+config should NOT require refresh");
    }

    @Test
    void needsRefreshTrueBeforeAnyConfigure() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");
        // Never configured -> registeredDriver is null -> driver != null -> true
        assertTrue(authenticator.needsRefresh(driver, config));
    }

    @Test
    void cleanupResetsInternalState() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasAuthentication.class));
        Map<String, String> config = new HashMap<>();
        config.put("username", "alice");
        config.put("password", "secret");
        authenticator.configureAuthentication(driver, config);
        assertTrue(readField(authenticator, "authRegistered"));

        authenticator.cleanup();

        assertFalse(readField(authenticator, "authRegistered"),
                "cleanup() should reset authRegistered to false");
    }

    private static boolean readField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (boolean) f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Could not read field " + name, e);
        }
    }
}
