package org.mule.extension.webcrawler.internal.helper.parameter;

import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;

public class CrawlerTargetPagesParameters {

  @Parameter
  @Alias("maxDepth")
  @DisplayName("Maximum depth")
  @Summary("The maximum level of depth that can be reached while crawling a website.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("2")
  private int maxDepth;

  @Parameter
  @Alias("restrictToPath")
  @DisplayName("Restrict crawl under URL")
  @Summary("If true only internal pages are crawled.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("True")
  private boolean restrictToPath;

  @Parameter
  @Alias("regexUrlsFilterLogic")
  @Summary("Type of filter logic to apply to regex URLs while crawling.")
  @DisplayName("Regex URLs filter logic")
  @Placement(order = 3)
  @Optional
  private Constants.RegexUrlsFilterLogic regexUrlsFilterLogic;

  @Parameter
  @Alias("regexUrls")
  @DisplayName("Regex URLs List")
  @Summary("List of regex patterns for URLs to include or exclude while crawling.")
  @Placement(order = 4)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("https://www\\.googletagmanager\\.com/.*")
  @Optional(defaultValue = "#[[\"https://www\\.googletagmanager\\.com/.*\"]]")
  private List<String> regexUrls;

  public boolean isRestrictToPath() {
    return restrictToPath;
  }

  public void setRestrictToPath(boolean restrictToPath) {
    this.restrictToPath = restrictToPath;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public Constants.RegexUrlsFilterLogic getRegexUrlsFilterLogic() { return regexUrlsFilterLogic; }

  public void setRegexUrlsFilterLogic(Constants.RegexUrlsFilterLogic regexUrlsFilterLogic) { this.regexUrlsFilterLogic = regexUrlsFilterLogic; }

  public List<String> getRegexUrls() { return regexUrls; }

  public void setRegexUrls(List<String> regexUrls) { this.regexUrls = regexUrls; }


  @Override
  public String toString() {
    return "CrawlerTargetPagesParameters{" +
        "maxDepth=" + maxDepth +
        ", restrictToPath=" + restrictToPath +
        ", urlFilterType=" + regexUrlsFilterLogic +
        ", regexUrls=" + regexUrls +
        '}';
  }
}
