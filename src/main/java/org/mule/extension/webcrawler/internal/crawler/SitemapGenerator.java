package org.mule.extension.webcrawler.internal.crawler;

/**
 * Renders a {@link SiteNode} tree as a sitemaps.org 0.9 XML document. Pure formatter — no I/O, no state. Lifted verbatim from the
 * deleted {@code Crawler.SitemapGenerator} inner class so {@code get-sitemap}'s output stays byte-identical.
 */
public final class SitemapGenerator {

    private SitemapGenerator() {}

    public static String generateSitemapXml(SiteNode rootNode) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, rootNode);

        xml.append("</urlset>");
        return xml.toString();
    }

    private static void appendUrl(StringBuilder xml, SiteNode node) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(node.getUrl())).append("</loc>\n");
        xml.append("    <priority>").append(calculatePriority(node.getCurrentDepth())).append("</priority>\n");
        xml.append("  </url>\n");

        for (SiteNode child : node.getChildren()) {
            appendUrl(xml, child);
        }
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String calculatePriority(int depth) {
        double priority = Math.max(0.0, Math.min(1.0, 1.0 - (depth * 0.1)));
        return String.format("%.1f", priority);
    }
}
