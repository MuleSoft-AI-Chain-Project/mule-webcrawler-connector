package org.mule.extension.webcrawler.internal.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class AbstractCrawlerSourceTest {

    // -----------------------------------------------------------------
    // isAcceptedMimeType — pure function, exhaustive coverage
    // -----------------------------------------------------------------

    @Test
    public void acceptsAllWhenListIsNull() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("text/html", null), is(true));
    }

    @Test
    public void acceptsAllWhenListIsEmpty() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("text/html", Collections.emptyList()), is(true));
    }

    @Test
    public void passesThroughWhenContentTypeNullAndListNonEmpty() {
        // Remote WebDriver fetches surface a null Content-Type. The filter passes through in that
        // case so a filter set up for the HTTP path doesn't silently drop every remote-fetched page.
        assertThat(AbstractCrawlerSource.isAcceptedMimeType(null, Arrays.asList("text/html")), is(true));
    }

    @Test
    public void passesThroughWhenContentTypeEmptyAndListNonEmpty() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("", Arrays.asList("text/html")), is(true));
    }

    @Test
    public void exactMatchAccepted() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("text/html",
                                                            Arrays.asList("text/html", "application/xml")),
                   is(true));
    }

    @Test
    public void caseInsensitiveMatch() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("Text/HTML",
                                                            Arrays.asList("text/html")),
                   is(true));
    }

    @Test
    public void stripsCharsetParameter() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("text/html; charset=UTF-8",
                                                            Arrays.asList("text/html")),
                   is(true));
    }

    @Test
    public void wildcardNotSupportedExactMatchOnly() {
        // Per design decision: exact-match only. Wildcard "*/*" is not interpreted as a wildcard.
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("application/pdf",
                                                            Arrays.asList("*/*")),
                   is(false));
    }

    @Test
    public void unmatchedMimeRejected() {
        assertThat(AbstractCrawlerSource.isAcceptedMimeType("application/pdf",
                                                            Arrays.asList("text/html", "application/xml")),
                   is(false));
    }

    // -----------------------------------------------------------------
    // isTransientNetworkError — drives the retry-vs-permanent-mark
    // decision in processUrl. A regression that misclassifies a real
    // permanent failure (e.g. an ObjectStoreException) as "transient"
    // would loop forever; the inverse would flap-mark transient blips
    // as visited and never retry them.
    // -----------------------------------------------------------------

    @Test
    public void ioException_isTransient() {
        assertThat(AbstractCrawlerSource.isTransientNetworkError(new IOException("read timed out")), is(true));
    }

    @Test
    public void socketTimeout_isTransient() {
        assertThat(AbstractCrawlerSource.isTransientNetworkError(new SocketTimeoutException("connect")), is(true));
    }

    @Test
    public void unknownHost_isTransient() {
        // DNS hiccups are transient — typical real-world cause is a CNAME flap.
        assertThat(AbstractCrawlerSource.isTransientNetworkError(new UnknownHostException("flaky.example.com")),
                   is(true));
    }

    @Test
    public void connectException_isTransient() {
        assertThat(AbstractCrawlerSource.isTransientNetworkError(new ConnectException("refused")), is(true));
    }

    @Test
    public void wrappedNetworkException_isTransient() {
        // Connectors typically wrap the underlying network failure in a RuntimeException
        // (see WebCrawlerConnection.fetchPage). The detector must walk the cause chain.
        Throwable wrapped = new RuntimeException("fetch failed",
                                                 new RuntimeException("io",
                                                                      new SocketTimeoutException("hit")));
        assertThat(AbstractCrawlerSource.isTransientNetworkError(wrapped), is(true));
    }

    @Test
    public void illegalArgumentException_isNotTransient() {
        // Malformed-URL-style failures are permanent — retrying won't help.
        assertThat(AbstractCrawlerSource.isTransientNetworkError(new IllegalArgumentException("bad url")),
                   is(false));
    }

    @Test
    public void runtimeException_isNotTransient() {
        assertThat(AbstractCrawlerSource.isTransientNetworkError(new RuntimeException("oops")), is(false));
    }

    @Test
    public void nullCause_terminatesWalk() {
        // The cause-chain walk must terminate cleanly on null cause.
        Throwable t = new RuntimeException("no cause");
        assertThat(AbstractCrawlerSource.isTransientNetworkError(t), is(false));
    }
}
