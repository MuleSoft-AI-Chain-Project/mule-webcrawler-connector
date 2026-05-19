package org.mule.extension.webcrawler.internal.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.mule.extension.webcrawler.api.HashRouteMode;

import java.util.List;

import org.junit.Test;

public class LinkExtractorTest {

    private static final String BASE_URL = "http://localhost:8080/index.html";

    @Test
    public void canExtractLinksFromHtml() {
        assertThat(LinkExtractor.canExtractLinks("text/html"), is(true));
    }

    @Test
    public void canExtractLinksFromHtmlWithCharset() {
        assertThat(LinkExtractor.canExtractLinks("text/html; charset=UTF-8"), is(true));
    }

    @Test
    public void canExtractLinksFromXhtml() {
        assertThat(LinkExtractor.canExtractLinks("application/xhtml+xml"), is(true));
    }

    @Test
    public void canExtractLinksFromXml() {
        assertThat(LinkExtractor.canExtractLinks("text/xml"), is(true));
        assertThat(LinkExtractor.canExtractLinks("application/xml"), is(true));
    }

    @Test
    public void cannotExtractLinksFromJson() {
        assertThat(LinkExtractor.canExtractLinks("application/json"), is(false));
    }

    @Test
    public void cannotExtractLinksFromNull() {
        assertThat(LinkExtractor.canExtractLinks(null), is(false));
    }

    @Test
    public void cannotExtractLinksFromPlainText() {
        assertThat(LinkExtractor.canExtractLinks("text/plain"), is(false));
    }

