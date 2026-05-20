package org.mule.extension.webcrawler.internal.source;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.api.crawl.HashRouteMode;
import org.mule.extension.webcrawler.api.metadata.PageResponseAttributes;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.helper.CrawlTask;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetContentParameters;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.extension.webcrawler.internal.service.LinkExtractor;
import org.mule.extension.webcrawler.internal.service.PageFetchService;
import org.mule.extension.webcrawler.internal.service.factory.PageFetchServiceFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Streaming-shaped crawler — emits one event per fetched page as the crawl progresses. Replaces the deleted batch-shaped
 * {@code crawl-website-full-scan} / {@code crawl-website-streaming} operations.
 *
 * <p>
 * At Source startup, the seed URL is offered onto a VM queue (named {@code queueName}, defaulting to
 * {@code webcrawler-{configName}-source-queue}) and a daemon worker drains it: per task, robots.txt, fetch via
 * {@link PageFetchServiceFactory}, link extraction, and emit. Inline image / document downloads and meta-tag extraction
 * mirror the deleted operations so existing flows can switch entrypoint without changing per-page behavior.
 * </p>
 */
@Alias("crawl-website-source")
@DisplayName("[Crawl] Website (Source)")
@org.mule.sdk.api.annotation.param.MediaType(value = org.mule.sdk.api.annotation.param.MediaType.ANY)
public class CrawlerSource extends Source<InputStream, PageResponseAttributes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerSource.class);

  // How long the worker blocks on queue.poll() before checking the stop flag. Lower wakes the worker more often
  // (idle CPU); higher delays nothing in practice because onStop interrupts the thread to wake it immediately.
  private static final long POLL_TIMEOUT_MS = 1000L;

  @Config
  private WebCrawlerConfiguration configuration;

  @Connection
  private ConnectionProvider<WebCrawlerConnection> connectionProvider;

  @Inject
  private QueueManager queueManager;

  @RefName
  private String configName;

  // ---------------------------------------------------------------------
  // Public API parameters — mirror the deleted crawl-website-streaming / crawl-website-full-scan operations.
  // ---------------------------------------------------------------------

  @Parameter
  @Optional
  @NullSafe
  @DisplayName("Seed URLs")
  @Summary("URLs where the crawl starts. Optional — when empty, the queue starts empty and waits for URLs to be pushed "
      + "in by another flow targeting the same queueName.")
  @Placement(order = 1)
  @Example("https://mac-project.ai/docs")
  private List<String> seedUrls;

  @Parameter
  @DisplayName("Output format")
  @Summary("Format applied to each emitted page body.")
  @Placement(order = 2)
  @Optional(defaultValue = "TEXT")
  private Constants.OutputFormat outputFormat;

  @Parameter
  @Optional
  @DisplayName("Download location")
  @Summary("Filesystem path where images / documents are written when their corresponding download flags are set. "
      + "Required only if downloadImages or downloadDocuments is true.")
  @Placement(order = 3)
  @Example("/users/mulesoft/downloads")
  private String downloadPath;

  @ParameterGroup(name = "Target Pages")
  private CrawlerTargetPagesParameters targetPagesParameters;

  @ParameterGroup(name = "Target Content")
  private CrawlerTargetContentParameters targetContentParameters;

  @Parameter
  @Optional
  @DisplayName("Queue Name")
  @Summary("VM queue used to schedule URLs for crawling. When omitted, defaults to 'webcrawler-{configName}-source-queue'.")
  @Placement(tab = "Advanced")
  @Example("webcrawler-urls")
  private String queueName;

  @Parameter
  @Optional
  @DisplayName("Object Store")
  @Summary("Object store used to track already-visited URLs. When omitted, no dedup is performed.")
  @Placement(tab = "Advanced")
  private ObjectStore<Serializable> objectStore;

  // ---------------------------------------------------------------------
  // Lifecycle state
  // ---------------------------------------------------------------------

  private volatile boolean stopped;
  private Thread consumerThread;
  private SourceCallback<InputStream, PageResponseAttributes> sourceCallback;
  private final AtomicLong fetchedCount = new AtomicLong();
  private final AtomicLong duplicateCount = new AtomicLong();

  /** Queue cached at consumer-loop start; shared between {@code consumeLoop} and {@link #enqueueLinks}. */
  private volatile Queue queue;

  /**
   * Connection acquired once at {@link #onStart} and reused for every page. Disconnecting per page would tear down and
   * rebuild the Remote WebDriver session each time (5–15s of session bring-up burned per page).
   */
  private WebCrawlerConnection connection;

  // ---------------------------------------------------------------------
  // SDK Source lifecycle
  // ---------------------------------------------------------------------

  @Override
  public void onStart(SourceCallback<InputStream, PageResponseAttributes> sourceCallback) throws MuleException {
    this.sourceCallback = sourceCallback;

    if (queueName == null || queueName.isEmpty()) {
      queueName = "webcrawler-" + configName + "-source-queue";
    }
    queueManager.setQueueConfiguration(queueName, new DefaultQueueConfiguration(0, false));
    seedQueue();

    try {
      this.connection = connectionProvider.connect();
    } catch (Exception e) {
      throw new DefaultMuleException("Failed to connect at Source startup", e);
    }
    stopped = false;
    consumerThread = new Thread(this::consumeLoop, "webcrawler-queue-consumer");
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
  }

  private void seedQueue() throws MuleException {
    if (seedUrls == null || seedUrls.isEmpty()) {
      LOGGER.info("No seed URLs provided; queue '{}' will start empty", queueName);
      return;
    }
    try {
      QueueSession session = queueManager.getQueueSession();
      Queue q = session.getQueue(queueName);
      for (String seed : seedUrls) {
        q.offer(new CrawlTask(seed, 0), 0);
      }
      LOGGER.info("Seeded queue '{}' with {} URL(s)", queueName, seedUrls.size());
    } catch (Exception e) {
      throw new DefaultMuleException("Failed to seed URL queue: " + e.getMessage(), e);
    }
  }

  // ---------------------------------------------------------------------
  // Queue consumer
  // ---------------------------------------------------------------------

  private void consumeLoop() {
    Integer maxCrawlDepth = targetPagesParameters != null ? targetPagesParameters.getMaxDepth() : null;
    HashRouteMode hashRouteMode = HashRouteMode.IGNORE;

    try {
      QueueSession session = queueManager.getQueueSession();
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
        String taskUrl = task.getUrl();
        int depth = task.getDepth();

        if (isAlreadyVisited(taskUrl)) {
          duplicateCount.incrementAndGet();
          LOGGER.debug("Duplicate URL skipped at queue poll: {}", taskUrl);
          continue;
        }
        processUrl(taskUrl, depth, maxCrawlDepth, hashRouteMode);
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

  private void processUrl(String taskUrl, int depth, Integer maxCrawlDepth, HashRouteMode hashRouteMode) {
    try {
      if (configuration.getCrawlerOptions().isEnforceRobotsTxt()
          && !PageHelper.canCrawl(taskUrl, connection.getUserAgent())) {
        LOGGER.debug("Skipping URL due to robots.txt: {}", taskUrl);
        markVisited(taskUrl);
        return;
      }

      int delayMillis = configuration.getCrawlerOptions().getDelayMillis();
      if (delayMillis > 0) {
        Thread.sleep(delayMillis);
      }

      PageFetchService service = PageFetchServiceFactory.getService(connection);
      Document document = service.getPageSource(taskUrl, connection.getReferrer(), configuration.getPageLoadOptions());

      markVisited(taskUrl);

      if (maxCrawlDepth == null || depth < maxCrawlDepth) {
        List<String> discovered = LinkExtractor.extractLinks(document.html(), taskUrl, "text/html", hashRouteMode);
        enqueueLinks(applyFilters(taskUrl, discovered), depth + 1);
      }

      emit(taskUrl, document);
      fetchedCount.incrementAndGet();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOGGER.error("Failed to crawl URL: {}", taskUrl, e);
      if (!isTransientNetworkError(e)) {
        markVisited(taskUrl);
      }
    }
  }

  private void emit(String taskUrl, Document document) {
    try {
      String formatted = PageHelper.getPageContent(document, targetContentParameters.getTags(), outputFormat);

      if (targetContentParameters.isGetMetaTags()) {
        // Meta tags surfaced via the body when the user opts in — matches the deleted crawl-website-streaming behavior.
        JSONArray metaTags = PageHelper.getPageMetaTags(document);
        formatted = formatted + "\n" + metaTags.toString();
      }

      if (targetContentParameters.isDownloadImages() && downloadPath != null && !downloadPath.isEmpty()) {
        try {
          PageHelper.downloadWebsiteImages(document, downloadPath, targetContentParameters.getMaxImageNumber());
        } catch (Exception ex) {
          LOGGER.warn("Image download failed for {}: {}", taskUrl, ex.getMessage());
        }
      }
      if (targetContentParameters.isDownloadDocuments() && downloadPath != null && !downloadPath.isEmpty()) {
        try {
          PageHelper.downloadFiles(document, downloadPath, targetContentParameters.getMaxDocumentNumber());
        } catch (Exception ex) {
          LOGGER.warn("Document download failed for {}: {}", taskUrl, ex.getMessage());
        }
      }

      HashMap<String, Object> attrMap = new HashMap<>();
      attrMap.put("url", taskUrl);
      attrMap.put("title", document.title());
      PageResponseAttributes attributes = new PageResponseAttributes(attrMap);

      Result<InputStream, PageResponseAttributes> result = Result.<InputStream, PageResponseAttributes>builder()
          .output(new ByteArrayInputStream(formatted.getBytes(StandardCharsets.UTF_8)))
          .attributes(attributes)
          .mediaType(mediaTypeFor(outputFormat))
          .build();
      sourceCallback.handle(result);
    } catch (Exception e) {
      LOGGER.error("Failed to emit fetched page for URL {}: {}", taskUrl, e.getMessage(), e);
    }
  }

  private static MediaType mediaTypeFor(Constants.OutputFormat fmt) {
    if (fmt == null) {
      return MediaType.ANY;
    }
    switch (fmt) {
      case HTML:
        return MediaType.TEXT.withCharset(StandardCharsets.UTF_8);
      case MARKDOWN:
        return MediaType.create("text", "markdown", StandardCharsets.UTF_8);
      case TEXT:
      default:
        return MediaType.TEXT.withCharset(StandardCharsets.UTF_8);
    }
  }

  // ---------------------------------------------------------------------
  // Filter pipeline helpers
  // ---------------------------------------------------------------------

  private List<String> applyFilters(String baseUrl, List<String> discovered) {
    if (discovered == null || discovered.isEmpty()) {
      return Collections.emptyList();
    }
    boolean restrictToPath = targetPagesParameters != null && targetPagesParameters.isRestrictToPath();
    Constants.RegexUrlsFilterLogic filterLogic =
        targetPagesParameters != null ? targetPagesParameters.getRegexUrlsFilterLogic() : null;
    List<String> regexUrls = targetPagesParameters != null ? targetPagesParameters.getRegexUrls() : null;
    String basePath = restrictToPath ? safePath(baseUrl) : null;

    List<String> kept = new ArrayList<>(discovered.size());
    for (String link : discovered) {
      if (restrictToPath && !isUnderPath(link, basePath)) {
        continue;
      }
      if (matchesRegexSkip(link, filterLogic, regexUrls)) {
        continue;
      }
      kept.add(link);
    }
    return kept;
  }

  private static boolean matchesRegexSkip(String link, Constants.RegexUrlsFilterLogic filterLogic,
                                          List<String> regexUrls) {
    if (filterLogic == null || regexUrls == null || regexUrls.isEmpty()) {
      return false;
    }
    boolean matches = regexUrls.stream().anyMatch(p -> Pattern.compile(p).matcher(link).matches());
    return (filterLogic == Constants.RegexUrlsFilterLogic.INCLUDE && !matches)
        || (filterLogic == Constants.RegexUrlsFilterLogic.EXCLUDE && matches);
  }

  private static boolean isUnderPath(String candidate, String basePath) {
    if (basePath == null || basePath.isEmpty() || basePath.equals("/")) {
      return true;
    }
    String candidatePath = safePath(candidate);
    return candidatePath != null && candidatePath.startsWith(basePath);
  }

  private static String safePath(String candidate) {
    try {
      return new URI(candidate).getPath();
    } catch (Exception e) {
      return null;
    }
  }

  // ---------------------------------------------------------------------
  // Queue + ObjectStore I/O
  // ---------------------------------------------------------------------

  private void enqueueLinks(List<String> links, int depth) {
    if (links == null || links.isEmpty()) {
      return;
    }
    Queue q = this.queue;
    if (q == null) {
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

  private boolean isAlreadyVisited(String taskUrl) {
    if (objectStore == null) {
      return false;
    }
    try {
      return objectStore.contains(taskUrl);
    } catch (ObjectStoreException e) {
      return false;
    }
  }

  private void markVisited(String taskUrl) {
    if (objectStore == null) {
      return;
    }
    try {
      if (!objectStore.contains(taskUrl)) {
        objectStore.store(taskUrl, taskUrl);
      }
    } catch (ObjectStoreException e) {
      LOGGER.warn("Failed to mark URL visited: {}", taskUrl);
    }
  }

  /**
   * Heuristic for "this looks like a transient network failure that may succeed on retry". Anything matching falls into the
   * "do NOT mark visited" bucket so the URL can be re-seeded later.
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
}
