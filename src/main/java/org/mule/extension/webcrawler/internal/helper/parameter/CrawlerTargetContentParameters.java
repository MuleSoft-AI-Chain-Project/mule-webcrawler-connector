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
  @DisplayName("Tag list")
  @Summary("List of html tags for which content must be retrieved.")
  @Placement(order = 1)
  @Optional
  private List<String> tags;

  @Parameter
  @Alias("getMetaTags")
  @DisplayName("Retrieve meta tags")
  @Summary("If true metatags are retrieved from the provided url.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean getMetaTags;

  @Parameter
  @Alias("downloadImages")
  @DisplayName("Download images")
  @Summary("If true images referenced at the provided url are downloaded.")
  @Placement(order = 3)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean downloadImages;

  @Parameter
  @Alias("maxImageNumber")
  @DisplayName("Max image number for each page")
  @Summary("Maximum number of images to download for each page. Default 0 means no limit.")
  @Placement(order = 4)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("0")
  @Optional
  private int maxImageNumber;

  @Parameter
  @Alias("downloadDocuments")
  @DisplayName("Download documents")
  @Summary("If true documents referenced at the provided url are downloaded.")
  @Placement(order = 5)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("False")
  private boolean downloadDocuments;

  @Parameter
  @Alias("maxDocumentNumber")
  @DisplayName("Max document number for each page")
  @Summary("Maximum number of documents to download for each page. Default 0 means no limit.")
  @Placement(order = 6)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("0")
  @Optional
  private int maxDocumentNumber;

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

  public int getMaxImageNumber() {
    return maxImageNumber;
  }

  public void setMaxImageNumber(int maxImageNumber) {
    this.maxImageNumber = maxImageNumber;
  }

  public boolean isDownloadDocuments() {
    return downloadDocuments;
  }

  public void setDownloadDocuments(boolean downloadDocuments) {
    this.downloadDocuments = downloadDocuments;
  }

  public int getMaxDocumentNumber() {
    return maxDocumentNumber;
  }

  public void setMaxDocumentNumber(int maxDocumentNumber) {
    this.maxDocumentNumber = maxDocumentNumber;
  }

  @Override
  public String toString() {
    return "CrawlerTargetContentParameters{" +
        "tags=" + tags +
        ", getMetaTags=" + getMetaTags +
        ", downloadImages=" + downloadImages +
        ", maxImageNumber=" + maxImageNumber +
        ", downloadDocuments=" + downloadDocuments +
        ", maxDocumentNumber=" + maxDocumentNumber +
        '}';
  }
}
