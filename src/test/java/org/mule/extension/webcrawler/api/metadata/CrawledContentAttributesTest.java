package org.mule.extension.webcrawler.api.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

public class CrawledContentAttributesTest {

    @Test
    public void carriesAllFields() {
        CrawledContentAttributes attrs = new CrawledContentAttributes("https://example.com", 200, "text/html", 2);
        assertThat(attrs.getUrl(), is("https://example.com"));
        assertThat(attrs.getStatusCode(), is(200));
        assertThat(attrs.getContentType(), is("text/html"));
        assertThat(attrs.getDepth(), is(2));
    }

    @Test
    public void implementsSerializable() {
        CrawledContentAttributes attrs = new CrawledContentAttributes("u", 200, "text/html", 0);
        assertThat(attrs, instanceOf(Serializable.class));
    }

    @Test
    public void roundTripsThroughJavaSerialization() throws Exception {
        CrawledContentAttributes original = new CrawledContentAttributes("https://example.com", 200, "text/html", 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(original);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            CrawledContentAttributes read = (CrawledContentAttributes) ois.readObject();
            assertThat(read.getUrl(), is(original.getUrl()));
            assertThat(read.getStatusCode(), is(original.getStatusCode()));
            assertThat(read.getContentType(), is(original.getContentType()));
            assertThat(read.getDepth(), is(original.getDepth()));
        }
    }
}
