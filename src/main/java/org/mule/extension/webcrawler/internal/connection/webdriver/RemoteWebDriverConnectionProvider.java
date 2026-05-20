package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
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

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection provider for the Remote WebDriver fetch path. Spins up a Selenium {@link RemoteWebDriver} pointed at the configured
 * remote endpoint (Selenium Grid 4, standalone ChromeDriver, BrowserStack/Sauce Labs, or a CloudHub-hosted remote-browser app).
 *
 * <p>
 * Browser must be reachable as a Chromium-family W3C WebDriver endpoint; {@link ChromeOptions} is used unconditionally, so
 * non-Chromium grids will fail at session creation.
 * </p>
 */
@Alias("remote-webdriver-connection")
@DisplayName("Remote WebDriver")
public class RemoteWebDriverConnectionProvider implements CachedConnectionProvider<RemoteWebDriverConnection> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverConnectionProvider.class);

  @Parameter
  @DisplayName("Remote WebDriver URL")
  @Summary("URL of the remote Selenium WebDriver endpoint (e.g. Selenium Grid, standalone ChromeDriver, BrowserStack, "
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
   * {@link RemoteWebDriverConnection#withDriver} after a session-fatal error has dropped the previous driver.
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
   * Constructs the actual remote driver. Extracted as a package-private seam so a future test can substitute it without standing
   * up a live worker.
   */
  WebDriver buildDriver(URL url, ChromeOptions options) {
    return new RemoteWebDriver(url, options);
  }
}
