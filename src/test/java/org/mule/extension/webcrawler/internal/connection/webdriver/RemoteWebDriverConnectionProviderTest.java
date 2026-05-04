package org.mule.extension.webcrawler.internal.connection.webdriver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

class RemoteWebDriverConnectionProviderTest {

    private RemoteWebDriverConnectionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RemoteWebDriverConnectionProvider();
    }

    @Test
    void validateNullConnectionReturnsFailure() {
        ConnectionValidationResult result = provider.validate(null);
        assertFalse(result.isValid(), "null connection should not validate");
        assertNotNull(result.getMessage());
    }

    @Test
    void validateNullDriverReturnsFailure() {
        WebDriverConnection conn = mock(WebDriverConnection.class);
        when(conn.getDriver()).thenReturn(null);

        ConnectionValidationResult result = provider.validate(conn);
        assertFalse(result.isValid());
    }

    @Test
    void validateNullSessionIdReturnsFailure() {
        WebDriverConnection conn = mock(WebDriverConnection.class);
        RemoteWebDriver driver = mock(RemoteWebDriver.class);
        when(driver.getSessionId()).thenReturn(null);
        when(conn.getDriver()).thenReturn(driver);

        ConnectionValidationResult result = provider.validate(conn);
        assertFalse(result.isValid(), "null session id should not validate");
    }

    @Test
    void validateCurrentUrlThrowsReturnsFailure() {
        WebDriverConnection conn = mock(WebDriverConnection.class);
        RemoteWebDriver driver = mock(RemoteWebDriver.class);
        when(driver.getSessionId()).thenReturn(new SessionId("sid-123"));
        when(driver.getCurrentUrl()).thenThrow(new WebDriverException("dead session"));
        when(conn.getDriver()).thenReturn(driver);

        ConnectionValidationResult result = provider.validate(conn);
        assertFalse(result.isValid(),
                "getCurrentUrl throw should cause validation failure (evict from pool)");
    }

    @Test
    void validateHappyPath() {
        WebDriverConnection conn = mock(WebDriverConnection.class);
        RemoteWebDriver driver = mock(RemoteWebDriver.class);
        when(driver.getSessionId()).thenReturn(new SessionId("sid-123"));
        when(driver.getCurrentUrl()).thenReturn("about:blank");
        when(conn.getDriver()).thenReturn(driver);

        ConnectionValidationResult result = provider.validate(conn);
        assertTrue(result.isValid(), "Healthy remote driver should validate successfully");
    }

    @Test
    void validateNonRemoteDriverSkipsSessionIdCheck() {
        // Non-RemoteWebDriver branch: sessionId check is skipped, only getCurrentUrl is exercised.
        WebDriverConnection conn = mock(WebDriverConnection.class);
        WebDriver driver = mock(WebDriver.class);
        when(driver.getCurrentUrl()).thenReturn("about:blank");
        when(conn.getDriver()).thenReturn(driver);

        ConnectionValidationResult result = provider.validate(conn);
        assertTrue(result.isValid());
    }

    @Test
    void disconnectNullIsNoOp() {
        assertDoesNotThrow(() -> provider.disconnect(null));
    }

    @Test
    void disconnectCallsQuitDriver() {
        WebDriverConnection conn = mock(WebDriverConnection.class);
        provider.disconnect(conn);
        verify(conn).quitDriver();
    }

    @Test
    void disconnectSwallowsQuitDriverException() {
        WebDriverConnection conn = mock(WebDriverConnection.class);
        doThrow(new RuntimeException("remote already gone")).when(conn).quitDriver();

        assertDoesNotThrow(() -> provider.disconnect(conn),
                "disconnect must be best-effort; quitDriver throwing must not propagate");
        verify(conn).quitDriver();
    }
}
