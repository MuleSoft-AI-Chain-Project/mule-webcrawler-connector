package org.mule.extension.webcrawler.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class EnumsTest {

    @Test
    public void hashRouteModeHasThreeOptions() {
        assertThat(HashRouteMode.values(), arrayWithSize(3));
        assertThat(HashRouteMode.valueOf("IGNORE"), is(HashRouteMode.IGNORE));
        assertThat(HashRouteMode.valueOf("PATH"), is(HashRouteMode.PATH));
        assertThat(HashRouteMode.valueOf("PRESERVE"), is(HashRouteMode.PRESERVE));
    }

    @Test
    public void outputFormatHasThreeOptions() {
        assertThat(OutputFormat.values(), arrayWithSize(3));
        assertThat(OutputFormat.valueOf("TEXT"), is(OutputFormat.TEXT));
        assertThat(OutputFormat.valueOf("HTML"), is(OutputFormat.HTML));
        assertThat(OutputFormat.valueOf("MARKDOWN"), is(OutputFormat.MARKDOWN));
    }

    @Test
    public void regexUrlsFilterLogicHasIncludeAndExclude() {
        assertThat(RegexUrlsFilterLogic.values(), arrayWithSize(2));
        assertThat(RegexUrlsFilterLogic.valueOf("INCLUDE"), is(RegexUrlsFilterLogic.INCLUDE));
        assertThat(RegexUrlsFilterLogic.valueOf("EXCLUDE"), is(RegexUrlsFilterLogic.EXCLUDE));
    }

    @Test
    public void documentExtensionCoversCommonTypes() {
        assertThat(DocumentExtension.values(), arrayWithSize(9));
        assertThat(DocumentExtension.valueOf("PDF"), is(DocumentExtension.PDF));
        assertThat(DocumentExtension.valueOf("DOCX"), is(DocumentExtension.DOCX));
        assertThat(DocumentExtension.valueOf("ZIP"), is(DocumentExtension.ZIP));
    }

    @Test
    public void pageInsightTypeCoversAllVariants() {
        assertThat(PageInsightType.values(), arrayWithSize(8));
        assertThat(PageInsightType.valueOf("ALL"), is(PageInsightType.ALL));
        assertThat(PageInsightType.valueOf("DOCUMENTLINKS"), is(PageInsightType.DOCUMENTLINKS));
    }
}