    @Test
    public void extractAbsoluteInternalLink() {
        String html = "<html><body><a href=\"http://localhost:8080/page1\">Link</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/page1"));
    }

    @Test
    public void extractRelativeLinks() {
        String html = "<html><body><a href=\"/about\">About</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/about"));
    }

    @Test
    public void extractRelativeSiblingLinks() {
        String html = "<html><body><a href=\"page2.html\">Page 2</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/page2.html"));
    }

    @Test
    public void extractMultipleInternalLinks() {
        String html = "<html><body>"
                + "<a href=\"http://localhost:8080/a\">A</a>"
                + "<a href=\"/b\">B</a>"
                + "<a href=\"c.html\">C</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, hasSize(3));
        assertThat(links, containsInAnyOrder(
                                             "http://localhost:8080/a",
                                             "http://localhost:8080/b",
                                             "http://localhost:8080/c.html"));
    }

    @Test
    public void extractLinksWithSingleQuotes() {
        String html = "<html><body><a href='http://localhost:8080/single'>Link</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/single"));
    }

    @Test
    public void ignoresFragmentOnlyLinks() {
        String html = "<html><body><a href=\"#section\">Section</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, is(empty()));
    }

    @Test
    public void stripsFragmentsFromCrossPageLinksInIgnoreMode() {
        String html = "<html><body>"
                + "<a href=\"http://localhost:8080/foo.html#section-a\">A</a>"
                + "<a href=\"http://localhost:8080/foo.html#section-b\">B</a>"
                + "<a href=\"http://localhost:8080/foo.html#section-c\">C</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/foo.html"));
    }

    @Test
    public void preservesFragmentsOnCrossPageLinksInPreserveMode() {
        String html = "<html><body>"
                + "<a href=\"http://localhost:8080/foo.html#section-a\">A</a>"
                + "<a href=\"http://localhost:8080/foo.html#section-b\">B</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL, null, HashRouteMode.PRESERVE);
        assertThat(links, containsInAnyOrder(
                                             "http://localhost:8080/foo.html#section-a",
                                             "http://localhost:8080/foo.html#section-b"));
    }

    @Test
    public void stripsFragmentsFromSitemapLocsInIgnoreMode() {
        String sitemap = "<?xml version=\"1.0\"?><urlset>"
                + "<url><loc>http://example.com/foo.html#a</loc></url>"
                + "<url><loc>http://example.com/foo.html#b</loc></url>"
                + "</urlset>";
        List<String> links = LinkExtractor.extractLinks(sitemap, "http://example.com/sitemap.xml");
        assertThat(links, contains("http://example.com/foo.html"));
    }

    @Test
    public void ignoresMailtoLinks() {
        String html = "<html><body><a href=\"mailto:test@example.com\">Email</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, is(empty()));
    }

    @Test
    public void ignoresJavascriptLinks() {
        String html = "<html><body><a href=\"javascript:void(0)\">Click</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, is(empty()));
    }

    @Test
    public void returnsEmptyForNullContent() {
        assertThat(LinkExtractor.extractLinks(null, BASE_URL), is(empty()));
    }

    @Test
    public void returnsEmptyForEmptyContent() {
        assertThat(LinkExtractor.extractLinks("", BASE_URL), is(empty()));
    }

    @Test
    public void returnsEmptyForNoLinks() {
        String html = "<html><body><p>No links here</p></body></html>";
        assertThat(LinkExtractor.extractLinks(html, BASE_URL), is(empty()));
    }

    @Test
    public void extractsLinksFromSitemapLocTags() {
        String sitemap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                + "  <url><loc>http://example.com/page1</loc></url>\n"
                + "  <url><loc>http://example.com/page2</loc></url>\n"
                + "  <url><loc>http://example.com/page3</loc></url>\n"
                + "</urlset>";
        List<String> links = LinkExtractor.extractLinks(sitemap, "http://example.com/sitemap.xml");
        assertThat(links, hasSize(3));
        assertThat(links, containsInAnyOrder(
                                             "http://example.com/page1",
                                             "http://example.com/page2",
                                             "http://example.com/page3"));
    }

    @Test
    public void extractsLinksFromSitemapIndexLocTags() {
        String sitemapIndex = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                + "  <sitemap><loc>http://example.com/sitemap1.xml</loc></sitemap>\n"
                + "  <sitemap><loc>http://example.com/sitemap2.xml</loc></sitemap>\n"
                + "</sitemapindex>";
        List<String> links = LinkExtractor.extractLinks(sitemapIndex, "http://example.com/sitemap.xml");
        assertThat(links, hasSize(2));
        assertThat(links, containsInAnyOrder(
                                             "http://example.com/sitemap1.xml",
                                             "http://example.com/sitemap2.xml"));
    }

    @Test
    public void deduplicatesLinksAcrossHrefAndLoc() {
        String content = "<html><body>"
                + "<a href=\"http://localhost:8080/page1\">Link</a>"
                + "<loc>http://localhost:8080/page1</loc>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(content, BASE_URL);
        assertThat(links, hasSize(1));
        assertThat(links, contains("http://localhost:8080/page1"));
    }

    @Test
    public void caseInsensitiveHrefAttribute() {
        String html = "<html><body><a HREF=\"http://localhost:8080/upper\">Link</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/upper"));
    }

    @Test
    public void handlesWhitespaceInLocTags() {
        String sitemap = "<?xml version=\"1.0\"?>"
                + "<urlset><url><loc>\n  http://example.com/spaced  \n</loc></url></urlset>";
        List<String> links = LinkExtractor.extractLinks(sitemap, "http://example.com/sitemap.xml");
        assertThat(links, contains("http://example.com/spaced"));
    }

    @Test
    public void handlesHttpsInternalLinks() {
        String html = "<html><body><a href=\"https://secure.example.com/page\">L</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, "https://secure.example.com/index.html");
        assertThat(links, contains("https://secure.example.com/page"));
    }

    @Test
    public void ignoresFtpLinks() {
        String html = "<a href=\"ftp://files.example.com/data.zip\">";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, is(empty()));
    }

    @Test
    public void hashRouteHrefsSkippedByJsoupResolution() {
        String html = "<a href=\"#/users\">Users</a><a href=\"#!/products\">Products</a>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, is(empty()));
    }

    // -----------------------------------------------------------------
    // Classification tests — internal / external / reference / document
    // -----------------------------------------------------------------

    @Test
    public void filtersExternalLinks() {
        String html = "<html><body>"
                + "<a href=\"http://localhost:8080/internal\">Internal</a>"
                + "<a href=\"http://example.com/external\">External</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/internal"));
    }

    @Test
    public void filtersReferenceLinks() {
        String html = "<html><body>"
                + "<a href=\"http://localhost:8080/index.html#section-a\">Section A</a>"
                + "<a href=\"/other\">Other</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/other"));
    }

    @Test
    public void filtersDocumentUrls() {
        String html = "<html><body>"
                + "<a href=\"/report.pdf\">PDF</a>"
                + "<a href=\"/data.xlsx\">XLSX</a>"
                + "<a href=\"/archive.zip\">ZIP</a>"
                + "<a href=\"/page\">Page</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/page"));
    }

    @Test
    public void iframeUrlsNotReturnedForCrawlFollow() {
        String html = "<html><body>"
                + "<iframe src=\"/embedded\"></iframe>"
                + "<a href=\"/page\">Page</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/page"));
    }

    @Test
    public void imageUrlsNotReturnedForCrawlFollow() {
        String html = "<html><body>"
                + "<img src=\"/logo.png\"/>"
                + "<a href=\"/page\">Page</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/page"));
    }

    @Test
    public void respectsBaseHrefTag() {
        String html = "<html><head><base href=\"http://localhost:8080/app/\"/></head><body>"
                + "<a href=\"page.html\">Page</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL);
        assertThat(links, contains("http://localhost:8080/app/page.html"));
    }

    @Test
    public void isDocumentUrlDetectsKnownExtensions() {
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/report.pdf"), is(true));
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/doc.docx"), is(true));
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/data.xlsx?v=1"), is(true));
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/archive.zip#top"), is(true));
    }

    @Test
    public void isDocumentUrlRejectsNonDocuments() {
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/page.html"), is(false));
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/page"), is(false));
        assertThat(LinkExtractor.isDocumentUrl("https://x.y/image.png"), is(false));
        assertThat(LinkExtractor.isDocumentUrl(null), is(false));
        assertThat(LinkExtractor.isDocumentUrl(""), is(false));
    }

    @Test
    public void isExternalLinkBasedOnHost() {
        assertThat(LinkExtractor.isExternalLink("http://localhost:8080/a", "http://localhost:8080/b"), is(false));
        assertThat(LinkExtractor.isExternalLink("http://localhost:8080/a", "http://example.com/b"), is(true));
    }

    @Test
    public void isReferenceLinkDetectsSamePageAnchor() {
        assertThat(LinkExtractor.isReferenceLink(
                                                 "http://localhost:8080/index.html",
                                                 "http://localhost:8080/index.html#anchor"),
                   is(true));
        assertThat(LinkExtractor.isReferenceLink(
                                                 "http://localhost:8080/index.html",
                                                 "http://localhost:8080/other.html#anchor"),
                   is(false));
        assertThat(LinkExtractor.isReferenceLink(
                                                 "http://localhost:8080/index.html",
                                                 "http://localhost:8080/index.html"),
                   is(false));
    }

    @Test
    public void hashRoutePathModeRewritesToPath() {
        String html = "<html><body><a href=\"#/users\">Users</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL, null, HashRouteMode.PATH);
        assertThat(links, contains("http://localhost:8080/users"));
    }

    @Test
    public void hashRoutePreserveModeKeepsHash() {
        String html = "<html><body><a href=\"#/users\">Users</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL, null, HashRouteMode.PRESERVE);
        assertThat(links, contains("http://localhost:8080/#/users"));
    }

    @Test
    public void hashbangPathModeStripsBang() {
        String html = "<html><body><a href=\"#!/products\">Products</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL, null, HashRouteMode.PATH);
        assertThat(links, contains("http://localhost:8080/products"));
    }

    @Test
    public void hashRouteSkippedIfNotPathLike() {
        String html = "<html><body><a href=\"#section\">Section</a></body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL, null, HashRouteMode.PATH);
        assertThat(links, is(empty()));
    }

    @Test
    public void mixedHashAndAbsoluteLinks() {
        String html = "<html><body>"
                + "<a href=\"#/users\">Users</a>"
                + "<a href=\"/about\">About</a>"
                + "</body></html>";
        List<String> links = LinkExtractor.extractLinks(html, BASE_URL, null, HashRouteMode.PATH);
        assertThat(links, containsInAnyOrder(
                                             "http://localhost:8080/users",
                                             "http://localhost:8080/about"));
    }
}
