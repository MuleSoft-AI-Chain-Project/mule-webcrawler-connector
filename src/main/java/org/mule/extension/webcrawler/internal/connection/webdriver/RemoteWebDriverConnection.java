package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteWebDriverConnection implements WebCrawlerConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWebDriverConnection.class);

  private final RemoteWebDriverConnectionProvider provider;
  private final String userAgent;
  private final String referrer;

  // volatile is required because two synchronized methods (restartDriverIfDropped and dropDeadSession)
  // write to driver, but withDriver reads it without holding the lock at the read site.
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

  /**
   * Runs {@code action} against the cached {@link WebDriver}, centralising session-fatal recovery so callers do not
   * have to handle it. On a session-fatal error the driver reference is dropped without {@link WebDriver#quit()}, so
   * the next call rebuilds via the provider.
   */
  public <T> T withDriver(WebDriverExecutable<T> action) throws ConnectionException {
    restartDriverIfDropped();
    try {
      return action.execute(driver);
    } catch (Exception e) {
      if (isSessionFatal(e)) {
        LOGGER.warn("WebDriver session appears dead ({}: {}). Dropping driver without quit() so the next call rebuilds.",
            e.getClass().getSimpleName(), e.getMessage());
        dropDeadSession();
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Re-creates the driver if a previous call dropped it after a session-fatal error.
   */
  private synchronized void restartDriverIfDropped() throws ConnectionException {
    if (driver == null) {
      driver = provider.createNewWebDriver();
    }
  }

  // Skip quit() — calling DELETE on a session the worker has lost stalls until the worker's HTTP responseTimeout
  // fires (~120s on CloudHub remote-browser today). The worker's TTL reaper cleans up the orphan instead.
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

  // True when the throwable indicates the WebDriver session is unrecoverable.
  // Covers Selenium's typed session exceptions and WebDriverException chains whose message matches
  // known session-loss modes (CloudHub remote-browser sentinel, server-rejected session id, session-route timeouts).
  private boolean isSessionFatal(Throwable t) {
    for (Throwable cause = t; cause != null; cause = cause.getCause()) {
      if (cause instanceof SessionNotCreatedException || cause instanceof NoSuchSessionException) {
        return true;
      }
      if (cause instanceof WebDriverException) {
        String msg = cause.getMessage();
        if (msg != null) {
          String lower = msg.toLowerCase();
          if (lower.contains("webdriver-unavailable")
              || lower.contains("invalid session id")
              || (lower.contains("timeout exceeded") && lower.contains("/session/"))) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
