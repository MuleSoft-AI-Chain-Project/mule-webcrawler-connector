package org.mule.extension.webcrawler.internal.helper.page;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.extension.webcrawler.api.RegexUrlsFilterLogic;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * Locks in the canonical regex-filter contract used by both the operations path and the {@code crawl-website-source} Source.
 *
 * <p>
 * The Source had its own {@code regexMatchesSkip} helper with every-pattern-must-match semantics and {@code Matcher.find()} —
 * different decisions from the operations path's {@link PageHelper#skipUrl}, which uses any-match +
 * {@link java.util.regex.Matcher#matches()}. That divergence was a regression vs. master (master shipped only the {@code skipUrl}
 * path). The Source now delegates to {@link PageHelper#skipUrl}, so testing {@code skipUrl} covers the regression contract for
 * both surfaces.
 * </p>
 */
public class PageHelperSkipUrlTest {

    // ---------------------------------------------------------------------
    // Reviewer-flagged regression: INCLUDE with multiple patterns.
    // ---------------------------------------------------------------------

    /**
     * The bug case: INCLUDE + [".*docs.*", ".*api.*"] + a URL that matches only one of the two. Master semantics: keep it (any
     * pattern matched). Old Source semantics: skip it (every pattern must match). Locking in master semantics here.
     */
    @Test
    public void includeAnyMatch_keepsUrlMatchingAtLeastOnePattern() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/docs/foo",
                                          RegexUrlsFilterLogic.INCLUDE,
                                          Arrays.asList(".*docs.*", ".*api.*"));
        assertThat(skip, is(false));
    }

    @Test
    public void includeAnyMatch_skipsUrlMatchingNoPattern() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/blog/foo",
                                          RegexUrlsFilterLogic.INCLUDE,
                                          Arrays.asList(".*docs.*", ".*api.*"));
        assertThat(skip, is(true));
    }

    @Test
    public void excludeAnyMatch_skipsUrlMatchingAtLeastOnePattern() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/admin/foo",
                                          RegexUrlsFilterLogic.EXCLUDE,
                                          Arrays.asList(".*admin.*", ".*internal.*"));
        assertThat(skip, is(true));
    }

    @Test
    public void excludeAnyMatch_keepsUrlMatchingNoPattern() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/public/foo",
                                          RegexUrlsFilterLogic.EXCLUDE,
                                          Arrays.asList(".*admin.*", ".*internal.*"));
        assertThat(skip, is(false));
    }

    // ---------------------------------------------------------------------
    // Pattern matching is full-string-anchored (Matcher.matches), not substring (Matcher.find).
    // ---------------------------------------------------------------------

    /**
     * A pattern without anchors and without leading/trailing wildcards must not partial-match against the URL — the Source's old
     * {@code find()}-based helper would have accepted this, but {@code matches()} requires the entire URL to match the pattern.
     */
    @Test
    public void includeAnchored_rejectsPartialPatternMatch() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/docs/foo",
                                          RegexUrlsFilterLogic.INCLUDE,
                                          Collections.singletonList("docs"));
        assertThat(skip, is(true));
    }

    @Test
    public void includeAnchored_acceptsFullStringPattern() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/docs/foo",
                                          RegexUrlsFilterLogic.INCLUDE,
                                          Collections.singletonList(".*docs.*"));
        assertThat(skip, is(false));
    }

    // ---------------------------------------------------------------------
    // Null / empty short-circuits — keep all URLs (no filter configured).
    // ---------------------------------------------------------------------

    @Test
    public void nullFilterLogic_keepsAllUrls() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/anything",
                                          null,
                                          Arrays.asList(".*docs.*"));
        assertThat(skip, is(false));
    }

    @Test
    public void emptyRegexList_keepsAllUrls() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/anything",
                                          RegexUrlsFilterLogic.INCLUDE,
                                          Collections.emptyList());
        assertThat(skip, is(false));
    }

    @Test
    public void nullRegexList_keepsAllUrls() {
        boolean skip = PageHelper.skipUrl(
                                          "https://example.com/anything",
                                          RegexUrlsFilterLogic.INCLUDE,
                                          null);
        assertThat(skip, is(false));
    }
}
