package org.mule.extension.webcrawler.internal.constant;

public class Constants {

  private Constants() {
  }

  public enum CrawlType {CONTENT, LINK}

  public enum PageInsightType {ALL, DOCUMENTLINKS, INTERNALLINKS, EXTERNALLINKS, REFERENCELINKS, IFRAMELINKS, IMAGELINKS, ELEMENTCOUNTSTATS}

  public enum DocumentExtension {PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, ZIP, RAR}

  public enum RegexUrlsFilterLogic { INCLUDE, EXCLUDE}

  // Google Chrome User-Agents
  public static final String USER_AGENT_CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
  public static final String USER_AGENT_CHROME_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_3_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
  public static final String USER_AGENT_CHROME_LINUX = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
  // Mozilla Firefox User-Agents
  public static final String USER_AGENT_FIREFOX_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:112.0) Gecko/20100101 Firefox/112.0";
  public static final String USER_AGENT_FIREFOX_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13.3; rv:112.0) Gecko/20100101 Firefox/112.0";
  public static final String USER_AGENT_FIREFOX_LINUX = "Mozilla/5.0 (X11; Linux x86_64; rv:112.0) Gecko/20100101 Firefox/112.0";
  // Safari User-Agents
  public static final String USER_AGENT_SAFARI_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_3_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Safari/605.1.15";
  // Microsoft Edge User-Agents
  public static final String USER_AGENT_EDGE_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 Edg/112.0.0.0";
  // Mobile User-Agents
  public static final String USER_AGENT_CHROME_ANDROID = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36";
  public static final String USER_AGENT_SAFARI_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1";
  // Googlebot User-Agent
  public static final String USER_AGENT_GOOGLEBOT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
  // Bingbot User-Agent
  public static final String USER_AGENT_BINGBOT = "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)";
  // General Mobile User-Agent
  public static final String USER_AGENT_MOBILE_GENERIC = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36";
  // Custom User-Agent for fallback
  public static final String USER_AGENT_CUSTOM_DEFAULT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";

}
