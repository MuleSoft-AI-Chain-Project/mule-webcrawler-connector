package org.mule.extension.webcrawler.internal.html2markdown;

import org.jsoup.nodes.Element;

import java.util.function.BiFunction;

public interface ElementConverter {
    String convert(Element element, BiFunction<Element, Integer, String> childConverter, int depth);
}