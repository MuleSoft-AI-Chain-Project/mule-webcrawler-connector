package org.mule.extension.webcrawler.testsupport;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.mule.extension.webcrawler.api.CustomAuthenticator;
import org.openqa.selenium.WebDriver;

/**
 * Test-only {@link CustomAuthenticator} implementation, registered via
 * {@code META-INF/services/org.mule.extension.webcrawler.api.CustomAuthenticator}
 * so {@link java.util.ServiceLoader} discovery is exercised during
 * {@code WebDriverConnection} construction.
 */
public class TestCustomAuthenticator implements CustomAuthenticator {

    public static final AtomicInteger configureCount = new AtomicInteger(0);
    public static final AtomicInteger needsRefreshCount = new AtomicInteger(0);
    public static final AtomicInteger canHandleUrlCount = new AtomicInteger(0);
    public static volatile boolean canHandleUrlReturn = true;
    public static volatile boolean needsRefreshReturn = true;
    public static volatile RuntimeException configureThrow = null;

    public static void reset() {
        configureCount.set(0);
        needsRefreshCount.set(0);
        canHandleUrlCount.set(0);
        canHandleUrlReturn = true;
        needsRefreshReturn = true;
        configureThrow = null;
    }

    @Override
    public String getId() {
        return "testCustomAuthenticator";
    }

    @Override
    public void configureAuthentication(WebDriver driver, Map<String, String> config) {
        configureCount.incrementAndGet();
        if (configureThrow != null) {
            throw configureThrow;
        }
    }

    @Override
    public boolean canHandleUrl(String url) {
        canHandleUrlCount.incrementAndGet();
        return canHandleUrlReturn;
    }

    @Override
    public boolean needsRefresh(WebDriver driver, Map<String, String> config) {
        needsRefreshCount.incrementAndGet();
        return needsRefreshReturn;
    }
}
