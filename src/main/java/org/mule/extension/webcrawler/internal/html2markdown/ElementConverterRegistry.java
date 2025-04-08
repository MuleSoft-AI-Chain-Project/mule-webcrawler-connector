package org.mule.extension.webcrawler.internal.html2markdown;

import java.util.HashMap;
import java.util.Map;

public class ElementConverterRegistry {

    private final Map<String, ElementConverter> converters = new HashMap<>();
    private final ElementConverter defaultConverter = new DefaultConverter();

    public ElementConverterRegistry() {
        registerConverter("h1", new HeadingConverter("# "));
        registerConverter("h2", new HeadingConverter("## "));
        registerConverter("h3", new HeadingConverter("### "));
        registerConverter("h4", new HeadingConverter("#### "));
        registerConverter("h5", new HeadingConverter("##### "));
        registerConverter("h6", new HeadingConverter("###### "));
        registerConverter("p", new ParagraphConverter());
        registerConverter("strong", new BoldConverter());
        registerConverter("b", new BoldConverter());
        registerConverter("em", new EmphasisConverter());
        registerConverter("i", new EmphasisConverter());
        registerConverter("ul", new UnorderedListConverter());
        registerConverter("ol", new OrderedListConverter());
        registerConverter("li", new ListItemConverter());
        registerConverter("br", new BreakConverter());
        registerConverter("hr", new HorizontalRuleConverter());
        registerConverter("table", new TableConverter());
        registerConverter("blockquote", new BlockquoteConverter());
        registerConverter("a", new LinkConverter());
        registerConverter("img", new ImageConverter());
        registerConverter("pre", new CodeBlockConverter());
        registerConverter("span", new SpanConverter());
        registerConverter("div", new DivConverter());
        registerConverter("code", new CodeConverter()); // For inline <code>
    }

    public void registerConverter(String tagName, ElementConverter converter) {
        converters.put(tagName, converter);
    }

    public ElementConverter getConverter(String tagName) {
        return converters.getOrDefault(tagName, defaultConverter);
    }
}
