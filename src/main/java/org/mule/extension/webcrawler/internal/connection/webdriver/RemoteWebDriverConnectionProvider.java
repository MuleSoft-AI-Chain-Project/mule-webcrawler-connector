package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Remote WebDriver connection provider for connecting to an external
 * ChromeDriver / Selenium Grid / BrowserStack / CloudHub-hosted
 * {@code remote-browser-*} app.
 *
 * <p><b>Pooled by design.</b> This provider implements
 * {@link PoolingConnectionProvider} so each concurrent crawl gets its own
 * dedicated {@link RemoteWebDriver} instance — and therefore its own Chrome
 * session on the remote host — rather than sharing one driver across every
 * flow that uses the config. Prior behaviour (one shared driver per app
 * lifetime) forced all concurrent crawls to serialise through a single
 * browser session; under load that caused sessions to trample each other and
 * silently truncate crawls. Pool size is tuned via the connector config's
 * standard {@code pooling-profile} element.
 *
 * <p><b>Selenium version compatibility.</b> Connector ships Selenium 4.35.0.
 * Use a Grid/driver within 2–3 minor versions for best results.
 */
@Alias("remote-web-driver")
@DisplayName("Remote WebDriver")
public class RemoteWebDriverConnectionProvider
        implements PoolingConnectionProvider<WebDriverConnection>, WebDriverProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverConnectionProvider.class);

  @Parameter
  @Alias("remoteUrl")
  @DisplayName("Remote WebDriver URL")
  @Summary("URL of the remote WebDriver server (e.g., Selenium Grid, BrowserStack, or standalone ChromeDriver)")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("http://localhost:9515")
  private String remoteUrl;

  @Parameter
  @Alias("userAgent")
  @DisplayName("User agent")
  @Summary("The request user-agent header.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(UserAgentNameProvider.class)
  @Optional
  private String userAgent;

  @Parameter
  @Alias("referrer")
  @DisplayName("Referrer")
  @Summary("The request referrer (aka \"referer\") header.")
  @Placement(order = 3)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("https://www.google.com")
  @Optional
  private String referrer;

  /**
   * Creates a dedicated RemoteWebDriver for this pool lease. Each call
   * establishes a brand-new Chrome session on the remote server — that's the
   * whole point of making this provider pool-backed. Mule's pool machinery
   * checks out one connection per concurrent flow, so parallel crawls no
   * longer share a driver.
   */
  @Override
  public WebDriverConnection connect() throws ConnectionException {
    try {
      WebDriver driver = createRemoteWebDriver();
      // pooled=true flips WebDriverConnection.restartDriver() to a no-op;
      // the pool, not the connection, owns the lifecycle now.
      return new WebDriverConnection(driver, userAgent, referrer, this, true);
    } catch (Exception e) {
      throw new ConnectionException("Failed to create Remote WebDriver connection: " + e.getMessage(), e);
    }
  }

  /**
   * Releases the browser session on the remote host. Called by the pool
   * when a connection is evicted (idle timeout, invalidation, pool stop).
   * Must be best-effort: a remote that's already gone away shouldn't fail
   * the pool's bookkeeping.
   */
  @Override
  public void disconnect(WebDriverConnection webDriverConnection) {
    if (webDriverConnection == null) {
      return;
    }
    try {
      webDriverConnection.quitDriver();
    } catch (Exception e) {
      LOGGER.warn("Error while disconnecting pooled Remote WebDriver: {}", e.getMessage());
    }
  }

  /**
   * Health-check used by the pool before handing a connection to a flow.
   * The previous implementation only checked {@code getSessionId() != null},
   * which reads a cached local field and does NOT contact the remote — so a
   * dead replica was reported healthy and the next call would blow up.
   *
   * <p>Here we make a cheap server round-trip ({@code getCurrentUrl()})
   * wrapped in try/catch so a truly dead session is ejected from the pool
   * and the next checkout creates a fresh one.
   */
  @Override
  public ConnectionValidationResult validate(WebDriverConnection webDriverConnection) {
    if (webDriverConnection == null) {
      return ConnectionValidationResult.failure("Connection is null",
          new IllegalStateException("null connection"));
    }
    WebDriver driver = webDriverConnection.getDriver();
    if (driver == null) {
      return ConnectionValidationResult.failure("WebDriver is null",
          new IllegalStateException("null driver"));
    }
    try {
      if (driver instanceof RemoteWebDriver
          && ((RemoteWebDriver) driver).getSessionId() == null) {
        return ConnectionValidationResult.failure("No active session",
            new IllegalStateException("sessionId is null"));
      }
      // Cheap remote round-trip; throws if the session is dead on the server.
      driver.getCurrentUrl();
      return ConnectionValidationResult.success();
    } catch (Exception e) {
      LOGGER.warn("Remote WebDriver validation failed, will be evicted from pool: {}", e.getMessage());
      return ConnectionValidationResult.failure("Remote WebDriver validation failed: " + e.getMessage(), e);
    }
  }

  /**
   * Kept so the {@link WebDriverProvider} contract still compiles; with
   * pooling enabled this is only used by legacy code paths that expect to
   * self-manage a driver. Each invocation creates an independent Chrome
   * session, same as {@link #connect()}.
   */
  @Override
  public WebDriver createNewWebDriver() {
    return createRemoteWebDriver();
  }

  private WebDriver createRemoteWebDriver() {
    try {
      LOGGER.info("Connecting to remote WebDriver at: {}", remoteUrl);

      ChromeOptions options = new ChromeOptions();
      // Standard headless options for server-side Chrome
      options.addArguments("--headless=new");
      options.addArguments("--disable-extensions");
      options.addArguments("--disable-gpu");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
      options.addArguments("--allow-running-insecure-content");

      if (userAgent != null && !userAgent.isEmpty()) {
        options.addArguments("--user-agent=" + userAgent);
      }
      if (referrer != null && !referrer.isEmpty()) {
        options.addArguments("--referer=" + referrer);
      }

      RemoteWebDriver driver = new RemoteWebDriver(new URL(remoteUrl), options);

      LOGGER.info("Connected to remote WebDriver at {}. Session ID: {}",
          remoteUrl, driver.getSessionId());

      return driver;
    } catch (Exception e) {
      LOGGER.error("Failed to connect to remote WebDriver at {}: {}", remoteUrl, e.getMessage(), e);
      throw new RuntimeException("Failed to connect to remote WebDriver: " + e.getMessage(), e);
    }
  }
}
