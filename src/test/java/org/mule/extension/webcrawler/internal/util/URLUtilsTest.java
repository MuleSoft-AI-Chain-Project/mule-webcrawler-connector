package org.mule.extension.webcrawler.internal.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.extension.webcrawler.internal.service.LinkExtractor;

import org.junit.Test;

public class URLUtilsTest {

    // -----------------------------------------------------------------
    // isDocumentUrl
    // -----------------------------------------------------------------

    @Test
    public void detectsKnownDocumentExtensions() {
        assertThat(URLUtils.isDocumentUrl("https://x.y/report.pdf"), is(true));
        assertThat(URLUtils.isDocumentUrl("https://x.y/sheet.xlsx"), is(true));
        assertThat(URLUtils.isDocumentUrl("https://x.y/slides.pptx"), is(true));
        assertThat(URLUtils.isDocumentUrl("https://x.y/archive.zip"), is(true));
        assertThat(URLUtils.isDocumentUrl("https://x.y/archive.rar"), is(true));
    }

    @Test
    public void rejectsNonDocumentExtensions() {
        assertThat(URLUtils.isDocumentUrl("https://x.y/page.html"), is(false));
        assertThat(URLUtils.isDocumentUrl("https://x.y/api"), is(false));
        assertThat(URLUtils.isDocumentUrl("https://x.y/image.png"), is(false));
    }

    @Test
    public void detectsExtensionWithQueryString() {
        assertThat(URLUtils.isDocumentUrl("https://x.y/data.xlsx?v=1"), is(true));
    }

    @Test
    public void detectsExtensionWithFragment() {
        assertThat(URLUtils.isDocumentUrl("https://x.y/archive.zip#anchor"), is(true));
    }

    @Test
    public void caseInsensitiveExtension() {
        assertThat(URLUtils.isDocumentUrl("https://x.y/REPORT.PDF"), is(true));
    }

    @Test
    public void handlesNullAndEmpty() {
        assertThat(URLUtils.isDocumentUrl(null), is(false));
        assertThat(URLUtils.isDocumentUrl(""), is(false));
    }

    // -----------------------------------------------------------------
    // External / reference classification — moved to LinkExtractor.
    //
    // URLUtils used to host buggy substring-based isExternalLink/isReferenceLink helpers
    // ('evil-example.com'.contains('example.com') == true). They were deleted in favor of
    // LinkExtractor's URI-host-equality versions; tests below pin the corrected contract.
    // -----------------------------------------------------------------

    @Test
    public void sameHostIsNotExternal() {
        assertThat(LinkExtractor.isExternalLink("http://localhost:8080/a", "http://localhost:8080/b"), is(false));
    }

    @Test
    public void differentHostIsExternal() {
        assertThat(LinkExtractor.isExternalLink("http://localhost:8080/a", "http://example.com/b"), is(true));
    }

    @Test
    public void hostSubstringIsNotConfusedAsInternal() {
        // The bug we deleted: 'evil-example.com' contains 'example.com' as a substring.
        // LinkExtractor compares full host strings via URI.getHost() — must classify as external.
        assertThat(LinkExtractor.isExternalLink("https://example.com/a", "https://evil-example.com/b"),
                   is(true));
    }

    @Test
    public void caseInsensitiveHostMatch() {
        assertThat(LinkExtractor.isExternalLink("https://Example.com/a", "https://example.COM/b"),
                   is(false));
    }

    @Test
    public void samePageWithFragmentIsReference() {
        assertThat(LinkExtractor.isReferenceLink(
                                                 "http://localhost:8080/index.html",
                                                 "http://localhost:8080/index.html#section"),
                   is(true));
    }

    @Test
    public void differentPageWithFragmentIsNotReference() {
        assertThat(LinkExtractor.isReferenceLink(
                                                 "http://localhost:8080/index.html",
                                                 "http://localhost:8080/other.html#section"),
                   is(false));
    }

    @Test
    public void samePageNoFragmentIsNotReference() {
        assertThat(LinkExtractor.isReferenceLink(
                                                 "http://localhost:8080/index.html",
                                                 "http://localhost:8080/index.html"),
                   is(false));
    }

    // -----------------------------------------------------------------
    // cleanURL
    // -----------------------------------------------------------------

    @Test
    public void cleanURLStripsFragment() {
        assertThat(URLUtils.cleanURL("https://example.com/page#section"), is("https://example.com/page"));
    }

    @Test
    public void cleanURLNormalizesDoubleHash() {
        String cleaned = URLUtils.cleanURL("https://example.com/page##fragment");
        // Fragment is then dropped — we just want it not to crash.
        assertThat(cleaned, is("https://example.com/page"));
    }

    @Test
    public void cleanURLPreservesQueryString() {
        assertThat(URLUtils.cleanURL("https://example.com/search?q=foo#anchor"),
                   is("https://example.com/search?q=foo"));
    }

    // -----------------------------------------------------------------
    // detectMimeTypeFromFileName
    // -----------------------------------------------------------------

    @Test
    public void detectMimeFromCommonExtensions() {
        String pdf = URLUtils.detectMimeTypeFromFileName("report.pdf");
        String html = URLUtils.detectMimeTypeFromFileName("index.html");
        // We don't assert the exact mime returned (jdk-dependent in some places); just ensure not null.
        assertThat(pdf != null, is(true));
        assertThat(html != null, is(true));
    }
}
