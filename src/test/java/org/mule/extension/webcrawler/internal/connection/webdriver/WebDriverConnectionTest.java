package org.mule.extension.webcrawler.internal.connection.webdriver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.webcrawler.api.CustomAuthenticator;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.testsupport.TestCustomAuthenticator;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

class WebDriverConnectionTest {

    // A mock class that implements both WebDriver and JavascriptExecutor so
    // the connection's `(JavascriptExecutor) driver` cast succeeds.
    interface JsDriver extends WebDriver, JavascriptExecutor {}

    private JsDriver driver;
    private WebDriverProvider provider;

    @BeforeEach
    void setUp() {
        driver = mock(JsDriver.class);
        provider = mock(WebDriverProvider.class);
        TestCustomAuthenticator.reset();
    }

    @AfterEach
    void tearDown() {
        TestCustomAuthenticator.reset();
    }

    // ---------- constructor / ServiceLoader discovery ----------

    @Test
    void constructorDoesNotThrow() {
        // ServiceLoader discovery is observably verified downstream by the
        // getPageSourceAuthentication* tests, which dispatch against the
        // registered TestCustomAuthenticator id. This smoke test only locks
        // in that the constructor does not throw when the ServiceLoader
        // returns a non-empty set of CustomAuthenticator SPIs.
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertNotNull(conn);
    }

    // ---------- restartDriver ----------

