package org.mule.extension.webcrawler.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UtilsTest {

    // ---- countWords ----

    @Test
    void countWordsNullReturnsZero() {
        assertEquals(0, Utils.countWords(null));
    }

    @Test
    void countWordsEmptyReturnsZero() {
        assertEquals(0, Utils.countWords(""));
    }

    @Test
    void countWordsWhitespaceOnlyReturnsZero() {
        assertEquals(0, Utils.countWords("   \t\n  "));
    }

    @Test
    void countWordsSimple() {
        assertEquals(3, Utils.countWords("the quick fox"));
    }

    @Test
    void countWordsCollapsesMultipleSpaces() {
        assertEquals(3, Utils.countWords("the   quick\tfox"));
    }

    @Test
    void countWordsTrimsLeadingTrailing() {
        assertEquals(2, Utils.countWords("  hello world  "));
    }

    // ---- getSanitizedFilename ----

    @Test
    void getSanitizedFilenameReplacesInvalidCharsWithUnderscore() {
        String out = Utils.getSanitizedFilename("a/b\\c:d*e?f\"g<h>i|j");
        assertFalse(out.contains("/"));
        assertFalse(out.contains("\\"));
        assertFalse(out.contains(":"));
        assertFalse(out.contains("*"));
        assertFalse(out.contains("?"));
        assertFalse(out.contains("\""));
        assertFalse(out.contains("<"));
        assertFalse(out.contains(">"));
        assertFalse(out.contains("|"));
        assertTrue(out.contains("_"));
    }

    @Test
    void getSanitizedFilenameStripsSpaces() {
        assertEquals("mytitlewithspaces",
                Utils.getSanitizedFilename("my title with spaces"));
    }

    @Test
    void getSanitizedFilenamePreservesValidChars() {
        assertEquals("report-2026_01.txt",
                Utils.getSanitizedFilename("report-2026_01.txt"));
    }

    // ---- addDelay ----

    @Test
    void addDelayZeroOrNegativeIsNoOp() {
        long start = System.currentTimeMillis();
        Utils.addDelay(0);
        Utils.addDelay(-100);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 250,
                "addDelay(0) and addDelay(-100) should return fast; took " + elapsed + "ms");
    }

    @Test
    void addDelayPositiveSleepsApproximatelyThatLong() {
        long start = System.currentTimeMillis();
        Utils.addDelay(50);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 40,
                "addDelay(50) should sleep ~50ms; actual elapsed " + elapsed + "ms");
    }

    // ---- convertHtmlToMarkdown ----

    @Test
    void convertHtmlToMarkdownH1ProducesMarkdownHeading() {
        String md = Utils.convertHtmlToMarkdown("<h1>Hello</h1>");
        assertTrue(md.contains("# Hello"), "Expected '# Hello' in output; got: " + md);
    }

    @Test
    void convertHtmlToMarkdownParagraph() {
        String md = Utils.convertHtmlToMarkdown("<p>Some text.</p>");
        assertTrue(md.contains("Some text."));
    }
}
