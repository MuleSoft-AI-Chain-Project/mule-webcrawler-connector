package org.mule.extension.webcrawler.internal.helper.parameter;

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

public class CrawlerTargetContentParameters {

  @Parameter
  @Alias("tags")
  @DisplayName("Tag List")
  @Summary("List of html tags for which content must be retrieved.")
  @Placement(order = 1)
  @Optional
  private List<String> tags;

  @Parameter
  @Alias("getMetaTags")
  @DisplayName("Retrieve Meta Tags")
  @Summary("If true metatags are retrieved from the provided url.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean getMetaTags;

  @Parameter
  @Alias("downloadImages")
  @DisplayName("Download Images")
  @Summary("If true images referenced at the provided url are downloaded.")
  @Placement(order = 3)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean downloadImages;

  @Parameter
  @Alias("downloadDocuments")
  @DisplayName("Download Documents")
  @Summary("If true documents referenced at the provided url are downloaded.")
  @Placement(order = 4)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean downloadDocuments;

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public boolean isGetMetaTags() {
    return getMetaTags;
  }

  public void setGetMetaTags(boolean getMetaTags) {
    this.getMetaTags = getMetaTags;
  }

  public boolean isDownloadImages() {
    return downloadImages;
  }

  public void setDownloadImages(boolean downloadImages) {
    this.downloadImages = downloadImages;
  }

  public boolean isDownloadDocuments() {
    return downloadDocuments;
  }

  public void setDownloadDocuments(boolean downloadDocuments) {
    this.downloadDocuments = downloadDocuments;
  }

  @Override
  public String toString() {
    return "CrawlerTargetContentParameters{" +
        "tags=" + tags +
        ", getMetaTags=" + getMetaTags +
        ", downloadImages=" + downloadImages +
        ", downloadDocuments=" + downloadDocuments +
        '}';
  }
}
