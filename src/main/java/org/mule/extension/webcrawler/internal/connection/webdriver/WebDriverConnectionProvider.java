package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.extension.webcrawler.internal.helper.webdriver.CloudHubChromeConfigurer;
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
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

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

  public WebDriver createNewWebDriver() {

    ChromeOptions options = new ChromeOptions();

    if (CloudHubChromeConfigurer.isCloudHubDeployment()) {
      CloudHubChromeConfigurer.setup();
      options.setBinary(CloudHubChromeConfigurer.CHROME_LIB_WRAPPER_SCRIPT);
      // Additional arguments to reduce containerized Chrome memory usage
      options.addArguments("--blink-settings=imagesEnabled=false"); // Disable image rendering
      options.addArguments("--disable-software-rasterizer");
      options.addArguments("--disable-background-networking");
      options.addArguments("--remote-debugging-pipe");

      options.addArguments("--window-size=1920,1080");
      options.addArguments("--no-zygote");
      options.addArguments("--disable-renderer-backgrounding");
    }

    options.addArguments("--headless=new");
    options.addArguments("--disable-extensions");
    options.addArguments("--disable-gpu"); // Disable GPU acceleration
    options.addArguments("--no-sandbox"); // Recommended for headless mode in Docker or CI environments
    options.addArguments("--disable-dev-shm-usage"); // Recommended for limited resources
    options.addArguments("--allow-running-insecure-content"); // Allow HTTP content on HTTPS pages
    if(userAgent != null && !userAgent.isEmpty()) options.addArguments("--user-agent=" + userAgent);
    if(referrer != null && !referrer.isEmpty()) options.addArguments("--referer=" + referrer);

    if(Boolean.getBoolean("webdriver.chrome.verboseLogging")) {

      LOGGER.debug("Enabling verbose logging for ChromeDriver");

      // Custom OutputStream to capture ChromeDriver logs with filtering
      OutputStream chromeLogStream = new OutputStream() {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void write(int b) throws IOException {
          if (b == '\n') {
            // Print each complete line with a prefix
            LOGGER.debug("[CHROMEDRIVER] " + buffer.toString());
            buffer.setLength(0);
          } else {
            buffer.append((char) b);
          }
        }
      };

      PrintStream printStream = new PrintStream(chromeLogStream, true);

      // Create ChromeDriverService with log redirect
      ChromeDriverService service = new ChromeDriverService.Builder()
          .withVerbose(true)      // enable verbose logging
          .withSilent(false)      // ensure logs are generated
          .withLogOutput(printStream) // redirect logs to our custom stream
          .build();
      driver = new ChromeDriver(service, options);
    } else {

      LOGGER.debug("Verbose logging for ChromeDriver is disabled");
      driver = new ChromeDriver(options);
    }

    String actualUserAgent = (String) ((JavascriptExecutor) driver).executeScript("return navigator.userAgent;");
    LOGGER.info("User Agent: {}", actualUserAgent);

    return driver;
  }
}
