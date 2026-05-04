package org.mule.extension.webcrawler.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;

class URLUtilsTest {

    // ---- isDocumentUrl ----

    @Test
    void isDocumentUrlNullReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl(null));
    }

    @Test
    void isDocumentUrlEmptyReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl(""));
    }

    @Test
    void isDocumentUrlNoExtensionReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl("https://example.com/about"));
    }

    @Test
    void isDocumentUrlTrailingDotReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl("https://example.com/file."));
    }

    @Test
    void isDocumentUrlExtensionTooLongReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl("https://example.com/file.longext"));
    }

    @Test
    void isDocumentUrlInvalidCharsInExtensionReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl("https://example.com/file.p@f"));
    }

    @Test
    void isDocumentUrlSupportedExtensionReturnsTrue() {
        assertTrue(URLUtils.isDocumentUrl("https://example.com/report.pdf"));
        assertTrue(URLUtils.isDocumentUrl("https://example.com/report.docx"));
        assertTrue(URLUtils.isDocumentUrl("https://example.com/data.xlsx"));
    }

    @Test
    void isDocumentUrlUnsupportedExtensionReturnsFalse() {
        // .html is not in DocumentExtension enum.
        assertFalse(URLUtils.isDocumentUrl("https://example.com/page.html"));
    }

    @Test
    void isDocumentUrlStripsQueryAndFragment() {
        assertTrue(URLUtils.isDocumentUrl("https://example.com/report.pdf?x=1"));
        assertTrue(URLUtils.isDocumentUrl("https://example.com/report.pdf#section"));
    }

    @Test
    void isDocumentUrlTooShortReturnsFalse() {
        assertFalse(URLUtils.isDocumentUrl("a"));
    }

    // ---- isReferenceLink ----

    @Test
    void isReferenceLinkSamePageWithFragmentReturnsTrue() {
        assertTrue(URLUtils.isReferenceLink(
                "https://docs.example.com/page",
                "https://docs.example.com/page#section"));
    }

    @Test
    void isReferenceLinkDifferentPathReturnsFalse() {
        assertFalse(URLUtils.isReferenceLink(
                "https://docs.example.com/page",
                "https://docs.example.com/other#section"));
    }

    @Test
    void isReferenceLinkNoFragmentReturnsFalse() {
        assertFalse(URLUtils.isReferenceLink(
                "https://docs.example.com/page",
                "https://docs.example.com/page"));
    }

    @Test
    void isReferenceLinkInvalidUrlReturnsFalse() {
        assertFalse(URLUtils.isReferenceLink("not a url", "also not a url"));
    }

    // ---- isExternalLink ----

    @Test
    void isExternalLinkSameDomainReturnsFalse() throws MalformedURLException {
        assertFalse(URLUtils.isExternalLink(
                "https://example.com/page",
                "https://example.com/other"));
    }

    @Test
    void isExternalLinkDifferentDomainReturnsTrue() throws MalformedURLException {
        assertTrue(URLUtils.isExternalLink(
                "https://example.com/page",
                "https://other.com/page"));
    }

    @Test
    void isExternalLinkBadBaseUrlThrows() {
        assertThrows(MalformedURLException.class,
                () -> URLUtils.isExternalLink("not-a-url", "https://example.com"));
    }

    // ---- extractAndDecodeUrl ----

    @Test
    void extractAndDecodeUrlNoQueryReturnsInput() throws Exception {
        String url = "https://example.com/path";
        assertEquals(url, URLUtils.extractAndDecodeUrl(url));
    }

    @Test
    void extractAndDecodeUrlWithUrlParamReturnsDecodedInner() throws Exception {
        String result = URLUtils.extractAndDecodeUrl(
                "https://example.com/image?url=%2F_next%2Fstatic%2Fmedia%2Fcard.png&w=3840");
        assertEquals("/_next/static/media/card.png", result);
    }

    @Test
    void extractAndDecodeUrlNoUrlParamReturnsInput() throws Exception {
        String url = "https://example.com/image?w=1024&q=75";
        assertEquals(url, URLUtils.extractAndDecodeUrl(url));
    }

    // ---- extractFileNameFromUrl ----

    @Test
    void extractFileNameFromUrlWithExtension() {
        assertEquals("report.pdf",
                URLUtils.extractFileNameFromUrl("https://example.com/docs/report.pdf"));
    }

    @Test
    void extractFileNameFromUrlWithQueryParam() {
        assertEquals("image.jpg",
                URLUtils.extractFileNameFromUrl("https://example.com/image.jpg?w=1024"));
    }

    @Test
    void extractFileNameFromUrlWithoutExtensionAppendsJpg() {
        assertEquals("imagefile.jpg",
                URLUtils.extractFileNameFromUrl("https://example.com/images/imagefile"));
    }

    // ---- detectMimeTypeFromFileName ----

    @Test
    void detectMimeTypeFromFileNameKnownExtension() {
        assertEquals("image/jpeg", URLUtils.detectMimeTypeFromFileName("photo.jpg"));
        assertEquals("image/png", URLUtils.detectMimeTypeFromFileName("diagram.png"));
        assertEquals("application/pdf", URLUtils.detectMimeTypeFromFileName("book.PDF"));
    }

    @Test
    void detectMimeTypeFromFileNameUnknownExtensionReturnsOctetStream() {
        assertEquals("application/octet-stream",
                URLUtils.detectMimeTypeFromFileName("weird.xyz"));
    }

    @Test
    void detectMimeTypeFromFileNameNoExtensionReturnsOctetStream() {
        assertEquals("application/octet-stream",
                URLUtils.detectMimeTypeFromFileName("filenoext"));
    }

    // ---- cleanURL ----

    @Test
    void cleanURLStripsFragment() {
        String clean = URLUtils.cleanURL("https://example.com/page#fragment");
        assertEquals("https://example.com/page", clean);
    }

    @Test
    void cleanURLNormalizesMultipleHashes() {
        String clean = URLUtils.cleanURL("https://example.com/page##fragment");
        assertEquals("https://example.com/page", clean);
    }

    @Test
    void cleanURLPreservesQueryAndPath() {
        String clean = URLUtils.cleanURL("https://example.com/path?a=1&b=2#frag");
        assertTrue(clean.startsWith("https://example.com/path"));
        assertTrue(clean.contains("a=1"));
        assertFalse(clean.contains("#frag"));
    }

    @Test
    void cleanURLHandlesSpecialCharsFallback() {
        // A URL with a space — the initial URI parse will likely fail and fall through
        // to the encoded-component path.
        String clean = URLUtils.cleanURL("https://example.com/path with space/file");
        assertTrue(clean.startsWith("https://example.com/"),
                "cleanURL should survive spaces via the encoding fallback; got: " + clean);
    }

    @Test
    void cleanURLHopelesslyInvalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> URLUtils.cleanURL("ht!tp://[bad"));
    }
}
