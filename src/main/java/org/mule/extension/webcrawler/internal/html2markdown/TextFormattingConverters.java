package org.mule.extension.webcrawler.internal.html2markdown;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.function.BiFunction;

class HeadingConverter implements ElementConverter {
    private final String prefix;

    public HeadingConverter(String prefix) {
        this.prefix = prefix;
    }

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
        return prefix + content.toString() + "\n\n";
    }
}

class ParagraphConverter implements ElementConverter {
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
        return content.toString() + "\n\n";
    }
}

class BoldConverter implements ElementConverter {
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
        return "**" + content.toString() + "**";
    }
}

class EmphasisConverter implements ElementConverter {
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
        return "*" + content.toString() + "*";
    }
}

class BreakConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        return "\n";
    }
}

class HorizontalRuleConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        return "---\n";
    }
}