package org.mule.extension.webcrawler.internal.source;

import org.mule.extension.webcrawler.api.OutputFormat;
import org.mule.extension.webcrawler.api.metadata.CrawledContentAttributes;
import org.mule.extension.webcrawler.internal.connection.RemoteWebDriverConnection;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetContentParameters;
import org.mule.extension.webcrawler.internal.helper.parameter.CrawlerTargetPagesParameters;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.runtime.operation.Result;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streaming-shaped crawler — emits one event per fetched page as the crawl progresses. Replaces the batch-shaped
 * {@code crawl-website-full-scan} / {@code crawl-website-streaming} operations that shipped on master.
 *
 * <p>
 * The Source seeds from {@link org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration#getUrl()} (set via config),
 * drains the queue defined by {@code queueName} on the config, deduplicates against the supplied {@code objectStore}, and emits a
 * single Mule event per accepted page. Inline downloads (images / documents) and meta-tag extraction match the behavior of the
 * legacy crawl operations.
 * </p>
 */
@Alias("crawl-website-source")
@DisplayName("[Crawl] Website (Source)")
@org.mule.sdk.api.annotation.param.MediaType(value = org.mule.sdk.api.annotation.param.MediaType.ANY)
public class CrawlerSource extends AbstractCrawlerSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerSource.class);

    // ---------------------------------------------------------------------
    // Public API parameters — placement orders preserved from master's crawl-website-full-scan.
    // ---------------------------------------------------------------------

    @Parameter
    @DisplayName("Output format")
    @Summary("Format applied to each emitted page body. Match master's crawl operations.")
    @Placement(order = 1)
    @Optional(defaultValue = "TEXT")
    private OutputFormat outputFormat;

    @Parameter
    @Optional
    @DisplayName("Download location")
    @Summary("Filesystem path where images / documents are written when their corresponding download flags are set. Required only if downloadImages or downloadDocuments is true.")
    @Placement(order = 2)
    @Example("/users/mulesoft/downloads")
    private String downloadPath;

    @ParameterGroup(name = "Target Pages")
    private CrawlerTargetPagesParameters targetPagesParameters;

    @ParameterGroup(name = "Target Content")
    private CrawlerTargetContentParameters targetContentParameters;

    @Parameter
    @Optional
    @DisplayName("Object Store")
    @Summary("Object store used to track already-visited URLs across crawl. When omitted, a transient in-memory store is used (lost on app restart).")
    @Placement(tab = "Advanced")
    private ObjectStore<Serializable> objectStore;

    @Override
    protected ObjectStore<Serializable> getObjectStore() {
        return objectStore;
    }

    @Override
    protected CrawlerTargetPagesParameters getTargetPages() {
        return targetPagesParameters;
    }

    @Override
    protected void consume(String url, int depth, byte[] bodyBytes, CrawledContentAttributes attributes,
                           WebCrawlerConnection connection) {
        try {
            Document document = Jsoup.parse(new String(bodyBytes, StandardCharsets.UTF_8), url);

            // Apply shadow-DOM extraction when running against a remote WebDriver and the user opted in.
            if (connection instanceof RemoteWebDriverConnection
                    && configuration.getPageLoadOptions() != null
                    && configuration.getPageLoadOptions().isExtractShadowDom()) {
                ((RemoteWebDriverConnection) connection)
                        .injectAllShadowDOMs(document, configuration.getPageLoadOptions().getShadowHostXPath());
            }

            // Format the body per outputFormat + content-tag selectors.
            String formatted = PageHelper.getPageContent(document, targetContentParameters.getTags(), outputFormat);

            // Optional meta-tags — surfaced on the emitted event's attributes.metaTags so the user-side
            // flow can read them without re-parsing the body.
            if (targetContentParameters.isGetMetaTags()) {
                JSONArray metaTagsJson = PageHelper.getPageMetaTags(document);
                attributes = new CrawledContentAttributes(attributes.getUrl(), attributes.getStatusCode(),
                                                          attributes.getContentType(), attributes.getDepth(),
                                                          metaTagsJson.toString());
            }

            if (targetContentParameters.isDownloadImages() && downloadPath != null && !downloadPath.isEmpty()) {
                try {
                    PageHelper.downloadWebsiteImages(document, downloadPath, targetContentParameters.getMaxImageNumber());
                } catch (Exception ex) {
                    LOGGER.warn("Image download failed for {}: {}", url, ex.getMessage());
                }
            }
            if (targetContentParameters.isDownloadDocuments() && downloadPath != null && !downloadPath.isEmpty()) {
                try {
                    PageHelper.downloadFiles(document, downloadPath, targetContentParameters.getMaxDocumentNumber());
                } catch (Exception ex) {
                    LOGGER.warn("Document download failed for {}: {}", url, ex.getMessage());
                }
            }

            MediaType mediaType = mediaTypeFor(outputFormat);
            Result<InputStream, CrawledContentAttributes> result =
                    Result.<InputStream, CrawledContentAttributes>builder()
                            .output(new ByteArrayInputStream(formatted.getBytes(StandardCharsets.UTF_8)))
                            .attributes(attributes)
                            .mediaType(mediaType)
                            .build();
            sourceCallback.handle(result);
        } catch (Exception e) {
            LOGGER.error("Failed to emit fetched page for URL {}: {}", url, e.getMessage(), e);
        }
    }

    private static MediaType mediaTypeFor(OutputFormat fmt) {
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
}
