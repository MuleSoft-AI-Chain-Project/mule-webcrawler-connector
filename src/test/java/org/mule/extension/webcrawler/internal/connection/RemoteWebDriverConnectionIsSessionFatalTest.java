package org.mule.extension.webcrawler.internal.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.webcrawler.internal.connection.provider.RemoteWebDriverConnectionProvider;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

/**
 * Locks in the {@code isSessionFatal} matrix that drives the recovery path: built-in CloudHub remote-browser failure modes,
 * vendor-specific patterns supplied via provider config, and the session-typed Selenium exceptions. A regression that softens any
 * of these matchers would silently leave dead drivers in place — the next fetch would hang on a worker-lost session instead of
 * rebuilding fresh.
 */
public class RemoteWebDriverConnectionIsSessionFatalTest {

    private RemoteWebDriverConnection newConnection(java.util.List<String> extraPatterns) {
        RemoteWebDriverConnectionProvider provider = mock(RemoteWebDriverConnectionProvider.class);
        when(provider.getSessionFatalPatterns()).thenReturn(extraPatterns);
        WebDriver driver = mock(WebDriver.class);
        return new RemoteWebDriverConnection(provider, driver, null, null);
    }

    @Test
    public void sessionNotCreatedException_isFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        assertThat(c.isSessionFatal(new SessionNotCreatedException("nope")), is(true));
    }

    @Test
    public void noSuchSessionException_isFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        assertThat(c.isSessionFatal(new NoSuchSessionException("gone")), is(true));
    }

    @Test
    public void webdriverUnavailableMessage_isFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        assertThat(c.isSessionFatal(new WebDriverException("upstream returned webdriver-unavailable: 503")),
                   is(true));
    }

    @Test
    public void invalidSessionIdMessage_isFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        assertThat(c.isSessionFatal(new WebDriverException("invalid session id (after worker restart)")),
                   is(true));
    }

    @Test
    public void sessionRouteTimeoutMessage_isFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        // Built-in matcher requires both 'timeout exceeded' AND '/session/' to be present —
        // narrow on purpose to avoid false-positives on element-find timeouts.
        assertThat(c.isSessionFatal(new WebDriverException("timeout exceeded calling /session/abc/url")),
                   is(true));
    }

    @Test
    public void elementFindTimeoutMessage_isNotFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        // 'timeout exceeded' alone, without /session/, must NOT mark the session as gone —
        // it's just a slow element wait, the session is still alive.
        assertThat(c.isSessionFatal(new WebDriverException("timeout exceeded waiting for element")),
                   is(false));
    }

    @Test
    public void unrelatedWebDriverException_isNotFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        assertThat(c.isSessionFatal(new WebDriverException("element click intercepted")), is(false));
    }

    @Test
    public void vendorPattern_browserStack_isFatal() {
        RemoteWebDriverConnection c = newConnection(Arrays.asList("session not found"));
        assertThat(c.isSessionFatal(new WebDriverException("BrowserStack: session not found in pool")),
                   is(true));
    }

    @Test
    public void vendorPattern_sauceLabs_isFatal() {
        RemoteWebDriverConnection c = newConnection(Arrays.asList("session expired"));
        assertThat(c.isSessionFatal(new WebDriverException("SauceLabs: session expired after 90 minutes")),
                   is(true));
    }

    @Test
    public void vendorPattern_caseInsensitive() {
        RemoteWebDriverConnection c = newConnection(Arrays.asList("ABANDONED"));
        assertThat(c.isSessionFatal(new WebDriverException("Worker reported session abandoned")),
                   is(true));
    }

    @Test
    public void emptyVendorPatterns_dontFalsePositive() {
        RemoteWebDriverConnection c = newConnection(Arrays.asList("", null));
        assertThat(c.isSessionFatal(new WebDriverException("just a regular failure")), is(false));
    }

    @Test
    public void wrappedCause_isFatalIfAnyCauseMatches() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        Throwable wrapped = new RuntimeException("outer",
                                                 new RuntimeException("middle",
                                                                      new NoSuchSessionException("inner")));
        assertThat(c.isSessionFatal(wrapped), is(true));
    }

    @Test
    public void nonWebDriverException_isNotFatal() {
        RemoteWebDriverConnection c = newConnection(Collections.emptyList());
        assertThat(c.isSessionFatal(new RuntimeException("invalid session id")), is(false));
    }
}
