package org.mule.extension.webcrawler.internal.service;

import org.mule.extension.webcrawler.api.HashRouteMode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 * Extracts candidate crawl URLs from an HTTP response body.
 *
 * <p>
 * Uses jsoup DOM parsing to select anchor tags and classify each href as internal / external / reference / document. Only
 * internal links are returned for crawl follow.
 * </p>
 *
 * <p>
 * <b>Behavior note on relative URL resolution:</b> relative URLs are resolved via jsoup's {@code element.absUrl(...)}, which
 * respects a document's {@code <base href>} tag when present and falls back to the page URL otherwise. This matches browser
 * semantics and differs from a pure response-URL-based resolver.
 * </p>
 */
public final class LinkExtractor {

    private static final Set<String> PARSEABLE_MIME_TYPES;
    private static final Set<String> XML_MIME_TYPES;
    private static final Set<String> DOCUMENT_EXTENSIONS;
    private static final int MAX_EXTENSION_LENGTH = 5;
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^[a-z0-9]+$");

    static {
        Set<String> types = new HashSet<>();
        types.add("text/html");
        types.add("application/xhtml+xml");
        types.add("text/xml");
        types.add("application/xml");
        PARSEABLE_MIME_TYPES = Collections.unmodifiableSet(types);

        Set<String> xmlTypes = new HashSet<>();
        xmlTypes.add("text/xml");
        xmlTypes.add("application/xml");
        XML_MIME_TYPES = Collections.unmodifiableSet(xmlTypes);

        DOCUMENT_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                                                                                      "pdf", "doc", "docx", "xls", "xlsx", "ppt",
                                                                                      "pptx", "zip", "rar")));
    }

    private LinkExtractor() {}

    public static boolean canExtractLinks(String contentType) {
        return PARSEABLE_MIME_TYPES.contains(normalizeMimeType(contentType));
    }

    public static List<String> extractLinks(String content, String baseUrl) {
        return extractLinks(content, baseUrl, null, HashRouteMode.IGNORE);
    }

    public static List<String> extractLinks(String content, String baseUrl, String contentType) {
        return extractLinks(content, baseUrl, contentType, HashRouteMode.IGNORE);
    }

    /**
     * Extracts crawlable links from the given content. Only <em>internal</em> anchor links are returned (plus sitemap
     * {@code <loc>} entries). External, reference (same-page {@code #anchor}), document (PDF/DOC/etc.), {@code <iframe>} and
     * {@code <img>} URLs are deliberately excluded from the crawl-follow set. SPA hash-route handling is controlled by
     * {@code hashRouteMode}.
     *
     * <p>
     * <b>Fragment handling:</b> in {@link HashRouteMode#IGNORE} (default), fragments are stripped from extracted URLs so that
     * {@code foo.html#section-a} and {@code foo.html#section-b} dedup to the same {@code foo.html}. Fragments are scroll
     * positions on SSR sites, not distinct pages — keeping them produces N near-duplicate fetches per page. Modes {@code PATH}
     * and {@code PRESERVE} keep fragments because the user has opted into SPA hash-route semantics where fragments do identify
     * pages.
     * </p>
     */
    public static List<String> extractLinks(String content, String baseUrl, String contentType, HashRouteMode hashRouteMode) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> seen = new LinkedHashSet<>();
        HashRouteMode mode = hashRouteMode == null ? HashRouteMode.IGNORE : hashRouteMode;
        String origin = extractOrigin(baseUrl);

        Document document = parseDocument(content, baseUrl, contentType);

        for (Element anchor : document.select("a[href]")) {
            String rawHref = anchor.attr("href");
            if (mode != HashRouteMode.IGNORE && origin != null && isHashRoute(rawHref)) {
                String rewritten = rewriteHashRoute(rawHref, origin, mode);
                if (rewritten != null) {
                    seen.add(rewritten);
                }
                continue;
            }

            String absolute = anchor.absUrl("href");

            if (absolute.isEmpty()) {
                continue;
            }
            if (!hasCrawlableScheme(absolute)) {
                continue;
            }
            if (isDocumentUrl(absolute)) {
                continue;
            }
            if (isExternalLink(baseUrl, absolute)) {
                continue;
            }
            if (isReferenceLink(baseUrl, absolute)) {
                continue;
            }
            seen.add(maybeStripFragment(absolute, mode));
        }

        // <iframe> and <img> tags are intentionally NOT enqueued for crawl-follow. They are parsed by jsoup but excluded
        // from the returned set.

        // Sitemap / XML <loc> tags — always parsed, regardless of HTML vs XML.
        for (Element loc : document.select("loc")) {
            String text = loc.text().trim();
            if (text.isEmpty()) {
                text = loc.ownText().trim();
            }
            String resolved = resolveUrl(text, baseUrl);
            if (resolved != null) {
                seen.add(maybeStripFragment(resolved, mode));
            }
        }

        return new ArrayList<>(seen);
    }

    /**
     * Strips the URL fragment in {@link HashRouteMode#IGNORE} mode; returns the URL untouched in {@code PATH} / {@code PRESERVE}
     * modes (where the user has opted into SPA hash-route semantics and fragments are load-bearing).
     */
    static String maybeStripFragment(String url, HashRouteMode mode) {
        if (mode != HashRouteMode.IGNORE || url == null) {
            return url;
        }
        int hash = url.indexOf('#');
        return hash < 0 ? url : url.substring(0, hash);
    }

    private static Document parseDocument(String content, String baseUrl, String contentType) {
        String mime = normalizeMimeType(contentType);
        String safeBase = baseUrl == null ? "" : baseUrl;
        if (XML_MIME_TYPES.contains(mime)) {
            return Jsoup.parse(content, safeBase, Parser.xmlParser());
        }
        // Heuristic: sitemap / XML without a declared content type — detect via the root element.
        String trimmed = content.trim();
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<urlset") || trimmed.startsWith("<sitemapindex")) {
            return Jsoup.parse(content, safeBase, Parser.xmlParser());
        }
        return Jsoup.parse(content, safeBase);
    }

    private static String resolveUrl(String href, String baseUrl) {
        try {
            URI uri = new URI(href);
            if (uri.isAbsolute()) {
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return uri.toString();
                }
                return null;
            }
            if (baseUrl == null) {
                return null;
            }
            return new URI(baseUrl).resolve(uri).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasCrawlableScheme(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalizeMimeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String mime = contentType.contains(";")
                ? contentType.substring(0, contentType.indexOf(';')).trim()
                : contentType.trim();
        return mime.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns true if the given URL points to a supported document type (pdf, doc, xls, etc.) based on its file extension.
     */
    public static boolean isDocumentUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            String cleanUrl = url.split("[?#]")[0].toLowerCase(Locale.ENGLISH);
            if (cleanUrl.length() < 2) {
                return false;
            }
            int lastDotIndex = cleanUrl.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == cleanUrl.length() - 1) {
                return false;
            }
            String fileExtension = cleanUrl.substring(lastDotIndex + 1);
            if (fileExtension.length() > MAX_EXTENSION_LENGTH) {
                return false;
            }
            if (!EXTENSION_PATTERN.matcher(fileExtension).matches()) {
                return false;
            }
            return DOCUMENT_EXTENSIONS.contains(fileExtension);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the link is a reference (same-page {@code #anchor}) on the same URL as {@code baseUrl}.
     */
    public static boolean isReferenceLink(String baseUrl, String linkToCheck) {
        if (baseUrl == null || linkToCheck == null) {
            return false;
        }
        try {
            URI baseUri = URI.create(baseUrl);
            URI linkUri = URI.create(linkToCheck);

            return baseUri.getScheme() != null
                    && baseUri.getScheme().equals(linkUri.getScheme())
                    && baseUri.getHost() != null
                    && baseUri.getHost().equals(linkUri.getHost())
                    && safeEquals(baseUri.getPath(), linkUri.getPath())
                    && linkUri.getFragment() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns true if {@code linkToCheck} belongs to a different host than {@code baseUrl}.
     */
    public static boolean isExternalLink(String baseUrl, String linkToCheck) {
        if (baseUrl == null || linkToCheck == null) {
            return false;
        }
        try {
            String baseHost = new URI(baseUrl).getHost();
            String linkHost = new URI(linkToCheck).getHost();
            if (baseHost == null || baseHost.isEmpty() || linkHost == null || linkHost.isEmpty()) {
                return false;
            }
            return !baseHost.equalsIgnoreCase(linkHost);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static boolean isHashRoute(String href) {
        return href != null && href.startsWith("#") && href.length() > 1;
    }

    private static String rewriteHashRoute(String href, String origin, HashRouteMode mode) {
        String stripped = href.substring(1);
        if (stripped.startsWith("!")) {
            stripped = stripped.substring(1);
        }
        if (!stripped.startsWith("/")) {
            return null;
        }
        if (mode == HashRouteMode.PATH) {
            return origin + stripped;
        }
        if (mode == HashRouteMode.PRESERVE) {
            return origin + "/#" + stripped;
        }
        return null;
    }

    private static String extractOrigin(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            if (port == -1) {
                return uri.getScheme() + "://" + uri.getHost();
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + port;
        } catch (Exception e) {
            return null;
        }
    }
}
