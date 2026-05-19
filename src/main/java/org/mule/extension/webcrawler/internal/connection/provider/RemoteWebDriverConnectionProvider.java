package org.mule.extension.webcrawler.internal.connection.provider;

import org.mule.extension.webcrawler.internal.connection.RemoteWebDriverConnection;
import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection provider for the Remote WebDriver fetch path. Spins up a Selenium {@link RemoteWebDriver} pointed at the configured
 * remote endpoint (Selenium Grid, standalone ChromeDriver, BrowserStack, CloudHub remote-browser, etc.).
 *
 * <p>
 * Embedded-Chrome support has been removed — see the new branch's pom and architecture docs. Browser must be reachable as a
 * Chromium-family W3C WebDriver endpoint; {@link ChromeOptions} is used unconditionally, so non-Chromium grids will fail at
 * session creation.
 * </p>
 */
@Alias("remote-webdriver-connection")
@DisplayName("Remote WebDriver")
public class RemoteWebDriverConnectionProvider implements CachedConnectionProvider<RemoteWebDriverConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverConnectionProvider.class);

    /**
     * Curated default block-list shipped to the worker via the {@code sfRemote:blockedUrls} capability when running against the
     * CloudHub remote-browser worker. Patterns use Chrome's URL-pattern syntax (wildcard {@code *}, scheme/host/path glob — not
     * regex), matched by chromedriver via {@code Network.setBlockedURLs}. Vanilla Selenium Grids and other vendors silently
     * ignore the unknown capability.
     *
     * <p>
     * <b>CloudHub remote-browser specific:</b> ad and tracker hosts dominate the rendered DOM weight on content sites; blocking
     * them at the network layer means {@code getPageSource} returns 1-2 MB instead of 10 MB+ for a typical ad-laden page, which
     * keeps the worker's Chrome under its memory budget.
     * </p>
     */
    static final List<String> DEFAULT_BLOCKED_URL_PATTERNS = Collections.unmodifiableList(Arrays.asList(
                                                                                                        "*://*.doubleclick.net/*",
                                                                                                        "*://*.googletagmanager.com/*",
                                                                                                        "*://*.googletagservices.com/*",
                                                                                                        "*://*.google-analytics.com/*",
                                                                                                        "*://*.googlesyndication.com/*",
                                                                                                        "*://*.googleadservices.com/*",
                                                                                                        "*://*.facebook.com/tr*",
                                                                                                        "*://connect.facebook.net/*",
                                                                                                        "*://*.hotjar.com/*",
                                                                                                        "*://*.taboola.com/*",
                                                                                                        "*://*.outbrain.com/*",
                                                                                                        "*://*.criteo.com/*",
                                                                                                        "*://*.adnxs.com/*",
                                                                                                        "*://*.adroll.com/*",
                                                                                                        "*://*.scorecardresearch.com/*",
                                                                                                        "*://*.amazon-adsystem.com/*"));

    static final String SF_REMOTE_BLOCKED_URLS_CAPABILITY = "sfRemote:blockedUrls";

    @Parameter
    @DisplayName("Remote WebDriver URL")
    @Summary("URL of the remote Selenium WebDriver endpoint (e.g. Selenium Grid, standalone ChromeDriver "
            + "or a CloudHub-hosted remote-browser app). Chromium-only (Chrome, Chromium, Edge).")
    @Placement(order = 1)
    @Example("https://remote-browser.example.com")
    private String remoteUrl;

    @Parameter
    @Alias("userAgent")
    @DisplayName("User agent")
    @Summary("Overrides the browser user-agent for all requests.")
    @Placement(order = 2)
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(UserAgentNameProvider.class)
    @Optional
    private String userAgent;

    @Parameter
    @Alias("referrer")
    @DisplayName("Referrer")
    @Summary("Sets the Referer header for navigations.")
    @Placement(order = 3)
    @Expression(ExpressionSupport.SUPPORTED)
    @Example("https://www.google.com")
    @Optional
    private String referrer;

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Blocked URL Patterns")
    @Summary("Chrome URL-pattern globs (e.g. '*://*.doubleclick.net/*') for requests the browser should drop at the network "
            + "layer. CloudHub remote-browser specific: sent to the grid via the sfRemote:blockedUrls capability; grids that don't "
            + "honor it ignore the capability. When omitted, a curated default list of ad/tracker hosts is used. Pass an empty list "
            + "to disable blocking entirely.")
    @Placement(tab = "Advanced")
    private List<String> blockedUrls;

    @Parameter
    @Optional(defaultValue = "true")
    @DisplayName("Use sfRemote Capabilities")
    @Summary("CloudHub remote-browser specific: send the sfRemote:blockedUrls and other sfRemote capabilities to the grid "
            + "during session creation. W3C says unknown capabilities should be ignored, but some strict grids reject unknown "
            + "caps with 'invalid argument'. Set to false when targeting Selenium Grid 4, BrowserStack, Sauce Labs, or any "
            + "non-CloudHub vendor.")
    @Placement(tab = "Advanced")
    private boolean useSfRemoteCapabilities;

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Additional Session-Fatal Patterns")
    @Summary("Additional case-insensitive substrings that, when found in a WebDriverException message, mark the session as "
            + "unrecoverable and trigger a fresh-driver rebuild on the next fetch. Built-in patterns cover CloudHub "
            + "remote-browser failure modes; add vendor-specific strings here (e.g. 'session not found' for BrowserStack, "
            + "'session expired' for Sauce Labs).")
    @Placement(tab = "Advanced")
    private List<String> sessionFatalPatterns;

    @Override
    public RemoteWebDriverConnection connect() throws ConnectionException {
        WebDriver driver = createNewWebDriver();
        return new RemoteWebDriverConnection(this, driver, userAgent, referrer);
    }

    @Override
    public void disconnect(RemoteWebDriverConnection connection) {
        if (connection != null) {
            connection.closeDriver();
        }
    }

    @Override
    public ConnectionValidationResult validate(RemoteWebDriverConnection connection) {
        return ConnectionValidationResult.success();
    }

    /**
     * Spins up a fresh {@link RemoteWebDriver}. Called both at initial connect time and from
     * {@link RemoteWebDriverConnection#fetchPage} after a session-fatal error has dropped the previous driver.
     */
    public WebDriver createNewWebDriver() throws ConnectionException {
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--allow-running-insecure-content");
            if (userAgent != null && !userAgent.isEmpty()) {
                options.addArguments("--user-agent=" + userAgent);
            }
            if (referrer != null && !referrer.isEmpty()) {
                options.addArguments("--referer=" + referrer);
            }
            // sfRemote:* capabilities are CloudHub-specific. Strict W3C grids reject unknown caps with
            // "invalid argument", so this is gated behind useSfRemoteCapabilities (default true for the
            // CloudHub workspace, set to false when targeting Grid 4 / BrowserStack / Sauce / etc).
            if (useSfRemoteCapabilities) {
                List<String> resolvedBlockedUrls = resolveBlockedUrls();
                if (!resolvedBlockedUrls.isEmpty()) {
                    options.setCapability(SF_REMOTE_BLOCKED_URLS_CAPABILITY, resolvedBlockedUrls);
                    LOGGER.info("Setting {} capability with {} pattern(s)",
                                SF_REMOTE_BLOCKED_URLS_CAPABILITY, resolvedBlockedUrls.size());
                }
            }
            WebDriver driver = buildDriver(new URL(remoteUrl), options);
            LOGGER.info("Connected to remote WebDriver at {}", remoteUrl);
            return driver;
        } catch (MalformedURLException e) {
            throw new ConnectionException("Invalid Remote WebDriver URL: " + remoteUrl, e);
        } catch (Exception e) {
            throw new ConnectionException("Failed to create remote WebDriver at " + remoteUrl + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns the effective block-list. Three cases: {@code null} → curated default; explicit empty list → user opted out, send
     * no capability; non-empty list → use as-is.
     */
    private List<String> resolveBlockedUrls() {
        if (blockedUrls == null) {
            return DEFAULT_BLOCKED_URL_PATTERNS;
        }
        return blockedUrls;
    }

    /**
     * Returns the user-supplied list of additional session-fatal substring patterns, or an empty list if none configured. Used by
     * {@link RemoteWebDriverConnection} to decide whether to drop the driver and rebuild on the next fetch.
     */
    public List<String> getSessionFatalPatterns() {
        return sessionFatalPatterns == null ? java.util.Collections.emptyList() : sessionFatalPatterns;
    }

    /**
     * Constructs the actual remote driver. Extracted as a package-private seam so tests can verify the {@link ChromeOptions}
     * passed in (notably the {@code sfRemote:blockedUrls} capability) without standing up a live worker.
     */
    WebDriver buildDriver(URL url, ChromeOptions options) {
        return new RemoteWebDriver(url, options);
    }

    void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    void setBlockedUrls(List<String> blockedUrls) {
        this.blockedUrls = blockedUrls;
    }
}
