package org.mule.extension.webcrawler.internal.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

public class CrawlTaskTest {

    @Test
    public void carriesUrlAndDepth() {
        CrawlTask task = new CrawlTask("https://example.com", 3);
        assertThat(task.getUrl(), is("https://example.com"));
        assertThat(task.getDepth(), is(3));
    }

    @Test
    public void toStringReturnsUrl() {
        CrawlTask task = new CrawlTask("https://example.com/page", 1);
        assertThat(task.toString(), is("https://example.com/page"));
    }

    @Test
    public void implementsSerializable() {
        CrawlTask task = new CrawlTask("https://example.com", 0);
        assertThat(task, instanceOf(Serializable.class));
    }

    @Test
    public void roundTripsThroughJavaSerialization() throws Exception {
        CrawlTask original = new CrawlTask("https://example.com/a", 2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(original);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            CrawlTask read = (CrawlTask) ois.readObject();
            assertThat(read.getUrl(), equalTo(original.getUrl()));
            assertThat(read.getDepth(), equalTo(original.getDepth()));
        }
    }
}
