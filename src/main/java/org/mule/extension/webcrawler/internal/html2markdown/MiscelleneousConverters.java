package org.mule.extension.webcrawler.internal.html2markdown;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.function.BiFunction;

class DefaultConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        StringBuilder markdown = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                markdown.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                markdown.append(childConverter.apply((Element) node, depth + 1));
            }
        }
        return markdown.toString(); // Just process children by default
    }
}

class TableConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        if (depth > 1) return element.text(); // Basic handling for nested tables

        StringBuilder markdown = new StringBuilder();
        Elements rows = element.select("tr");
        if (rows.isEmpty()) {
            return "";
        }

        // Header row
        Elements headerCells = rows.first().select("th, td");
        if (!headerCells.isEmpty()) {
            for (int i = 0; i < headerCells.size(); i++) {
                StringBuilder cellContent = new StringBuilder();
                for (Node node : headerCells.get(i).childNodes()) {
                    if (node instanceof TextNode) {
                        cellContent.append(((TextNode) node).text());
                    } else if (node instanceof Element) {
                        cellContent.append(childConverter.apply((Element) node, depth + 1));
                    }
                }
                markdown.append(cellContent.toString());
                if (i < headerCells.size() - 1) {
                    markdown.append(" | ");
                }
            }
            markdown.append("\n");
            for (int i = 0; i < headerCells.size(); i++) {
                markdown.append("---");
                if (i < headerCells.size() - 1) {
                    markdown.append(" | ");
                }
            }
            markdown.append("\n");
        }

        // Data rows
        for (int i = (headerCells.isEmpty() ? 0 : 1); i < rows.size(); i++) {
            Elements dataCells = rows.get(i).select("td");
            for (int j = 0; j < dataCells.size(); j++) {
                StringBuilder cellContent = new StringBuilder();
                for (Node node : dataCells.get(j).childNodes()) {
                    if (node instanceof TextNode) {
                        cellContent.append(((TextNode) node).text());
                    } else if (node instanceof Element) {
                        cellContent.append(childConverter.apply((Element) node, depth + 1));
                    }
                }
                markdown.append(cellContent.toString());
                if (j < dataCells.size() - 1) {
                    markdown.append(" | ");
                }
            }
            markdown.append("\n");
        }
        markdown.append("\n");
        return markdown.toString();
    }
}

class BlockquoteConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        StringBuilder markdown = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                String[] lines = ((TextNode) node).text().split("\\n");
                for (String line : lines) {
                    markdown.append("> ").append(line.trim()).append("\n");
                }
            } else if (node instanceof Element) {
                String childMarkdown = childConverter.apply((Element) node, depth + 1);
                String[] lines = childMarkdown.split("\\n");
                for (String line : lines) {
                    markdown.append("> ").append(line).append("\n");
                }
            }
        }
        markdown.append("\n");
        return markdown.toString();
    }
}

class LinkConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        String href = element.attr("href");
        StringBuilder text = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                text.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                text.append(childConverter.apply((Element) node, depth + 1));
            }
        }
        if (href.isEmpty()) {
            return text.toString();
        }
        return "[" + text.toString() + "](" + href + ")";
    }
}

class ImageConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        String src = element.attr("src");
        String alt = element.attr("alt");
        if (src.isEmpty()) {
            return "";
        }
        return "![" + alt + "](" + src + ")";
    }
}

class CodeBlockConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        String language = "";
        if (element.hasClass("language-") && element.className().contains("language-")) {
            language = element.className().substring(element.className().indexOf("language-") + "language-".length()).split("\\s+")[0];
        } else {
            Element codeElement = element.selectFirst("code[data-language]");
            if (codeElement != null) {
                language = codeElement.attr("data-language");
            }
        }

        StringBuilder content = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                content.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                // Do not process nested elements within code blocks as markdown
                content.append(((Element) node).text());
            }
        }

        return "```" + language + "\n" + content.toString() + "\n```\n\n";
    }
}

class SpanConverter implements ElementConverter {
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
        return content.toString(); // By default, just render the content inline
    }
}

class DivConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        StringBuilder markdown = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                markdown.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                markdown.append(childConverter.apply((Element) node, depth + 1));
            }
        }
        return markdown.toString() + "\n\n"; // Treat div as a block-level element with spacing
    }
}

class CodeConverter implements ElementConverter {
    @Override
    public String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth) {
        return "`" + element.text() + "`"; // Inline code is wrapped in single backticks
    }
}