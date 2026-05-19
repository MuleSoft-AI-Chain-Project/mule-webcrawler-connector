package org.mule.extension.webcrawler.internal.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class WebCrawlerResponseTest {

    @Test
    public void carriesAllThreeFields() {
        InputStream body = new ByteArrayInputStream(new byte[] {1, 2, 3});
        WebCrawlerResponse response = new WebCrawlerResponse(200, "text/html", body);
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("text/html"));
        assertThat(response.getBody(), is(body));
    }

    @Test
    public void allowsNullContentType() {
        WebCrawlerResponse response = new WebCrawlerResponse(204, null, null);
        assertThat(response.getStatusCode(), is(204));
        assertThat(response.getContentType(), is(nullValue()));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    public void allowsErrorStatusCodes() {
        WebCrawlerResponse response = new WebCrawlerResponse(500, "text/plain", null);
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentType(), is(notNullValue()));
    }
}
