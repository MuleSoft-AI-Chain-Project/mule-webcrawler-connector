package org.mule.extension.webcrawler.internal.html2markdown;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.function.BiFunction;

class UnorderedListConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        StringBuilder markdown = new StringBuilder();
        for (Element li : element.select("> li")) { // Only direct children
            markdown.append("* ");
            for (Node node : li.childNodes()) {
                if (node instanceof TextNode) {
                    markdown.append(((TextNode) node).text());
                } else if (node instanceof Element) {
                    markdown.append(childConverter.apply((Element) node, depth + 1));
                }
            }
            markdown.append("\n");
        }
        markdown.append("\n");
        return markdown.toString();
    }
}

class OrderedListConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        StringBuilder markdown = new StringBuilder();
        int counter = 1;
        for (Element li : element.select("> li")) { // Only direct children
            markdown.append(counter++).append(". ");
            for (Node node : li.childNodes()) {
                if (node instanceof TextNode) {
                    markdown.append(((TextNode) node).text());
                } else if (node instanceof Element) {
                    markdown.append(childConverter.apply((Element) node, depth + 1));
                }
            }
            markdown.append("\n");
        }
        markdown.append("\n");
        return markdown.toString();
    }
}

class ListItemConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        StringBuilder content = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                content.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                content.append(childConverter.apply((Element) node, depth + 1));
            }
        }
        return content.toString(); // List item content is handled by parent list
    }
}