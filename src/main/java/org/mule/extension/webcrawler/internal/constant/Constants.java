package org.mule.extension.webcrawler.internal.constant;

public class Constants {

  private Constants() {
  }

  public enum CrawlType {CONTENT, LINK}

  public enum PageInsightType {ALL, DOCUMENTLINKS, INTERNALLINKS, EXTERNALLINKS, REFERENCELINKS, IMAGELINKS, ELEMENTCOUNTSTATS}

  public enum DocumentExtension {PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, ZIP, RAR}

}
