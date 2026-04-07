package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Remote WebDriver Connection Provider for connecting to external Selenium Grid, BrowserStack,
 * or standalone ChromeDriver instances.
 *
 * This provider eliminates the need for bundling Chrome browser with the connector,
 * making it suitable for certification and CloudHub deployments.
 *
 * IMPORTANT: Selenium client version must be compatible with the remote Grid version.
 * Current connector uses Selenium 4.35.0. For best results, use Selenium Grid 4.35.x
 * or a version within 2-3 minor versions.
 */
@Alias("remote-web-driver")
@DisplayName("Remote WebDriver")
public class RemoteWebDriverConnectionProvider implements CachedConnectionProvider<WebDriverConnection>, Startable, Stoppable, WebDriverProvider {

  private static Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverConnectionProvider.class);

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

  private WebDriver driver;

  @Override
  public WebDriverConnection connect() throws ConnectionException {
    return new WebDriverConnection(driver, userAgent, referrer, this);
  }

  @Override
  public void disconnect(WebDriverConnection webDriverConnection) {
    // Connection pooling - don't close driver on disconnect
  }

  @Override
  public ConnectionValidationResult validate(WebDriverConnection webDriverConnection) {
    try {
      // Test if driver is still responsive by checking session ID
      if (driver != null && driver instanceof RemoteWebDriver) {
        RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
        // Just check if session ID exists - don't execute any commands
        if (remoteDriver.getSessionId() != null) {
          LOGGER.debug("Session validation successful. Session ID: {}", remoteDriver.getSessionId());
          return ConnectionValidationResult.success();
        } else {
          return ConnectionValidationResult.failure("Session ID is null", new IllegalStateException("No active session"));
        }
      }
      return ConnectionValidationResult.failure("WebDriver not initialized", new IllegalStateException("Driver is null"));
    } catch (Exception e) {
      LOGGER.warn("Connection validation failed: {}", e.getMessage());
      return ConnectionValidationResult.failure("WebDriver validation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void start() throws MuleException {
    if (driver == null) {
      synchronized (RemoteWebDriverConnectionProvider.class) {
        if (driver == null) {
          driver = createRemoteWebDriver();
        }
      }
    }
  }

  @Override
  public void stop() throws MuleException {
    if (driver != null) {
      LOGGER.debug("Quitting Remote Selenium WebDriver");
      try {
        driver.quit();
      } catch (Exception e) {
        LOGGER.warn("Error while quitting WebDriver: {}", e.getMessage());
      } finally {
        driver = null;
      }
    }
  }

  /**
   * Creates a new RemoteWebDriver instance connected to the specified remote URL.
   *
   * @return RemoteWebDriver instance
   */
  public WebDriver createNewWebDriver() {
    return createRemoteWebDriver();
  }

  private WebDriver createRemoteWebDriver() {
    try {
      LOGGER.info("Connecting to remote WebDriver at: {}", remoteUrl);

      ChromeOptions options = new ChromeOptions();

      // Standard headless options
      options.addArguments("--headless=new");
      options.addArguments("--disable-extensions");
      options.addArguments("--disable-gpu");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
      options.addArguments("--allow-running-insecure-content");

      // User agent and referrer
      if (userAgent != null && !userAgent.isEmpty()) {
        options.addArguments("--user-agent=" + userAgent);
      }
      if (referrer != null && !referrer.isEmpty()) {
        options.addArguments("--referer=" + referrer);
      }

      // Create RemoteWebDriver
      URL remoteAddress = new URL(remoteUrl);
      driver = new RemoteWebDriver(remoteAddress, options);

      // Log session details without executing commands that might affect session state
      LOGGER.info("Connected to remote WebDriver at {}. Session ID: {}", remoteUrl,
                  ((RemoteWebDriver) driver).getSessionId());

      return driver;

    } catch (Exception e) {
      LOGGER.error("Failed to connect to remote WebDriver at {}: {}", remoteUrl, e.getMessage(), e);
      throw new RuntimeException("Failed to connect to remote WebDriver: " + e.getMessage(), e);
    }
  }
}
