package org.mule.extension.webcrawler.internal.config;

import org.mule.extension.webcrawler.api.HashRouteMode;
import org.mule.extension.webcrawler.internal.connection.provider.HttpConnectionProvider;
import org.mule.extension.webcrawler.internal.connection.provider.RemoteWebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.model.CrawlTask;
import org.mule.extension.webcrawler.internal.operation.CrawlOperations;
import org.mule.extension.webcrawler.internal.operation.PageOperations;
import org.mule.extension.webcrawler.internal.operation.SearchOperations;
import org.mule.extension.webcrawler.internal.source.CrawlerSource;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.Sources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple operations since
 * they represent something core from the extension.
 *
 * <p>
 * The configuration is {@link Initialisable}: at app startup, the seed URL is pushed onto the VM queue named {@code queueName} so
 * the {@link CrawlerSource} can begin draining it as soon as it starts.
 * </p>
 */
@Configuration(name = "config")
@ConnectionProviders({HttpConnectionProvider.class, RemoteWebDriverConnectionProvider.class})
@Operations({CrawlOperations.class, PageOperations.class, SearchOperations.class})
@Sources(CrawlerSource.class)
public class WebCrawlerConfiguration implements Initialisable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebCrawlerConfiguration.class);

    /** Default MIME allow-list — the parseable set. Users override to admit PDFs, images, etc. */
    static final List<String> DEFAULT_ACCEPTED_MIME_TYPES = Collections.unmodifiableList(Arrays.asList(
                                                                                                       "text/html",
                                                                                                       "application/xhtml+xml",
                                                                                                       "text/xml",
                                                                                                       "application/xml"));

    @Inject
    private QueueManager queueManager;

    /**
     * Mule injects the configuration's XML element name (e.g. {@code httpOperationsConfig}). Used to synthesize a unique default
     * queue name when the user hasn't supplied {@code queueName} explicitly, so ops-only configs don't have to declare a
     * never-used queue name.
     */
    @RefName
    private String configName;

    @Parameter
    @Optional
    @DisplayName("Seed URL")
    @Summary("URL where the crawl starts. The configuration seeds this URL onto the queue at startup. Only required when the configuration is used with the crawl-website-source Source — operations that take a per-call URL (page-*, get-sitemap, search-google) do not consult this value.")
    @Placement(order = 1)
    @Example("https://mac-project.ai/docs")
    private String url;

    @Parameter
    @Optional
    @DisplayName("Queue Name")
    @Summary("Name of the VM queue used to schedule URLs for crawling. Optional — when omitted, a unique default of "
            + "'webcrawler-{configName}-queue' is synthesized from the config's XML name so ops-only configs don't have to declare "
            + "an unused queue. Set this explicitly only when you need to share a queue across configs or align with an "
            + "external naming scheme.")
    @Placement(order = 2)
    @Example("webcrawler-urls")
    private String queueName;

    @ParameterGroup(name = "Crawler Options")
    private CrawlerOptions crawlerOptions;

    @ParameterGroup(name = "Page Load Options (WebDriver)")
    private PageLoadOptions pageLoadOptions;

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Accepted MIME types")
    @Summary("Only pages whose Content-Type matches an entry in this list will be emitted. Link extraction still proceeds on every fetched page regardless of MIME match. Defaults to the parseable set (text/html, application/xhtml+xml, text/xml, application/xml).")
    @Placement(tab = "Advanced", order = 1)
    private List<String> acceptedMimeTypes;

    @Parameter
    @Optional(defaultValue = "IGNORE")
    @DisplayName("Hash route mode")
    @Summary("How the link extractor handles URL fragments. IGNORE (default) strips fragments so foo.html#section-a and foo.html#section-b dedup to foo.html. PATH rewrites #/users to /users. PRESERVE keeps fragments verbatim for pure SPAs.")
    @Placement(tab = "Advanced", order = 2)
    private HashRouteMode hashRouteMode;

    @Override
    public void initialise() throws InitialisationException {
        if (queueName == null || queueName.isEmpty()) {
            queueName = "webcrawler-" + configName + "-queue";
            LOGGER.debug("queueName not supplied for config '{}'; synthesized default '{}'", configName, queueName);
        }
        queueManager.setQueueConfiguration(queueName, new DefaultQueueConfiguration(0, false));
        seedQueue();
    }

    private void seedQueue() throws InitialisationException {
        if (url == null || url.isEmpty()) {
            LOGGER.info("No seed URL provided; queue '{}' will start empty", queueName);
            return;
        }
        try {
            QueueSession session = queueManager.getQueueSession();
            Queue queue = session.getQueue(queueName);
            queue.offer(new CrawlTask(url, 0), 0);
            LOGGER.info("Seeded queue '{}' with seed URL: {}", queueName, url);
        } catch (Exception e) {
            throw new InitialisationException(
                                              I18nMessageFactory
                                                      .createStaticMessage("Failed to seed URL queue: " + e.getMessage()),
                                              e, this);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getQueueName() {
        return queueName;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public CrawlerOptions getCrawlerOptions() {
        return crawlerOptions;
    }

    public void setCrawlerOptions(CrawlerOptions crawlerOptions) {
        this.crawlerOptions = crawlerOptions;
    }

    public PageLoadOptions getPageLoadOptions() {
        return pageLoadOptions;
    }

    public void setPageLoadOptions(PageLoadOptions pageLoadOptions) {
        this.pageLoadOptions = pageLoadOptions;
    }

    public List<String> getAcceptedMimeTypes() {
        return (acceptedMimeTypes == null || acceptedMimeTypes.isEmpty())
                ? DEFAULT_ACCEPTED_MIME_TYPES
                : acceptedMimeTypes;
    }

    public HashRouteMode getHashRouteMode() {
        return hashRouteMode == null ? HashRouteMode.IGNORE : hashRouteMode;
    }
}
