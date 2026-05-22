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
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

@Alias("web-driver")
@DisplayName("WebDriver")
public class WebDriverConnectionProvider implements CachedConnectionProvider<WebDriverConnection>, Startable, Stoppable {

  private static Logger LOGGER = LoggerFactory.getLogger(WebDriverConnectionProvider.class);

  @Parameter
  @Alias("userAgent")
  @DisplayName("User agent")
  @Summary("The request user-agent header.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(UserAgentNameProvider.class)
  @Optional
  private String userAgent;

  @Parameter
  @Alias("referrer")
  @DisplayName("Referrer")
  @Summary("The request referrer (aka \"referer\") header..")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("https://www.google.com")
  @Optional
  private String referrer;

  private WebDriver driver;

  @Override
  public WebDriverConnection connect() throws ConnectionException {
    return new WebDriverConnection(driver,userAgent, referrer, this);
  }

  @Override
  public void disconnect(WebDriverConnection webDriverConnection) {

  }

  @Override
  public ConnectionValidationResult validate(WebDriverConnection webDriverConnection) {
    return ConnectionValidationResult.success();
  }

  @Override
  public void start() throws MuleException {

    if (driver == null) {
      synchronized (WebDriverConnectionProvider.class) {
        if (driver == null) {
          driver = createNewWebDriver();
        }
      }
    }
  }

  @Override
  public void stop() throws MuleException {

    if (driver != null) {
      LOGGER.debug("Quitting Selenium WebDriver");
      driver.quit();
      driver = null;
    }
  }

  // Default chromedriver endpoint exposed by remote-browser-server-plugin on the same worker.
  // Override with -Dremote.browser.url=http://host:port for non-Mule-server tests.
  private static final String DEFAULT_REMOTE_URL = "http://localhost:38301";

  public WebDriver createNewWebDriver() {

    /*
     * In-process ChromeDriver path is intentionally removed on this branch.
     * All driver construction MUST go through the remote chromedriver hosted
     * by remote-browser-server-plugin. If the plugin isn't running on the
     * worker, the RemoteWebDriver constructor below will fail to connect —
     * that's the desired behaviour, it makes a misconfiguration loud.
     *
     * Anything that imports org.openqa.selenium.chrome.ChromeDriver in this
     * branch is wrong; only ChromeOptions remains because it's serialized
     * into the W3C newSession capability payload and not a local driver.
     */

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    options.addArguments("--disable-extensions");
    options.addArguments("--disable-gpu");
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--allow-running-insecure-content");
    options.addArguments("--blink-settings=imagesEnabled=false");
    options.addArguments("--disable-software-rasterizer");
    options.addArguments("--disable-background-networking");
    options.addArguments("--window-size=1920,1080");
    options.addArguments("--ignore-certificate-errors");
    options.addArguments("--disable-renderer-backgrounding");
    if (userAgent != null && !userAgent.isEmpty()) options.addArguments("--user-agent=" + userAgent);
    if (referrer != null && !referrer.isEmpty()) options.addArguments("--referer=" + referrer);

    String remoteUrl = System.getProperty("remote.browser.url", DEFAULT_REMOTE_URL);
    URL endpoint;
    try {
      endpoint = new URL(remoteUrl);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Invalid remote.browser.url: " + remoteUrl, e);
    }
    LOGGER.info("[REMOTE-DRIVER] Connecting to remote chromedriver at {}", endpoint);
    RemoteWebDriver remote = new RemoteWebDriver(endpoint, options);
    driver = remote;

    // Log the actual session id and the executor's addressable URL — this is
    // unforgeable proof we're talking to the remote endpoint, not an in-process driver.
    CommandExecutor executor = remote.getCommandExecutor();
    String executorAddr = (executor instanceof HttpCommandExecutor)
        ? ((HttpCommandExecutor) executor).getAddressOfRemoteServer().toString()
        : executor.getClass().getName();
    LOGGER.info("[REMOTE-DRIVER] Session id={} executor={}", remote.getSessionId(), executorAddr);

    String actualUserAgent = (String) ((JavascriptExecutor) driver).executeScript("return navigator.userAgent;");
    LOGGER.info("[REMOTE-DRIVER] User Agent: {}", actualUserAgent);

    return driver;
  }
}
