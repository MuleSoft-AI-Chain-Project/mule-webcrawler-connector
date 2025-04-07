package org.mule.extension.webcrawler.internal.html2markdown;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class HtmlToMarkdownConverter {

    private final ElementConverterRegistry converterRegistry;
    private final int maxDepth;

    public HtmlToMarkdownConverter(int maxDepth) {
        this.converterRegistry = new ElementConverterRegistry();
        this.maxDepth = maxDepth;
    }

    public String convert(String html) {
        Document document = Jsoup.parse(html);
        StringBuilder markdown = new StringBuilder();
        for (Element element : document.body().children()) {
            markdown.append(convertElement(element, 1));
        }
        return markdown.toString();
    }

    private String convertElement(Element element, int depth) {
        if (depth > maxDepth) {
            return ""; // Stop processing if depth exceeds the limit
        }

        ElementConverter converter = converterRegistry.getConverter(element.tagName());
        StringBuilder markdown = new StringBuilder();

        if (converter != null) {
            markdown.append(converter.convert(element, this::convertElement, depth));
        } else {
            // Default behavior: process children and append text
            for (Node node : element.childNodes()) {
                if (node instanceof TextNode) {
                    markdown.append(((TextNode) node).text());
                } else if (node instanceof Element) {
                    markdown.append(convertElement((Element) node, depth + 1));
                }
            }
        }
        return markdown.toString();
    }

    public static void main(String[] args) {
        String html = "<div><h1>Hello</h1><p>This is a <strong><span>nested</span> paragraph</strong>.</p><ul><li>Item 1<ul><li>Sub-item</li></ul></li><li>Item 2</li></ul></div>";
        HtmlToMarkdownConverter converter = new HtmlToMarkdownConverter(3); // Set max depth to 3
        String markdown = converter.convert(html);
        System.out.println(markdown);
    }
}
