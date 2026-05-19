package org.mule.extension.webcrawler.internal.source;

import org.mule.extension.webcrawler.api.HashRouteMode;
import org.mule.extension.webcrawler.api.RegexUrlsFilterLogic;
import org.mule.extension.webcrawler.api.metadata.CrawledContentAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.extension.webcrawler.internal.model.CrawlTask;
import org.mule.extension.webcrawler.internal.model.WebCrawlerResponse;
import org.mule.extension.webcrawler.internal.service.LinkExtractor;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared engine behind any queue-driven, ObjectStore-backed crawler {@link Source}. Concrete subclasses provide a
 * {@link #consume} implementation that surfaces the per-page event in whatever shape the user-facing operation requires (e.g.,
 * the default {@link CrawlerSource} hands off to the SDK's {@link SourceCallback}).
 *
 * <p>
 * Threading model — single-threaded daemon worker per Source instance, blocks on {@link Queue#poll(long)} indefinitely. When the
 * queue drains the worker idles, ready to resume if anything ever re-enqueues. See the refactor decision doc for the trade-offs.
 * </p>
 *
 * <p>
 * <b>Filter pipeline.</b> Two distinct passes — note that these run at different points in the queue's lifecycle, not as a single
 * ordered chain:
 * </p>
 *
 * <p>
 * <i>Pass A — per fetched page</i>, applied in {@link #processUrl} after the URL is dequeued:
 * <ol>
 * <li>robots.txt check (when {@code enforceRobotsTxt=true}) — applied <em>before</em> the fetch so a disallowed URL costs
 * nothing.</li>
 * <li>fetch the page via the connection.</li>
 * <li>mark the URL as visited in the ObjectStore.</li>
 * <li>link extraction (only if the response MIME is parseable, i.e. HTML/XML).</li>
 * <li>{@code acceptedMimeTypes} emit gate — pages whose content-type isn't in the list are NOT emitted, but their links have
 * already been extracted at step 4. This is intentional: a user crawling for PDFs needs the connector to walk through HTML index
 * pages to find them.</li>
 * </ol>
 * </p>
 *
 * <p>
 * <i>Pass B — per discovered link</i>, applied in {@link #applyFilters} before each link is enqueued:
 * <ol>
 * <li>same-host check (already done by {@link LinkExtractor}'s internal/external classification).</li>
 * <li>path-prefix check when {@code restrictToPath=true}.</li>
 * <li>regex include/exclude per the {@code regexUrls} list and {@code regexUrlsFilterLogic}.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Robots.txt and {@code acceptedMimeTypes} are NOT checked at enqueue time — only when the link is later popped by the worker.
 * </p>
 */
public abstract class AbstractCrawlerSource extends Source<InputStream, CrawledContentAttributes> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrawlerSource.class);

    /** Poll timeout for {@link Queue#poll(long)} — short enough to react to {@code stopped} flips, long enough to avoid spin. */
    private static final long POLL_TIMEOUT_MS = 1000L;

    @Config
    protected WebCrawlerConfiguration configuration;

    @Connection
    protected ConnectionProvider<WebCrawlerConnection> connectionProvider;

    protected volatile boolean stopped;
    protected Thread consumerThread;
    protected SourceCallback<InputStream, CrawledContentAttributes> sourceCallback;
    protected final AtomicLong fetchedCount = new AtomicLong();
    protected final AtomicLong duplicateCount = new AtomicLong();

    /** Queue cached at consumer-loop start; shared between {@code consumeLoop} and {@link #enqueueLinks}. */
    private volatile Queue queue;

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    /**
     * The connection is acquired once at {@link #onStart} and reused for every page in the queue. The runtime
     * ({@link CachedConnectionProvider}) decides when to release it — we MUST NOT call {@link ConnectionProvider#disconnect} per
     * page or the Remote WebDriver session is torn down and rebuilt on every URL (5–15s of session bring-up burned per page).
     */
    protected WebCrawlerConnection connection;

    @Override
    public void onStart(SourceCallback<InputStream, CrawledContentAttributes> sourceCallback) throws MuleException {
        // Fail fast at Source startup if the configuration didn't seed a URL — the Source has nothing to do
        // without one, and silently idling on an empty queue is worse than a clear failure. The config's
        // `url` parameter is @Optional precisely because page-* / get-sitemap operations don't need it; the
        // Source does, so check here.
        String seed = configuration.getUrl();
        if (seed == null || seed.isEmpty()) {
            throw new org.mule.runtime.api.exception.DefaultMuleException(
                                                                          "crawl-website-source requires the configuration's `url` parameter to be set with a seed URL. "
                                                                                  + "The configuration `"
                                                                                  + configuration.getQueueName()
                                                                                  + "` has no url; either set it or remove the Source from the flow.");
        }
        this.sourceCallback = sourceCallback;
        try {
            this.connection = connectionProvider.connect();
        } catch (Exception e) {
            throw new org.mule.runtime.api.exception.DefaultMuleException("Failed to connect at Source startup", e);
        }
        stopped = false;
        consumerThread = new Thread(this::consumeLoop, threadName());
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @Override
    public void onStop() {
        stopped = true;
        LOGGER.info("Crawler Source stopping. Final totals: fetched={}, duplicatesSkipped={}",
                    fetchedCount.get(), duplicateCount.get());
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        // The runtime cache will dispose the connection — leave the field alone so a late-arriving
        // worker iteration can still see it during shutdown.
    }

    /** Subclasses override to label the daemon thread for diagnostics. */
    protected String threadName() {
        return "webcrawler-queue-consumer";
    }

    // ---------------------------------------------------------------------
    // Per-Source customization points
    // ---------------------------------------------------------------------

    /** Returns the visited-URL ObjectStore. Subclasses inject this as a {@code @Parameter}. */
    protected abstract ObjectStore<Serializable> getObjectStore();

    /** Returns the per-page filter parameters declared on the Source. */
    protected abstract CrawlerTargetPagesParameters getTargetPages();

    /**
     * Hand the fetched page off to the SDK's {@link SourceCallback} or whatever the subclass wants to do with it. The body has
     * already been read into {@code bodyBytes} by the engine — subclasses must not consume the (already-empty)
     * {@link WebCrawlerResponse#getBody()} stream. The shared connection is passed in so subclasses can call vendor-specific
     * helpers (e.g. shadow-DOM extraction on the Remote WebDriver path) without reopening a connection.
     */
    protected abstract void consume(String url, int depth, byte[] bodyBytes, CrawledContentAttributes attributes,
                                    WebCrawlerConnection connection);

    // ---------------------------------------------------------------------
    // Queue consumer
    // ---------------------------------------------------------------------

    private void consumeLoop() {
        String queueName = configuration.getQueueName();
        Integer maxCrawlDepth = getTargetPages() != null ? getTargetPages().getMaxDepth() : null;
        HashRouteMode hashRouteMode = configuration.getHashRouteMode();
        List<String> acceptedMimeTypes = configuration.getAcceptedMimeTypes();

        // Cache the QueueSession + Queue once outside the per-iteration loop. The session and queue
        // references are stable for the lifetime of the Source — re-fetching them on every poll burns
        // QueueManager lookup overhead for nothing.
        try {
            QueueSession session = configuration.getQueueManager().getQueueSession();
            this.queue = session.getQueue(queueName);
        } catch (Exception e) {
            LOGGER.error("Failed to acquire queue session at consumer-loop start", e);
            return;
        }

        while (!stopped) {
            try {
                Serializable item = queue.poll(POLL_TIMEOUT_MS);

                if (item == null) {
                    continue;
                }

                CrawlTask task = (CrawlTask) item;
                String url = task.getUrl();
                int depth = task.getDepth();

                if (isAlreadyVisited(url)) {
                    duplicateCount.incrementAndGet();
                    LOGGER.debug("Duplicate URL skipped at queue poll: {}", url);
                    continue;
                }

                processUrl(url, depth, maxCrawlDepth, acceptedMimeTypes, hashRouteMode, queueName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (!stopped) {
                    LOGGER.error("Error in crawl queue consumer loop", e);
                }
            }
        }
    }

    private void processUrl(String url, int depth, Integer maxCrawlDepth, List<String> acceptedMimeTypes,
                            HashRouteMode hashRouteMode, String queueName) {
        try {
            // robots.txt — applies BEFORE we burn a fetch. Real per-config UA so per-UA Disallow blocks
            // are honored (canCrawl with null only matches the User-agent: * block).
            if (configuration.getCrawlerOptions().isEnforceRobotsTxt()
                    && !PageHelper.canCrawl(url, connection.getUserAgent())) {
                LOGGER.debug("Skipping URL due to robots.txt: {}", url);
                markVisited(url);
                return;
            }

            // delay between requests.
            int delayMillis = configuration.getCrawlerOptions().getDelayMillis();
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }

            WebCrawlerResponse response = connection.fetchPage(url, connection.getReferrer(),
                                                               configuration.getPageLoadOptions());
            String contentType = response.getContentType();

            // Read the body ONCE into bytes. The InputStream is one-shot; passing the response to both
            // link extraction and consume(...) would empty the stream on the first read and the second
            // reader would see zero bytes. Hand bytes around instead.
            byte[] bodyBytes = readAllBytes(response);

            // Mark visited — we attempted the fetch.
            markVisited(url);

            // Link extraction proceeds regardless of acceptedMimeTypes (so the crawler can walk through
            // HTML pages to find e.g. PDFs the user actually wants). Only HTML/XML responses produce links.
            if (LinkExtractor.canExtractLinks(contentType)) {
                if (maxCrawlDepth == null || depth < maxCrawlDepth) {
                    String body = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
                    List<String> discovered = LinkExtractor.extractLinks(body, url, contentType, hashRouteMode);
                    enqueueLinks(applyFilters(url, discovered), depth + 1, queueName);
                }
            }

            if (!isAcceptedMimeType(contentType, acceptedMimeTypes)) {
                LOGGER.debug("Skipping emit for URL {} — content type {} not in acceptedMimeTypes", url, contentType);
                return;
            }

            CrawledContentAttributes attributes = new CrawledContentAttributes(
                                                                               url, response.getStatusCode(), contentType,
                                                                               depth);

            consume(url, depth, bodyBytes, attributes, connection);
            fetchedCount.incrementAndGet();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Per-page failures are logged-and-swallowed so the crawl continues. Don't mark visited for
            // transient network errors (DNS hiccup, 5xx, socket timeout) — that would prevent a retry for
            // the lifetime of the visited-set ObjectStore. Only mark visited for permanent / non-network
            // failures so a temporarily-flaky URL gets another shot if it's re-seeded.
            LOGGER.error("Failed to crawl URL: {}", url, e);
            if (!isTransientNetworkError(e)) {
                markVisited(url);
            }
        }
    }

    /**
     * Heuristic for "this looks like a transient network failure that may succeed on retry". Anything matching falls into the "do
     * NOT mark visited" bucket so the URL can be re-seeded later. Anything else (parse errors, programming errors, configuration
     * errors) is non-transient and gets marked visited so the queue doesn't loop on a malformed page.
     */
    static boolean isTransientNetworkError(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.io.IOException
                    || cause instanceof java.net.SocketTimeoutException
                    || cause instanceof java.net.UnknownHostException
                    || cause instanceof java.net.ConnectException) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Filter pipeline helpers
    // ---------------------------------------------------------------------

    /** Apply restrictToPath + regex filters to a list of discovered links. */
    private List<String> applyFilters(String baseUrl, List<String> discovered) {
        if (discovered == null || discovered.isEmpty()) {
            return Collections.emptyList();
        }
        CrawlerTargetPagesParameters tp = getTargetPages();
        boolean restrictToPath = tp != null && tp.isRestrictToPath();
        RegexUrlsFilterLogic filterLogic = tp != null ? tp.getRegexUrlsFilterLogic() : null;
        List<String> regexUrls = tp != null ? tp.getRegexUrls() : null;
        String basePath = restrictToPath ? safePath(baseUrl) : null;

        List<String> kept = new ArrayList<>(discovered.size());
        for (String link : discovered) {
            if (restrictToPath && !isUnderPath(link, basePath)) {
                continue;
            }
            // Delegate regex-skip to PageHelper so the Source and operations paths share one
            // implementation: any-match semantics, full-string-anchored matches(), shared compile cache.
            // Master shipped only the PageHelper path; this Source is new code on this branch and must
            // not diverge.
            if (PageHelper.skipUrl(link, filterLogic, regexUrls)) {
                continue;
            }
            kept.add(link);
        }
        return kept;
    }

    private static boolean isUnderPath(String candidate, String basePath) {
        if (basePath == null || basePath.isEmpty() || basePath.equals("/")) {
            return true;
        }
        String candidatePath = safePath(candidate);
        return candidatePath != null && candidatePath.startsWith(basePath);
    }

    private static String safePath(String url) {
        try {
            return new URI(url).getPath();
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isAcceptedMimeType(String contentType, List<String> acceptedMimeTypes) {
        if (acceptedMimeTypes == null || acceptedMimeTypes.isEmpty()) {
            return true;
        }
        // Remote WebDriver fetches don't surface Content-Type — they emit a null sentinel.
        // Pass through in that case rather than rejecting silently, so an HTML-only filter
        // doesn't drop every remote-fetched page. Consumers wanting strict MIME enforcement
        // should restrict crawls to the HTTP fetch path.
        if (contentType == null || contentType.isEmpty()) {
            return true;
        }
        String mime = contentType.contains(";")
                ? contentType.substring(0, contentType.indexOf(';')).trim()
                : contentType.trim();
        for (String accepted : acceptedMimeTypes) {
            if (mime.equalsIgnoreCase(accepted.trim())) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Queue + ObjectStore I/O
    // ---------------------------------------------------------------------

    private void enqueueLinks(List<String> links, int depth, String queueName) {
        if (links == null || links.isEmpty()) {
            return;
        }
        Queue q = this.queue;
        if (q == null) {
            // Shouldn't happen — consumeLoop sets it before any enqueue could fire — but defensive.
            LOGGER.warn("enqueueLinks called before queue was cached; skipping {} links", links.size());
            return;
        }
        try {
            for (String link : links) {
                if (isAlreadyVisited(link)) {
                    duplicateCount.incrementAndGet();
                    continue;
                }
                q.offer(new CrawlTask(link, depth), 0);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to enqueue discovered links", e);
        }
    }

    private boolean isAlreadyVisited(String url) {
        ObjectStore<Serializable> os = getObjectStore();
        if (os == null) {
            return false;
        }
        try {
            return os.contains(url);
        } catch (ObjectStoreException e) {
            return false;
        }
    }

    private void markVisited(String url) {
        ObjectStore<Serializable> os = getObjectStore();
        if (os == null) {
            return;
        }
        try {
            if (!os.contains(url)) {
                os.store(url, url);
            }
        } catch (ObjectStoreException e) {
            LOGGER.warn("Failed to mark URL visited: {}", url);
        }
    }

    // ---------------------------------------------------------------------
    // Body decoding
    // ---------------------------------------------------------------------

    /**
     * Drain the response body into a byte array. Called once per fetched page so link extraction and the subclass's
     * {@link #consume} both work off the same bytes — InputStream is one-shot.
     */
    private static byte[] readAllBytes(WebCrawlerResponse response) {
        try {
            if (response.getBody() == null) {
                return new byte[0];
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = response.getBody().read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            LOGGER.warn("Failed to read response body", e);
            return new byte[0];
        }
    }
}
