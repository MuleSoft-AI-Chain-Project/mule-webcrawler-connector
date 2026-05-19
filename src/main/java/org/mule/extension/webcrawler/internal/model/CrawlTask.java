package org.mule.extension.webcrawler.internal.model;

import java.io.Serializable;

/**
 * Internal task carried through the VM queue: a URL to crawl plus the depth at which it was discovered.
 *
 * <p>
 * {@link Serializable} so the runtime's {@code QueueManager} can persist the queue when backed by a clustered or persistent store
 * (e.g., CloudHub Persistent Queues).
 * </p>
 */
public class CrawlTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String url;
    private final int depth;

    public CrawlTask(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return url;
    }
}
