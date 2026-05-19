package org.mule.extension.webcrawler.internal.connection;

import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.provider.RemoteWebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.model.WebCrawlerResponse;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives a remote Selenium W3C WebDriver endpoint that hosts a Chromium-family browser (Chrome, Chromium, Edge).
 *
 * <p>
 * Replaces the embedded-Chrome connection that shipped on master. The browser no longer runs in-process — this connection is a
 * thin Selenium client driving an external Selenium Grid, standalone ChromeDriver, BrowserStack/Sauce Labs, or a CloudHub-hosted
 * remote-browser app.
 * </p>
 *
 * <p>
 * <b>Session recovery.</b> Selenium throws session-fatal exceptions when the worker has lost the session
 * ({@link SessionNotCreatedException}, {@link NoSuchSessionException}, or {@link WebDriverException} chains whose message
 * indicates {@code webdriver-unavailable}, {@code invalid session id}, or session-route timeouts). On those, this connection
 * drops the driver reference without calling {@link WebDriver#quit()} — calling DELETE on a worker-lost session blocks until the
 * worker's HTTP timeout fires (~120s on CloudHub remote-browser today), so issuing it would be pure latency tax.
 * </p>
 */
public class RemoteWebDriverConnection implements WebCrawlerConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverConnection.class);

    private static final long DEFAULT_WAIT_ON_PAGE_LOAD_MS = 30000L;

    private final RemoteWebDriverConnectionProvider provider;
    private final String userAgent;
    private final String referrer;

    // WebDriver instances are NOT thread-safe. The single shared instance is safe today because the
    // crawl-website-source consumes the queue with a single thread. If concurrency ever rises, switch
    // to a driver pool first.
    private volatile WebDriver driver;

    public RemoteWebDriverConnection(RemoteWebDriverConnectionProvider provider, WebDriver driver, String userAgent,
                                     String referrer) {
        this.provider = provider;
        this.driver = driver;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String getReferrer() {
        return referrer;
    }

    @Override
    public CompletableFuture<InputStream> getPageSource(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {
        return CompletableFuture.supplyAsync(() -> {
            WebCrawlerResponse response = fetchPage(url, currentReferrer, pageLoadOptions);
            return response.getBody() != null ? response.getBody() : new ByteArrayInputStream(new byte[0]);
        });
    }

    @Override
    public CompletableFuture<Integer> getUrlStatusCode(String url, String currentReferrer) {
        return CompletableFuture.supplyAsync(() -> {
            // The remote browser doesn't expose HTTP status to the W3C protocol on plain GETs. The connector
            // dispatches an in-page fetch() to harvest the status — same approach the embedded driver used.
            try {
                ensureDriver();
                driver.get(url);
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Object status = js.executeScript(
                                                 "return fetch(arguments[0], { method: 'HEAD' })"
                                                         + ".then(response => response.status)"
                                                         + ".catch(() => 0);",
                                                 url);
                return status instanceof Long ? ((Long) status).intValue() : 500;
            } catch (Exception e) {
                if (isSessionFatal(e)) {
                    dropDeadSession();
                }
                LOGGER.debug("Status check failed for {}: {}", url, e.getMessage());
                return 500;
            }
        });
    }

    /**
     * Synchronous page fetch tailored for the {@code crawl-website-source} Source. Returns status code, content type, and body
     * together so the Source can filter and route per-page events without a second probe call.
     *
     * <p>
     * Page-load wait params (wait-on-page-load, wait-for-xpath) are wired here to the W3C driver's wait primitives. Shadow-DOM
     * extraction is handled separately by {@link #injectAllShadowDOMs(Document, String)} — callers pass the parsed jsoup
     * {@link Document} after fetch. See {@link org.mule.extension.webcrawler.internal.helper.page.PageHelper#getDocument} for the
     * orchestration.
     * </p>
     */
    @Override
    public WebCrawlerResponse fetchPage(String url, String currentReferrer, PageLoadOptions pageLoadOptions) {
        LOGGER.debug("RemoteWebDriver fetch: {}", url);
        try {
            ensureDriver();
            driver.get(url);

            Long effectiveTimeout = Optional.ofNullable(pageLoadOptions != null ? pageLoadOptions.getWaitOnPageLoad() : null)
                    .filter(t -> t > 0)
                    .orElse(DEFAULT_WAIT_ON_PAGE_LOAD_MS);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            new FluentWait<>(driver)
                    .withTimeout(Duration.ofMillis(effectiveTimeout))
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> "complete".equals(js.executeScript("return document.readyState")));

            if (pageLoadOptions != null
                    && pageLoadOptions.getWaitForXPath() != null
                    && !pageLoadOptions.getWaitForXPath().isEmpty()) {
                waitForXPathLoad(effectiveTimeout, pageLoadOptions.getWaitForXPath());
            }

            if (pageLoadOptions != null
                    && pageLoadOptions.getJavascript() != null
                    && !pageLoadOptions.getJavascript().isEmpty()) {
                executeScript(pageLoadOptions.getJavascript());
            }

            String pageSource = driver.getPageSource();
            byte[] bodyBytes = pageSource != null ? pageSource.getBytes(StandardCharsets.UTF_8) : new byte[0];

            // Probe for the navigation's real HTTP status via the Performance API
            // (Chromium 109+ exposes it on PerformanceResourceTiming.responseStatus).
            // When the probe returns a usable number, surface it; otherwise emit a
            // -1/null sentinel so consumers can recognise "status unknown" instead
            // of trusting a hardcoded 200. The W3C WebDriver protocol itself doesn't
            // expose the status, so this probe is the cheapest path that doesn't
            // require a separate network round-trip.
            int statusCode = probeNavigationStatus(js);
            String contentType = statusCode > 0 ? "text/html" : null;
            return new WebCrawlerResponse(statusCode, contentType, new ByteArrayInputStream(bodyBytes));
        } catch (ConnectionException e) {
            // ensureDriver() couldn't build a fresh driver (remote unreachable, malformed URL,
            // worker rejected session creation). Preserve the typed signal as the cause of a
            // MuleRuntimeException so Mule's connection-management layer can identify it as a
            // reconnectable failure if a per-iteration connect strategy is ever introduced.
            throw new MuleRuntimeException(
                                           I18nMessageFactory
                                                   .createStaticMessage("Remote WebDriver fetch failed for URL: " + url),
                                           e);
        } catch (Exception e) {
            if (isSessionFatal(e)) {
                // Session is already dead worker-side. Skip driver.quit() — calling DELETE on a session the
                // worker has lost stalls until the worker's HTTP responseTimeout fires. Just null out our
                // references and let the worker's TTL reaper clean up the orphan; the next fetch builds fresh.
                LOGGER.warn("WebDriver session appears dead ({}: {}). Dropping driver without quit() so the next fetch "
                        + "rebuilds a fresh session immediately.", e.getClass().getSimpleName(), e.getMessage());
                dropDeadSession();
            }
            throw new RuntimeException("Remote WebDriver fetch failed for URL: " + url, e);
        }
    }

    // ---------------------------------------------------------------------
    // Page-load wait helpers
    // ---------------------------------------------------------------------

    private void waitForXPathLoad(Long waitOnPageLoad, String waitForXPath) {
        LOGGER.debug("Wait until {} for {} milliseconds", waitForXPath, waitOnPageLoad);
        try {
            new FluentWait<>(driver)
                    .withTimeout(Duration.ofMillis(waitOnPageLoad))
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> {
                        try {
                            driver.findElement(By.xpath(waitForXPath));
                            return true;
                        } catch (NoSuchElementException e) {
                            return false;
                        }
                    });
        } catch (TimeoutException e) {
            LOGGER.warn("Element {} not found within the timeout period {}", waitForXPath, waitOnPageLoad);
        }
    }

    private void executeScript(String script) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(script);
    }

    /**
     * Returns the HTTP status code of the just-completed navigation by reading {@code PerformanceResourceTiming.responseStatus}
     * from the Performance API. Available on Chromium 109+; older browsers return {@code undefined} and we fall back to
     * {@code -1} so callers can distinguish "unknown" from a real status.
     *
     * <p>
     * Cross-origin redirects, opaque responses, and pages served outside the navigation entry produce {@code 0} or
     * {@code undefined}; both collapse to {@code -1} here. Downstream consumers should treat any non-positive value as "status
     * unavailable, do not assume success."
     * </p>
     */
    private int probeNavigationStatus(JavascriptExecutor js) {
        try {
            Object result = js.executeScript(
                                             "var nav = performance.getEntriesByType('navigation')[0];"
                                                     + "return (nav && typeof nav.responseStatus === 'number') ? nav.responseStatus : -1;");
            if (result instanceof Long) {
                long status = (Long) result;
                return status > 0 ? (int) status : -1;
            }
            return -1;
        } catch (Exception e) {
            LOGGER.debug("Status probe failed: {}", e.getMessage());
            return -1;
        }
    }

    // ---------------------------------------------------------------------
    // Shadow DOM injection — invoked by PageHelper.getDocument when extractShadowDom=true.
    // ---------------------------------------------------------------------

    /**
     * Recursively injects all shadow DOMs inside a specified XPath into the jsoup document.
     *
     * <p>
     * Tracks per-tag occurrence counts so when a page has multiple instances of the same shadow-host tag (e.g. several
     * {@code <my-card>} components), each host's shadow content is appended to its own jsoup element rather than collapsing
     * everything onto the first occurrence. Master's embedded-Chrome implementation used {@code selectFirst(tagName)} and
     * silently merged shadow DOMs from later hosts onto the first one — fixed here.
     * </p>
     */
    public void injectAllShadowDOMs(Document document, String shadowHostXPath) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        String hostXPath = shadowHostXPath == null ? "//*" : shadowHostXPath;

        List<WebElement> shadowHosts = driver.findElements(By.xpath(hostXPath));
        // Per-tag cursor — Selenium's findElements returns hosts in document order, jsoup's
        // select(tag) does too, so the Nth shadow host of a given tag matches the Nth jsoup
        // element with that tag.
        java.util.Map<String, Integer> tagCursor = new java.util.HashMap<>();

        for (WebElement shadowHost : shadowHosts) {
            Boolean hasShadowRoot = (Boolean) jsExecutor.executeScript(
                                                                       "return arguments[0].shadowRoot !== null", shadowHost);
            if (Boolean.TRUE.equals(hasShadowRoot)) {
                String shadowContent = (String) jsExecutor.executeScript(
                                                                         "return arguments[0].shadowRoot.innerHTML;", shadowHost);

                String tagName = shadowHost.getTagName();
                appendToNthJsoupElement(document, tagName, tagCursor, shadowContent);

                injectNestedShadowDOMs(jsExecutor, document, shadowHost, tagCursor);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void injectNestedShadowDOMs(JavascriptExecutor jsExecutor, Document document, WebElement shadowHost,
                                        java.util.Map<String, Integer> tagCursor) {
        String shadowRootContent = (String) jsExecutor.executeScript(
                                                                     "return arguments[0].shadowRoot ? arguments[0].shadowRoot.innerHTML : null;",
                                                                     shadowHost);

        if (shadowRootContent != null) {
            String tagName = shadowHost.getTagName();
            appendToNthJsoupElement(document, tagName, tagCursor, shadowRootContent);

            List<WebElement> nestedElements = (List<WebElement>) jsExecutor.executeScript(
                                                                                          "return Array.from(arguments[0].shadowRoot.querySelectorAll('*'))",
                                                                                          shadowHost);

            for (WebElement nestedElement : nestedElements) {
                injectNestedShadowDOMs(jsExecutor, document, nestedElement, tagCursor);
            }
        }
    }

    /**
     * Appends {@code content} to the Nth jsoup element of {@code tagName} where N is incremented in {@code tagCursor}. Falls back
     * to {@code selectFirst} (master parity) if the index is out of range — better to merge onto the first host than drop the
     * content silently.
     */
    private static void appendToNthJsoupElement(Document document, String tagName,
                                                java.util.Map<String, Integer> tagCursor, String content) {
        int idx = tagCursor.getOrDefault(tagName, 0);
        org.jsoup.select.Elements matches = document.select(tagName);
        Element target = idx < matches.size() ? matches.get(idx) : document.selectFirst(tagName);
        if (target != null) {
            target.append(content);
        }
        tagCursor.put(tagName, idx + 1);
    }

    // ---------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------

    /**
     * Re-creates the driver if it has been dropped due to a session-fatal error. The provider holds the connection details
     * (remote URL, blocked-URLs cap, etc.) and knows how to spin up a fresh driver.
     *
     * <p>
     * <b>Thread-safety contract:</b> the {@link #driver} field is updated only inside this method, {@link #dropDeadSession()},
     * and {@link #closeDriver()}, all {@code synchronized}. Callers that need a stable {@link WebDriver} reference within a
     * single fetch must capture it from the field once after invoking {@code ensureDriver()} — they MUST NOT cache it across
     * method boundaries, or the caller may end up holding a stale handle if a session-fatal error fires concurrently and the
     * recovery path swaps the field. The Source today drains the queue with a single thread, so this constraint is academic; if
     * the queue consumer ever becomes multi-threaded the per-page fetch loop becomes a serialization point on this single driver
     * field. See {@link RemoteWebDriverConnectionProvider} for the connection-pool TODO.
     * </p>
     */
    private synchronized void ensureDriver() throws ConnectionException {
        if (driver == null) {
            driver = provider.createNewWebDriver();
        }
    }

    /**
     * Drops the driver reference without calling {@link WebDriver#quit()}. Used when an upstream error has already proven the
     * session is gone. Calling {@code quit()} on a worker-lost session blocks until the worker's HTTP response timeout (CloudHub
     * remote-browser: 120s today), so issuing the DELETE is pure latency tax — the session is already orphaned and the worker's
     * TTL reaper will reclaim it.
     */
    private synchronized void dropDeadSession() {
        if (driver == null) {
            return;
        }
        driver = null;
    }

    /**
     * Quits the driver gracefully. Invoked by the {@link RemoteWebDriverConnectionProvider} on disconnect.
     */
    public synchronized void closeDriver() {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (Exception e) {
            LOGGER.warn("Error while quitting remote WebDriver: {}", e.getMessage());
        } finally {
            driver = null;
        }
    }

    /**
     * True when the throwable indicates the WebDriver session is unrecoverable. Covers Selenium types and generic
     * {@link WebDriverException} chains whose message matches known session-loss modes.
     *
     * <p>
     * The string-match patterns below are biased toward CloudHub remote-browser worker failure modes —
     * {@code webdriver-unavailable}, {@code invalid session id}, and session-route timeouts. Other vendors surface session loss
     * with different messages: BrowserStack uses "session not found", Sauce Labs uses "session expired", a vanilla Selenium Grid
     * surfaces "no such session id". The provider holds an additional configurable {@code sessionFatalPatterns} list (Advanced
     * tab) that gets OR'd with the built-in matchers — vendors targeting non-CH grids can add their own patterns without editing
     * the connector.
     * </p>
     */
    boolean isSessionFatal(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof SessionNotCreatedException || cause instanceof NoSuchSessionException) {
                return true;
            }
            if (cause instanceof WebDriverException) {
                String msg = cause.getMessage();
                if (msg != null) {
                    String lower = msg.toLowerCase();
                    // Built-in CloudHub remote-browser failure modes.
                    if (lower.contains("webdriver-unavailable")
                            || lower.contains("invalid session id")
                            || (lower.contains("timeout exceeded") && lower.contains("/session/"))) {
                        return true;
                    }
                    // Vendor-specific patterns supplied via provider config.
                    for (String pattern : provider.getSessionFatalPatterns()) {
                        if (pattern != null && !pattern.isEmpty() && lower.contains(pattern.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
