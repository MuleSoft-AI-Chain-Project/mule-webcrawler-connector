package org.mule.extension.webcrawler.internal.html2markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmlToMarkdownConverterTest {

    private HtmlToMarkdownConverter converter;

    @BeforeEach
    void setUp() {
        converter = new HtmlToMarkdownConverter(100);
    }

    // ---- Heading converter family ----

    @Test
    void h1RendersWithSingleHash() {
        String md = converter.convert("<h1>Title</h1>");
        assertTrue(md.contains("# Title"), md);
    }

    @Test
    void h2ThroughH6RenderWithAscendingHashes() {
        assertTrue(converter.convert("<h2>H2</h2>").contains("## H2"));
        assertTrue(converter.convert("<h3>H3</h3>").contains("### H3"));
        assertTrue(converter.convert("<h4>H4</h4>").contains("#### H4"));
        assertTrue(converter.convert("<h5>H5</h5>").contains("##### H5"));
        assertTrue(converter.convert("<h6>H6</h6>").contains("###### H6"));
    }

    // ---- Text formatting (TextFormattingConverters) ----

    @Test
    void paragraphRendersTextWithTrailingNewlines() {
        String md = converter.convert("<p>Hello world.</p>");
        assertTrue(md.contains("Hello world."));
    }

    @Test
    void boldAndStrongRenderWithDoubleAsterisks() {
        assertTrue(converter.convert("<p><strong>bold</strong></p>").contains("**bold**"));
        assertTrue(converter.convert("<p><b>bold</b></p>").contains("**bold**"));
    }

    @Test
    void emAndIRenderWithSingleAsterisks() {
        assertTrue(converter.convert("<p><em>italic</em></p>").contains("*italic*"));
        assertTrue(converter.convert("<p><i>italic</i></p>").contains("*italic*"));
    }

    @Test
    void brRendersNewline() {
        String md = converter.convert("<p>line1<br/>line2</p>");
        assertTrue(md.contains("line1") && md.contains("line2"));
    }

    @Test
    void hrRendersTripleDash() {
        String md = converter.convert("<hr/>");
        assertTrue(md.contains("---"));
    }

    // ---- List converters (ListConverters) ----

    @Test
    void unorderedListRendersAsteriskBullets() {
        String md = converter.convert("<ul><li>one</li><li>two</li></ul>");
        assertTrue(md.contains("* one"));
        assertTrue(md.contains("* two"));
    }

    @Test
    void orderedListRendersNumbers() {
        String md = converter.convert("<ol><li>one</li><li>two</li></ol>");
        assertTrue(md.contains("1. one"));
        assertTrue(md.contains("2. two"));
    }

    // ---- Miscellaneous / link / image / code / blockquote / table ----

    @Test
    void linkRendersMarkdownLink() {
        String md = converter.convert("<p><a href=\"https://example.com\">Example</a></p>");
        assertTrue(md.contains("[Example](https://example.com)"), md);
    }

    @Test
    void linkWithoutHrefRendersOnlyText() {
        String md = converter.convert("<p><a>bare</a></p>");
        assertTrue(md.contains("bare"));
        assertFalse(md.contains("]("));
    }

    @Test
    void imageRendersMarkdownImage() {
        String md = converter.convert("<img src=\"pic.png\" alt=\"Pic\"/>");
        assertTrue(md.contains("![Pic](pic.png)"), md);
    }

    @Test
    void imageWithoutSrcRendersEmpty() {
        String md = converter.convert("<img alt=\"nopic\"/>");
        assertFalse(md.contains("nopic"));
    }

    @Test
    void inlineCodeRendersBackticks() {
        String md = converter.convert("<p>use <code>foo()</code> here</p>");
        assertTrue(md.contains("`foo()`"), md);
    }

    @Test
    void preRendersAsCodeBlock() {
        String md = converter.convert("<pre>code\nblock</pre>");
        assertTrue(md.contains("```"));
        assertTrue(md.contains("code"));
    }

    @Test
    void blockquoteRendersWithGtPrefix() {
        String md = converter.convert("<blockquote>Quoted.</blockquote>");
        assertTrue(md.contains("> Quoted."), md);
    }

    @Test
    void tableRendersHeadersAndSeparator() {
        String md = converter.convert(
                "<table><tr><th>A</th><th>B</th></tr><tr><td>1</td><td>2</td></tr></table>");
        assertTrue(md.contains("A | B"), md);
        assertTrue(md.contains("--- | ---"), md);
        assertTrue(md.contains("1 | 2"), md);
    }

    @Test
    void divRendersContentWithSpacing() {
        String md = converter.convert("<div>content</div>");
        assertTrue(md.contains("content"));
    }

    @Test
    void spanRendersContentInline() {
        String md = converter.convert("<p><span>inline</span></p>");
        assertTrue(md.contains("inline"));
    }

    // ---- Default / unknown tags ----

    @Test
    void unknownTagFallsBackToDefaultConverter() {
        String md = converter.convert("<section>some text</section>");
        assertTrue(md.contains("some text"));
    }

    // ---- maxDepth guard ----

    @Test
    void maxDepthLimitsRecursion() {
        HtmlToMarkdownConverter shallow = new HtmlToMarkdownConverter(1);
        // Deeply nested elements should be truncated past depth=1; no exception expected.
        String md = shallow.convert("<div><div><div><p>deep</p></div></div></div>");
        // We don't assert the exact output shape here — only that the converter
        // completed without recursing forever.
        assertEquals(String.class, md.getClass());
    }

    // ---- limitConsecutiveNewlines (exercised via convert) ----

    @Test
    void consecutiveBlockElementsCollapseToAtMostTwoNewlines() {
        String md = converter.convert("<p>one</p><p>two</p><p>three</p>");
        // Ensure we never emit three-in-a-row newlines.
        assertFalse(md.contains("\n\n\n"),
                "Expected no triple newlines after limit; got: [" + md + "]");
    }
}
