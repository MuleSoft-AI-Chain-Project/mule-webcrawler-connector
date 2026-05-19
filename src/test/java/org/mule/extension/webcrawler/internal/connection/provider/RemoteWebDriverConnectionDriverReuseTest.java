package org.mule.extension.webcrawler.internal.connection.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.webcrawler.internal.connection.RemoteWebDriverConnection;
import org.mule.extension.webcrawler.internal.model.WebCrawlerResponse;

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Locks in the cached-driver contract: a single {@link RemoteWebDriverConnection} reuses the same {@link WebDriver} across N
 * consecutive {@code fetchPage} calls. The provider's {@code buildDriver(URL, ChromeOptions)} seam — package-private specifically
 * for this test — is wired to count invocations.
 *
 * <p>
 * If a future patch quietly regresses to "rebuild a session per fetch" (e.g. a misguided refresh-the-driver tweak), this test
 * fails immediately. The driver-reuse fix is correct today, and once locked in by this regression test it tends to stay working.
 * </p>
 */
public class RemoteWebDriverConnectionDriverReuseTest {

    @Test
    public void buildDriverInvokedOnceAcrossManyFetches() throws Exception {
        AtomicInteger buildDriverInvocations = new AtomicInteger();

        // Stub out the live Selenium client by overriding the package-private buildDriver seam.
        // The mocked WebDriver also implements JavascriptExecutor so the document.readyState
        // poll inside fetchPage can be satisfied without a real browser.
        WebDriver mockDriver = mock(WebDriver.class,
                                    org.mockito.Mockito.withSettings().extraInterfaces(JavascriptExecutor.class));
        when(((JavascriptExecutor) mockDriver).executeScript("return document.readyState")).thenReturn("complete");
        when(mockDriver.getPageSource()).thenReturn("<html><body>ok</body></html>");

        RemoteWebDriverConnectionProvider provider = new RemoteWebDriverConnectionProvider() {

            @Override
            WebDriver buildDriver(URL url, ChromeOptions options) {
                buildDriverInvocations.incrementAndGet();
                return mockDriver;
            }
        };
        provider.setRemoteUrl("http://localhost:4444/wd/hub");

        RemoteWebDriverConnection connection = provider.connect();
        assertThat(connection, notNullValue());
        // connect() builds the first driver — counter is 1 going into the loop.
        assertThat(buildDriverInvocations.get(), is(1));

        for (int i = 0; i < 10; i++) {
            WebCrawlerResponse response = connection.fetchPage("https://example.com/page-" + i, null, null);
            assertThat(response, notNullValue());
            // The status-code probe (performance.getEntriesByType('navigation')[0].responseStatus)
            // isn't stubbed, so the mock JS executor returns null and the connection collapses to
            // the -1 sentinel. The test only cares about driver reuse — not the status number.
            assertThat(response.getStatusCode(), is(-1));
        }

        // The cached driver is reused across all 10 fetches. If a regression makes us rebuild
        // per call, this would be 11 instead of 1.
        assertThat(buildDriverInvocations.get(), is(1));
    }
}
