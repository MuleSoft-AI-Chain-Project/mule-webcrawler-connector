package org.mule.extension.webcrawler.internal.helper.page;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mule.extension.webcrawler.internal.constant.Constants;

/**
 * Pure-static JUnit coverage for {@link PageHelper}. No Selenium, no full Mule
 * boot — Jsoup fixtures + WireMock only.
 */
class PageHelperTest {

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
        // Clear robotsTxtCache so repeat tests don't cross-pollute.
        try {
            Field f = PageHelper.class.getDeclaredField("robotsTxtCache");
            f.setAccessible(true);
            ((Map<?, ?>) f.get(null)).clear();
        } catch (Exception ignored) {
        }
    }

    // ---------- getPageMetaTags ----------

    @Test
    void getPageMetaTagsProperty() {
        String html = "<html><head>"
            + "<meta property=\"og:title\" content=\"Hello\">"
            + "<meta name=\"description\" content=\"A page\">"
            + "<meta name=\"noContent\" content=\"\">"       // skipped (empty content)
            + "<meta content=\"noNameNoProperty\">"         // skipped (no name & no property)
            + "</head></html>";
        Document doc = Jsoup.parse(html, "https://example.com");

        JSONArray result = PageHelper.getPageMetaTags(doc);
        assertEquals(2, result.length(), "Only tags with (name|property) + content survive");

        boolean hasProperty = false;
        boolean hasName = false;
        for (int i = 0; i < result.length(); i++) {
            JSONObject obj = result.getJSONObject(i);
            if (obj.has("property")) {
                assertEquals("og:title", obj.getString("property"));
                hasProperty = true;
            }
            if (obj.has("name")) {
                assertEquals("description", obj.getString("name"));
                hasName = true;
            }
        }
        assertTrue(hasProperty, "property branch reached");
        assertTrue(hasName, "name branch reached");
    }

    // ---------- getPageInsights — all 6 insight types ----------

    private static Document insightsFixture() {
        String html = "<html><head><title>Insights</title></head><body>"
            + "<a href=\"/internal\">internal</a>"
            + "<a href=\"https://other.com/ext\">external</a>"
            + "<a href=\"#ref\">ref</a>"
            + "<a href=\"https://cdn.example.com/doc.pdf\">doc</a>"
            + "<iframe src=\"https://frame.example.com/x\"></iframe>"
            + "<img src=\"https://cdn.example.com/pic.png\" />"
            + "<div><p>hello world</p><p>again</p></div>"
            + "<h1>h1</h1><h2>h2</h2>"
            + "</body></html>";
        return Jsoup.parse(html, "https://example.com/");
    }

    @Test
    void getPageInsightsAll() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.ALL);
        assertNotNull(insights.get("links"));
        assertNotNull(insights.get("pageStats"));
        assertEquals("https://example.com/", insights.get("url"));
    }

    @Test
    void getPageInsightsInternalLinks() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.INTERNALLINKS);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertNotNull(links.get("internal"));
    }

    @Test
    void getPageInsightsExternalLinks() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.EXTERNALLINKS);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertTrue(links.get("external").stream().anyMatch(s -> s.contains("other.com")));
    }

    @Test
    void getPageInsightsReferenceLinks() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.REFERENCELINKS);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertNotNull(links.get("reference"));
    }

    @Test
    void getPageInsightsIframeLinks() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.IFRAMELINKS);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertTrue(links.get("iframe").stream().anyMatch(s -> s.contains("frame.example.com")));
    }

    @Test
    void getPageInsightsImageLinks() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.IMAGELINKS);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertTrue(links.get("images").stream().anyMatch(s -> s.contains("pic.png")));
    }

    @Test
    void getPageInsightsDocumentLinks() {
        Map<String, Object> insights =
                PageHelper.getPageInsights(insightsFixture(), null, Constants.PageInsightType.DOCUMENTLINKS);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertTrue(links.get("documents").stream().anyMatch(s -> s.endsWith(".pdf")));
    }

    @Test
    void getPageInsightsElementCountStats() {
        Map<String, Object> insights = PageHelper.getPageInsights(
                insightsFixture(),
                Arrays.asList("div", "p", "h1"),
                Constants.PageInsightType.ELEMENTCOUNTSTATS);
        @SuppressWarnings("unchecked")
        Map<String, Integer> stats = (Map<String, Integer>) insights.get("pageStats");
        assertNotNull(stats);
        assertTrue(stats.getOrDefault("p", 0) >= 2);
        assertTrue(stats.containsKey("wordCount"));
    }

    @Test
    void getPageInsightsElementCountStatsDefaultTags() {
        Map<String, Object> insights = PageHelper.getPageInsights(
                insightsFixture(), null, Constants.PageInsightType.ELEMENTCOUNTSTATS);
        @SuppressWarnings("unchecked")
        Map<String, Integer> stats = (Map<String, Integer>) insights.get("pageStats");
        // default tags are div/p/h1..h5
        assertNotNull(stats.get("div"));
        assertNotNull(stats.get("h1"));
    }

    // ---------- skipUrl / regex filter ----------

    @Test
    void getPageInsightsWithIncludeFilter() {
        Map<String, Object> insights = PageHelper.getPageInsights(
                insightsFixture(), null, Constants.PageInsightType.ALL,
                Constants.RegexUrlsFilterLogic.INCLUDE,
                List.of("^https://cdn\\.example\\.com.*"));
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        // External link https://other.com/ext should be filtered out (no "cdn.example.com")
        assertFalse(links.get("external").stream().anyMatch(s -> s.contains("other.com")),
                "INCLUDE regex should drop non-matching external links");
        // call again to exercise COMPILED_PATTERN_CACHE hit branch
        Map<String, Object> again = PageHelper.getPageInsights(
                insightsFixture(), null, Constants.PageInsightType.ALL,
                Constants.RegexUrlsFilterLogic.INCLUDE,
                List.of("^https://cdn\\.example\\.com.*"));
        assertNotNull(again);
    }

    @Test
    void getPageInsightsWithExcludeFilter() {
        Map<String, Object> insights = PageHelper.getPageInsights(
                insightsFixture(), null, Constants.PageInsightType.ALL,
                Constants.RegexUrlsFilterLogic.EXCLUDE,
                List.of("^https://other\\.com.*"));
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> links = (Map<String, Set<String>>) insights.get("links");
        assertFalse(links.get("external").stream().anyMatch(s -> s.contains("other.com")),
                "EXCLUDE regex should drop matching external links");
    }

    @Test
    void getPageInsightsCatchWrapsInModuleException() {
        // Passing null document causes NPE inside try — class wraps as ModuleException.
        try {
            PageHelper.getPageInsights(null, null, Constants.PageInsightType.ALL);
            fail("should have thrown");
        } catch (RuntimeException e) {
            // ModuleException extends RuntimeException; accepted.
            assertNotNull(e);
        }
    }

    // ---------- getPageContent — all 4 switch branches + nested-selected ----------

    private static Document contentFixture() {
        return Jsoup.parse(
            "<html><body><div><p>Hello World</p></div><section>aside</section></body></html>",
            "https://example.com/");
    }

    @Test
    void getPageContentTextNoTags() {
        String text = PageHelper.getPageContent(contentFixture(), null, Constants.OutputFormat.TEXT);
        assertTrue(text.contains("Hello World"));
    }

    @Test
    void getPageContentTextWithTagsNestedSelected() {
        // div includes p — p should be skipped (nested-inside-selected).
        String text = PageHelper.getPageContent(contentFixture(),
                Arrays.asList("div", "p"),
                Constants.OutputFormat.TEXT);
        // Must contain the outer text once — with p skipped, "Hello World" still appears,
        // but exactly once not duplicated.
        assertTrue(text.contains("Hello World"));
        int first = text.indexOf("Hello World");
        int second = text.indexOf("Hello World", first + 1);
        assertEquals(-1, second, "p is nested inside div and should not be collected twice");
    }

    @Test
    void getPageContentHtml() {
        String html = PageHelper.getPageContent(contentFixture(),
                Arrays.asList("div"),
                Constants.OutputFormat.HTML);
        assertTrue(html.contains("<p>Hello World</p>"));
    }

    @Test
    void getPageContentHtmlNoTagsReturnsFullDocument() {
        String html = PageHelper.getPageContent(contentFixture(), null, Constants.OutputFormat.HTML);
        assertTrue(html.contains("<html"));
    }

    @Test
    void getPageContentMarkdown() {
        String md = PageHelper.getPageContent(contentFixture(), null, Constants.OutputFormat.MARKDOWN);
        assertNotNull(md);
        assertTrue(md.length() > 0);
    }

    // ---------- getPageRawHtmlContent branches ----------

    @Test
    void getPageRawHtmlContentMatchedTag() {
        String html = PageHelper.getPageRawHtmlContent(contentFixture(), Arrays.asList("div"));
        assertTrue(html.contains("<p>Hello World</p>"));
    }

    @Test
    void getPageRawHtmlContentNoTagMatchFallsThrough() {
        String html = PageHelper.getPageRawHtmlContent(contentFixture(), Arrays.asList("nothere"));
        assertTrue(html.contains("<html"));
    }

    @Test
    void getPageRawHtmlContentEmptyTagsList() {
        String html = PageHelper.getPageRawHtmlContent(contentFixture(), java.util.Collections.emptyList());
        assertTrue(html.contains("<html"));
    }

    // ---------- downloadSingleImage — data URL branches ----------

    @Test
    void downloadSingleImageDataUrlValidBase64(@TempDir Path tmp) throws Exception {
        // 1x1 transparent PNG in base64
        String pngB64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        String url = "data:image/png;base64," + pngB64;
        JSONObject json = PageHelper.downloadSingleImage(url, tmp.toString());
        assertNotNull(json);
        assertEquals("image/png", json.getString("mimeType"));
        assertTrue(json.getString("fileName").endsWith(".png"));
    }

    @Test
    void downloadSingleImageDataUrlEmptyBase64(@TempDir Path tmp) throws Exception {
        String url = "data:image/png;base64,";
        JSONObject json = PageHelper.downloadSingleImage(url, tmp.toString());
        assertNull(json, "empty base64 payload -> null JSON");
    }

    @Test
    void downloadSingleImageDataUrlInvalidBase64(@TempDir Path tmp) throws Exception {
        String url = "data:image/png;base64,@@@notvalid@@@";
        JSONObject json = PageHelper.downloadSingleImage(url, tmp.toString());
        assertNull(json, "invalid base64 -> null JSON");
    }

    @Test
    void downloadSingleImageWithSubFolder(@TempDir Path tmp) throws Exception {
        String pngB64 = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});
        String url = "data:image/png;base64," + pngB64;
        JSONObject json = PageHelper.downloadSingleImage(url, tmp.toString(), "sub");
        assertNotNull(json);
        assertEquals("sub", json.getString("relativePath"));
    }

    // ---------- downloadFile (WireMock) ----------

    @Test
    void downloadFileHappyPathWithContentDisposition(@TempDir Path tmp) {
        wireMock.stubFor(get(urlEqualTo("/file.pdf"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Disposition", "attachment; filename=\"report.pdf\"")
                        .withHeader("Content-Type", "application/pdf")
                        .withBody("PDF-PAYLOAD")));
        String fileUrl = "http://localhost:" + wireMock.port() + "/file.pdf";
        JSONObject json = PageHelper.downloadFile(fileUrl, tmp.toString());
        assertNotNull(json);
        assertEquals("report.pdf", json.getString("fileName"));
        assertEquals("application/pdf", json.getString("mimeType"));
    }

    @Test
    void downloadFileHappyPathFallbackFileName(@TempDir Path tmp) {
        wireMock.stubFor(get(urlEqualTo("/inline.pdf"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("PDF-PAYLOAD")));
        String fileUrl = "http://localhost:" + wireMock.port() + "/inline.pdf";
        JSONObject json = PageHelper.downloadFile(fileUrl, tmp.toString(), "sub");
        assertNotNull(json);
        assertEquals("inline.pdf", json.getString("fileName"));
        assertEquals("sub", json.getString("relativePath"));
    }

    @Test
    void downloadFileNon200ReturnsNull(@TempDir Path tmp) {
        wireMock.stubFor(get(urlEqualTo("/missing.pdf"))
                .willReturn(aResponse().withStatus(404)));
        String fileUrl = "http://localhost:" + wireMock.port() + "/missing.pdf";
        JSONObject json = PageHelper.downloadFile(fileUrl, tmp.toString());
        assertNull(json);
    }

    @Test
    void downloadFileIoErrorReturnsNull(@TempDir Path tmp) {
        JSONObject json = PageHelper.downloadFile(
                "http://localhost:1/nothing-here", tmp.toString());
        assertNull(json, "connection-refused yields IOException -> null");
    }

    // ---------- canCrawl / robots.txt ----------

    @Test
    void canCrawlAllowsByDefaultWhenRobotsNotRetrievable() {
        // hostname that cannot be connected -> getRobotsTxt returns null -> canCrawl returns true
        boolean canCrawl = PageHelper.canCrawl(
                "http://localhost:1/", "Mozilla/5.0 (compatible; Googlebot/2.1)");
        assertTrue(canCrawl);
    }

    @Test
    void canCrawlDisallowsAllForMatchingAgent() {
        seedRobotsCache("https://example.com",
                "User-agent: TestBot\nDisallow: /");
        boolean canCrawl = PageHelper.canCrawl("https://example.com/path", "TestBot");
        assertFalse(canCrawl);
    }

    @Test
    void canCrawlAllowsPathForWildcardAgent() {
        seedRobotsCache("https://example.com",
                "# a comment\n\nUser-agent: *\nAllow: /public\nDisallow: /private\n");
        boolean allowed = PageHelper.canCrawl("https://example.com/public/index", "AnyBot");
        assertTrue(allowed);
        boolean disallowed = PageHelper.canCrawl("https://example.com/private/x", "AnyBot");
        assertFalse(disallowed);
    }

    @Test
    void canCrawlIgnoresCommentsAndBlankLines() {
        seedRobotsCache("https://example.com",
                "# comment line\n   \nUser-agent: *\nDisallow: /blocked\n");
        assertTrue(PageHelper.canCrawl("https://example.com/ok", "A"));
        assertFalse(PageHelper.canCrawl("https://example.com/blocked/page", "A"));
    }

    @Test
    void canCrawlUserAgentDoesNotMatchIsIgnored() {
        seedRobotsCache("https://example.com",
                "User-agent: SomeOther\nDisallow: /\n");
        // Our agent is different -> no matched block -> default allowed true
        assertTrue(PageHelper.canCrawl("https://example.com/path", "MyAgent"));
    }

    /** Seed the private {@code robotsTxtCache} map with a host key that matches what getRobotsTxt would look up. */
    @SuppressWarnings("unchecked")
    private static void seedRobotsCache(String baseUrlHost, String content) {
        try {
            Field f = PageHelper.class.getDeclaredField("robotsTxtCache");
            f.setAccessible(true);
            ConcurrentHashMap<String, String> cache =
                    (ConcurrentHashMap<String, String>) f.get(null);
            cache.put(baseUrlHost, content);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    // ---------- downloadWebsiteImages / downloadFiles (thin wrappers) ----------

    @Test
    void downloadWebsiteImagesWithNoImagesReturnsEmpty(@TempDir Path tmp) throws Exception {
        Document doc = Jsoup.parse("<html><body><p>no images</p></body></html>",
                "https://example.com/");
        JSONArray imgs = PageHelper.downloadWebsiteImages(doc, tmp.toString(), 10);
        assertEquals(0, imgs.length());
    }

    @Test
    void downloadFilesWithNoDocsReturnsEmpty(@TempDir Path tmp) throws Exception {
        Document doc = Jsoup.parse("<html><body><p>no docs</p></body></html>",
                "https://example.com/");
        JSONArray files = PageHelper.downloadFiles(doc, tmp.toString(), 10);
        assertEquals(0, files.length());
    }

    // ---------- savePageContents ----------

    @Test
    void savePageContentsWritesFile(@TempDir Path tmp) throws Exception {
        JSONObject json = new JSONObject();
        json.put("hello", "world");
        String fileName = PageHelper.savePageContents(json, tmp.toString(), "test title");
        assertNotNull(fileName);
        File f = new File(tmp.toFile(), fileName);
        assertTrue(f.exists());
    }
}
