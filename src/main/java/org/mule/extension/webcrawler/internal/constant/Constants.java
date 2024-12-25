package org.mule.extension.webcrawler.internal.constant;

public class Constants {

  private Constants() {}

  public enum CrawlType { CONTENT, LINK }

  public enum PageInsightType { ALL, INTERNALLINKS, EXTERNALLINKS, REFERENCELINKS, IMAGELINKS, ELEMENTCOUNTSTATS }
}
