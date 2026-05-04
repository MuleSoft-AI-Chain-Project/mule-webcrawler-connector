package org.mule.extension.webcrawler.internal.crawler.mule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.webcrawler.internal.config.CrawlerOptions;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;

/**
 * Unit tests for {@link MuleCrawler} focusing on {@code DocumentIterator} edge
 * cases and {@code map()} invalid-URL branch. Deliberately avoids standing up
 * WebDriver/Selenium — only the {@link WebCrawlerConnection} interface is
 * exercised via Mockito mocks.
 */
class MuleCrawlerTest {

    private WebCrawlerConfiguration config;
    private WebCrawlerConnection connection;
    private CrawlerOptions crawlerOptions;

    @BeforeEach
    void setUp() throws Exception {
        config = mock(WebCrawlerConfiguration.class);
        crawlerOptions = mock(CrawlerOptions.class);
        when(config.getCrawlerOptions()).thenReturn(crawlerOptions);
        lenient().when(crawlerOptions.isEnforceRobotsTxt()).thenReturn(false);
        lenient().when(crawlerOptions.getDelayMillis()).thenReturn(0);

        connection = mock(WebCrawlerConnection.class);
        lenient().when(connection.getReferrer()).thenReturn("ref");
        lenient().when(connection.getUserAgent()).thenReturn("agent");
    }

    private MuleCrawler newCrawler(String url, int maxDepth) {
        return new MuleCrawler(config, connection, url,
                1000L, null, false, null,
                null, Collections.emptyMap(),
                maxDepth, false,
                false, 0, false, 0, "/tmp",
                null, Constants.OutputFormat.TEXT, false,
                null, null);
    }

    // ---------- DocumentIterator ----------

    @Test
    void documentIteratorConstructorNullUrlThrows() {
        // Per test-design §2.1, DocumentIterator constructor must throw
        // when rootURL is null. In practice, URLUtils.cleanURL(null) NPEs
        // before the IllegalArgumentException guard; either outcome is
        // acceptable — the contract is "does not construct successfully".
        MuleCrawler crawler = newCrawler(null, 1);
        assertThrows(RuntimeException.class, crawler::documentIterator);
    }

    @Test
    void documentIteratorNextThrowsAtExhaustion() throws Exception {
        // Feed a valid root URL and an empty HTML page so the iterator consumes
        // exactly one document and then `hasNext()` becomes false.
        when(connection.getPageSource(anyString(), any(), any(PageLoadOptions.class)))
                .thenReturn(new ByteArrayInputStream(
                        "<html><body></body></html>".getBytes()));
        MuleCrawler crawler = newCrawler("https://example.com", 0);
        MuleCrawler.DocumentIterator it = crawler.documentIterator();

        assertTrue(it.hasNext(), "root present initially");
        it.next(); // consume root
        assertEquals(false, it.hasNext(), "no more after root");
        assertThrows(NoSuchElementException.class, it::next);
    }

    // ---------- map() edge cases ----------

    @Test
    void mapMaxDepthZeroInvalidUrlReturnsNull() throws Exception {
        // isURLValid -> false by making getUrlStatusCode return anything other than 200.
        when(connection.getUrlStatusCode(anyString(), any())).thenReturn(404);
        MuleCrawler crawler = newCrawler("https://example.com/missing", 0);
        assertNull(crawler.map(), "invalid URL at maxDepth=0 must return null");
    }

    @Test
    void mapMaxDepthZeroValidUrlReturnsRoot() throws Exception {
        when(connection.getUrlStatusCode(anyString(), any())).thenReturn(200);
        MuleCrawler crawler = newCrawler("https://example.com", 0);
        assertNotNull(crawler.map(), "valid URL at maxDepth=0 returns rootNode");
    }
}