    @Test
    void restartDriverPooledIsNoOp() {
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider, /*pooled*/ true);
        conn.restartDriver();
        verify(driver, never()).quit();
        verify(provider, never()).createNewWebDriver();
    }

    @Test
    void restartDriverNonPooledQuitsAndRecreates() {
        JsDriver fresh = mock(JsDriver.class);
        when(provider.createNewWebDriver()).thenReturn(fresh);
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider, /*pooled*/ false);
        conn.restartDriver();
        verify(driver, times(1)).quit();
        verify(provider, times(1)).createNewWebDriver();
        assertEquals(fresh, conn.getDriver(), "driver should have been replaced");
    }

    @Test
    void restartDriverHandlesQuitException() {
        JsDriver fresh = mock(JsDriver.class);
        doThrow(new WebDriverException("boom")).when(driver).quit();
        when(provider.createNewWebDriver()).thenReturn(fresh);
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider, false);
        assertDoesNotThrow(conn::restartDriver);
        verify(provider).createNewWebDriver();
        assertEquals(fresh, conn.getDriver());
    }

    // ---------- quitDriver ----------

    @Test
    void quitDriverNullIsNoOp() {
        WebDriverConnection conn = new WebDriverConnection(null, "ua", "ref", provider);
        assertDoesNotThrow(conn::quitDriver);
    }

    @Test
    void quitDriverSwallowsException() {
        doThrow(new WebDriverException("dead")).when(driver).quit();
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertDoesNotThrow(conn::quitDriver);
        assertNull(conn.getDriver(), "driver nulled out in finally");
    }

    @Test
    void quitDriverHappyPath() {
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        conn.quitDriver();
        verify(driver).quit();
        assertNull(conn.getDriver());
    }

    // ---------- getPageSource — auth dispatch branches ----------

    /** Stubs the standard readyState loop to return "complete" immediately. */
    private void stubReadyStateComplete() {
        when(driver.executeScript(anyString())).thenReturn("complete");
        when(driver.getPageSource()).thenReturn("<html></html>");
    }

    @Test
    void getPageSourceNoAuthenticationId() throws Exception {
        stubReadyStateComplete();
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null, null, null);

        InputStream in = conn.getPageSource("https://x", "ref", opts);
        String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("<html"));
        verify(driver).get("https://x");
    }

    @Test
    void getPageSourceAuthenticationIdNotRegistered() throws Exception {
        stubReadyStateComplete();
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null, "unknownAuthId", null);

        // Should just fall through (authenticator == null).
        conn.getPageSource("https://x", "ref", opts);
        assertEquals(0, TestCustomAuthenticator.configureCount.get());
    }

    @Test
    void getPageSourceAuthenticationIdCanHandleFalse() throws Exception {
        stubReadyStateComplete();
        TestCustomAuthenticator.canHandleUrlReturn = false;
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null,
                "testCustomAuthenticator", Collections.emptyMap());

        conn.getPageSource("https://x", "ref", opts);
        assertEquals(1, TestCustomAuthenticator.canHandleUrlCount.get());
        assertEquals(0, TestCustomAuthenticator.configureCount.get(),
                "canHandleUrl=false -> configureAuthentication not called");
    }

    @Test
    void getPageSourceAuthenticationIdNeedsRefreshFalse() throws Exception {
        stubReadyStateComplete();
        TestCustomAuthenticator.canHandleUrlReturn = true;
        TestCustomAuthenticator.needsRefreshReturn = false;
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null,
                "testCustomAuthenticator", Collections.emptyMap());

        conn.getPageSource("https://x", "ref", opts);
        assertEquals(1, TestCustomAuthenticator.needsRefreshCount.get());
        assertEquals(0, TestCustomAuthenticator.configureCount.get(),
                "needsRefresh=false -> skip configure");
    }

    @Test
    void getPageSourceAuthenticationFullyInvoked() throws Exception {
        stubReadyStateComplete();
        TestCustomAuthenticator.canHandleUrlReturn = true;
        TestCustomAuthenticator.needsRefreshReturn = true;
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null,
                "testCustomAuthenticator", Collections.emptyMap());

        conn.getPageSource("https://x", "ref", opts);
        assertEquals(1, TestCustomAuthenticator.configureCount.get(),
                "full path should call configureAuthentication exactly once");
    }

    @Test
    void getPageSourceBlankAuthenticationIdSkipsDispatch() throws Exception {
        stubReadyStateComplete();
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null, "   ", null);
        conn.getPageSource("https://x", "ref", opts);
        assertEquals(0, TestCustomAuthenticator.canHandleUrlCount.get());
    }

    // ---------- getPageSource — javascript + waitForXPath branches ----------

    @Test
    void getPageSourceWithJavascriptExecutesScript() throws Exception {
        stubReadyStateComplete();
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, null, false, null, null, null,
                "console.log('hi')");

        conn.getPageSource("https://x", "ref", opts);
        // One executeScript for readyState, one for the javascript option.
        verify(driver, times(1)).executeScript("console.log('hi')");
    }

    @Test
    void getPageSourceWithWaitForXPathFound() throws Exception {
        // Stub readyState complete.
        when(driver.executeScript(anyString())).thenReturn("complete");
        when(driver.getPageSource()).thenReturn("<html></html>");
        when(driver.findElement(any(By.class))).thenReturn(mock(WebElement.class));
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(1000L, "//body", false, null, null, null);

        conn.getPageSource("https://x", "ref", opts);
        verify(driver).findElement(any(By.class));
    }

    @Test
    void getPageSourceWithWaitForXPathNotFoundLogsAndContinues() throws Exception {
        when(driver.executeScript(anyString())).thenReturn("complete");
        when(driver.getPageSource()).thenReturn("<html></html>");
        // Throw NoSuchElementException each poll; FluentWait eventually times out.
        when(driver.findElement(any(By.class)))
                .thenThrow(new NoSuchElementException("no"));
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        // tiny timeout so the test completes fast. Note FluentWait uses seconds, so 1s is smallest reasonable.
        PageLoadOptions opts = new PageLoadOptions(1L, "//nope", false, null, null, null);

        assertDoesNotThrow(() -> conn.getPageSource("https://x", "ref", opts));
    }

    @Test
    void getPageSourceNullWaitOnPageLoadUsesDefault() throws Exception {
        stubReadyStateComplete();
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        PageLoadOptions opts = new PageLoadOptions(null, null, false, null, null, null);
        // Null timeout -> Optional.orElse(30000L), still works.
        conn.getPageSource("https://x", "ref", opts);
        verify(driver).get("https://x");
    }

    // ---------- getUrlStatusCode ----------

    @Test
    void getUrlStatusCodeLongIsReturnedAsInt() {
        when(driver.executeScript(anyString(), eq("https://x"))).thenReturn(Long.valueOf(200));
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertEquals(200, conn.getUrlStatusCode("https://x", null));
    }

    @Test
    void getUrlStatusCodeNonLongFallsBackTo500() {
        // Return a String (not Long) -> fallback 500.
        when(driver.executeScript(anyString(), eq("https://x"))).thenReturn("something");
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertEquals(500, conn.getUrlStatusCode("https://x", null));
    }

    // ---------- injectAllShadowDOMs ----------

    @Test
    void injectAllShadowDOMsNoShadowHosts() {
        when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
        Document doc = Jsoup.parse("<html><body><div></div></body></html>");
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertDoesNotThrow(() -> conn.injectAllShadowDOMs(doc, "//div"));
    }

    @Test
    void injectAllShadowDOMsWithShadowHostPresent() {
        WebElement host = mock(WebElement.class);
        when(host.getTagName()).thenReturn("custom-el");
        when(driver.findElements(any(By.class))).thenReturn(Arrays.asList(host));
        // First script call: hasShadowRoot => true; Second: innerHTML; Third in nested recursion: null (stop)
        // Fourth: nestedElements.
        when(driver.executeScript(eq("return arguments[0].shadowRoot !== null"), eq(host))).thenReturn(true);
        when(driver.executeScript(eq("return arguments[0].shadowRoot.innerHTML;"), eq(host)))
                .thenReturn("<span>inner</span>");
        // Nested recursion path:
        when(driver.executeScript(
                eq("return arguments[0].shadowRoot ? arguments[0].shadowRoot.innerHTML : null;"), eq(host)))
                .thenReturn("<span>nested</span>");
        when(driver.executeScript(
                eq("return Array.from(arguments[0].shadowRoot.querySelectorAll('*'))"), eq(host)))
                .thenReturn(Collections.emptyList());

        Document doc = Jsoup.parse("<html><body><custom-el></custom-el></body></html>");
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        conn.injectAllShadowDOMs(doc, null); // null -> defaults to "//*"
        assertTrue(doc.html().contains("inner") || doc.html().contains("nested"),
                "shadow content appended to Jsoup element");
    }

    @Test
    void injectAllShadowDOMsHostWithoutShadowRoot() {
        WebElement host = mock(WebElement.class);
        when(driver.findElements(any(By.class))).thenReturn(Arrays.asList(host));
        when(driver.executeScript(eq("return arguments[0].shadowRoot !== null"), eq(host))).thenReturn(false);

        Document doc = Jsoup.parse("<html><body><div></div></body></html>");
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertDoesNotThrow(() -> conn.injectAllShadowDOMs(doc, "//div"));
        // innerHTML script should NOT be called when hasShadowRoot=false.
        verify(driver, never()).executeScript(eq("return arguments[0].shadowRoot.innerHTML;"), eq(host));
    }

    @Test
    void injectAllShadowDOMsNestedRecursion() {
        WebElement host = mock(WebElement.class);
        WebElement nested = mock(WebElement.class);
        when(host.getTagName()).thenReturn("outer");
        when(nested.getTagName()).thenReturn("inner");
        when(driver.findElements(any(By.class))).thenReturn(Arrays.asList(host));

        when(driver.executeScript(eq("return arguments[0].shadowRoot !== null"), eq(host))).thenReturn(true);
        when(driver.executeScript(eq("return arguments[0].shadowRoot.innerHTML;"), eq(host)))
                .thenReturn("<outer-child/>");

        // Nested: host -> inner html non-null, nestedElements list=[nested], then nested -> null stops recursion
        when(driver.executeScript(
                eq("return arguments[0].shadowRoot ? arguments[0].shadowRoot.innerHTML : null;"), eq(host)))
                .thenReturn("<deep/>");
        when(driver.executeScript(
                eq("return Array.from(arguments[0].shadowRoot.querySelectorAll('*'))"), eq(host)))
                .thenReturn(Arrays.asList(nested));
        when(driver.executeScript(
                eq("return arguments[0].shadowRoot ? arguments[0].shadowRoot.innerHTML : null;"), eq(nested)))
                .thenReturn(null);

        Document doc = Jsoup.parse("<html><body><outer></outer><inner></inner></body></html>");
        WebDriverConnection conn = new WebDriverConnection(driver, "ua", "ref", provider);
        assertDoesNotThrow(() -> conn.injectAllShadowDOMs(doc, "//outer"));
    }

    // ---------- getUserAgent / getReferrer passthroughs ----------

    @Test
    void getUserAgentAndReferrer() {
        WebDriverConnection conn = new WebDriverConnection(driver, "agent-x", "ref-y", provider);
        assertEquals("agent-x", conn.getUserAgent());
        assertEquals("ref-y", conn.getReferrer());
        assertEquals(driver, conn.getDriver());
    }
}
