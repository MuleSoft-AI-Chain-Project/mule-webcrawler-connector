package org.mule.extension.webcrawler.internal.crawler;

import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.constant.Constants.RegexUrlsFilterLogic;
import org.mule.extension.webcrawler.internal.crawler.mule.MuleCrawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class Crawler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);

  protected Queue<SiteNode> siteNodeQueue;
  protected Set<String> visitedLinksGlobal;

  protected WebCrawlerConfiguration configuration;
  protected WebCrawlerConnection connection;
  protected String rootURL;
  protected Long waitOnPageLoad;
  protected String waitForXPath;
  protected boolean extractShadowDom;
  protected String shadowHostXPath;
  protected int maxDepth;
  protected boolean restrictToPath;
  protected boolean downloadImages;
  protected int maxImageNumber;
  protected boolean downloadDocuments;
  protected int maxDocumentNumber;
  protected String downloadPath;
  protected List<String> contentTags;
  protected Constants.OutputFormat outputFormat;
  protected boolean getMetaTags;
  protected RegexUrlsFilterLogic regexUrlsFilterLogic;
  protected List<String> regexUrls;

  public Crawler(WebCrawlerConfiguration configuration, WebCrawlerConnection connection, String rootURL, Long waitOnPageLoad,
                 String waitForXPath, boolean extractShadowDom, String shadowHostXPath, int maxDepth, boolean restrictToPath,
                 boolean downloadImages, int maxImageNumber, boolean downloadDocuments, int maxDocumentNumber, String downloadPath,
                 List<String> contentTags, Constants.OutputFormat outputFormat, boolean getMetaTags,
                 RegexUrlsFilterLogic regexUrlsFilterLogic, List<String> regexUrls) {

    this.configuration = configuration;
    this.connection = connection;
    this.rootURL = rootURL;
    this.waitOnPageLoad = waitOnPageLoad;
    this.waitForXPath = waitForXPath;
    this.extractShadowDom = extractShadowDom;
    this.shadowHostXPath = shadowHostXPath;
    this.maxDepth = maxDepth;
    this.restrictToPath = restrictToPath;
    this.downloadImages = downloadImages;
    this.maxImageNumber = maxImageNumber;
    this.downloadDocuments = downloadDocuments;
    this.maxDocumentNumber = maxDocumentNumber;
    this.downloadPath = downloadPath;
    this.contentTags = contentTags;
    this.outputFormat = outputFormat;
    this.getMetaTags = getMetaTags;
    this.regexUrlsFilterLogic = regexUrlsFilterLogic;
    this.regexUrls = regexUrls;
  }

  public abstract SiteNode crawl();

  public abstract SiteNode map();

  public static Crawler.Builder builder() {

    return new Crawler.Builder();
  }

  @Override
  public String toString() {
    return "Crawler{" +
        "siteNodeQueue=" + siteNodeQueue +
        ", visitedLinksGlobal=" + visitedLinksGlobal +
        ", configuration=" + configuration +
        ", connection=" + connection +
        ", rootURL='" + rootURL + '\'' +
        ", waitOnPageLoad=" + waitOnPageLoad +
        ", waitForXPath='" + waitForXPath + '\'' +
        ", extractShadowDom=" + extractShadowDom +
        ", shadowHostXPath='" + shadowHostXPath + '\'' +
        ", maxDepth=" + maxDepth +
        ", restrictToPath=" + restrictToPath +
        ", downloadImages=" + downloadImages +
        ", maxImageNumber=" + maxImageNumber +
        ", downloadDocuments=" + downloadDocuments +
        ", maxDocumentNumber=" + maxDocumentNumber +
        ", downloadPath='" + downloadPath + '\'' +
        ", contentTags=" + contentTags +
        ", outputFormat=" + outputFormat +
        ", getMetaTags=" + getMetaTags +
        ", regexUrlsFilterLogic=" + regexUrlsFilterLogic +
        ", regexUrls=" + regexUrls +
        '}';
  }

  public static class Builder {

    private WebCrawlerConfiguration configuration;
    private WebCrawlerConnection connection;
    private String rootURL;
    private Long waitOnPageLoad;
    private String waitForXPath;
    private boolean extractShadowDom;
    private String shadowHostXPath;
    private int maxDepth;
    private boolean restrictToPath = false;
    private boolean downloadImages;
    private int maxImageNumber;
    private boolean downloadDocuments;
    private int maxDocumentNumber;
    private String downloadPath;
    private List<String> contentTags;
    private Constants.OutputFormat outputFormat;
    private boolean getMetaTags = false;
    private RegexUrlsFilterLogic regexUrlsFilterLogic;
    private List<String> regexUrls;

    public Crawler.Builder configuration(WebCrawlerConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }

    public Crawler.Builder connection(WebCrawlerConnection connection) {
      this.connection = connection;
      return this;
    }

    public Crawler.Builder rootURL(String rootURL) {
      this.rootURL = rootURL;
      return this;
    }

    public Crawler.Builder waitOnPageLoad(Long waitOnPageLoad) {
      this.waitOnPageLoad = waitOnPageLoad;
      return this;
    }

    public Crawler.Builder waitForXPath(String waitForXPath) {
      this.waitForXPath = waitForXPath;
      return this;
    }

    public Crawler.Builder extractShadowDom(boolean extractShadowDom) {
      this.extractShadowDom = extractShadowDom;
      return this;
    }

    public Crawler.Builder shadowHostXPath(String shadowHostXPath) {
      this.shadowHostXPath = shadowHostXPath;
      return this;
    }

    public Crawler.Builder maxDepth(int maxDepth) {
      this.maxDepth = maxDepth;
      return this;
    }

    public Crawler.Builder restrictToPath(boolean restrictToPath) {
      this.restrictToPath = restrictToPath;
      return this;
    }

    public Crawler.Builder downloadImages(boolean downloadImages) {
      this.downloadImages = downloadImages;
      return this;
    }

    public Crawler.Builder maxImageNumber(int maxImageNumber) {
      this.maxImageNumber = maxImageNumber;
      return this;
    }

    public Crawler.Builder downloadDocuments(boolean downloadDocuments) {
      this.downloadDocuments = downloadDocuments;
      return this;
    }

    public Crawler.Builder maxDocumentNumber(int maxDocumentNumber) {
      this.maxDocumentNumber = maxDocumentNumber;
      return this;
    }

    public Crawler.Builder downloadPath(String downloadPath) {
      this.downloadPath = downloadPath;
      return this;
    }

    public Crawler.Builder contentTags(List<String> contentTags) {
      this.contentTags = contentTags;
      return this;
    }

    public Crawler.Builder outputFormat(Constants.OutputFormat outputFormat) {
      this.outputFormat = outputFormat;
      return this;
    }

    public Crawler.Builder getMetaTags(boolean getMetaTags) {
      this.getMetaTags = getMetaTags;
      return this;
    }

    public Crawler.Builder regexUrlsFilterLogic(RegexUrlsFilterLogic regexUrlsFilterLogic) {
      this.regexUrlsFilterLogic = regexUrlsFilterLogic;
      return this;
    }

    public Crawler.Builder regexUrls(List<String> regexUrls) {
      this.regexUrls = regexUrls;
      return this;
    }

    public Crawler build() {

      Crawler crawler;

      try {

        crawler = new MuleCrawler(configuration, connection, rootURL, waitOnPageLoad, waitForXPath, extractShadowDom, shadowHostXPath,
                                  maxDepth, restrictToPath, downloadImages, maxImageNumber, downloadDocuments, maxDocumentNumber,
                                  downloadPath, contentTags, outputFormat, getMetaTags, regexUrlsFilterLogic, regexUrls);

      } catch (ModuleException e) {

        throw e;

      } catch (Exception e) {

        throw new ModuleException(
            "Error while initializing crawler.",
            WebCrawlerErrorType.CRAWL_OPERATIONS_FAILURE,
            e);
      }

      return crawler;
    }
  }

  public static class SiteNode {

    private String url;
    @JsonIgnore
    private int currentDepth;
    @JsonIgnore
    private String referrer;
    private String filename;
    private List<SiteNode> children;
    @JsonIgnore
    private SiteNode parent;

    public SiteNode(String url, int currentDepth, String referrer) {

      this.url = url;
      this.currentDepth = currentDepth;
      this.referrer = referrer;
      this.children = new ArrayList<>();
    }

    public SiteNode(String url, int currentDepth, String referrer, SiteNode parent) {

      this.url = url;
      this.currentDepth = currentDepth;
      this.referrer = referrer;
      this.children = new ArrayList<>();
      this.parent = parent;
    }

    public SiteNode(String url, int currentDepth, String referrer, String filename) {

      this.url = url;
      this.currentDepth = currentDepth;
      this.filename = filename;
      this.referrer = referrer;
      this.children = new ArrayList<>();
    }

    public String getUrl() {
      return url;
    }

    public int getCurrentDepth() {
      return currentDepth;
    }

    public String getReferrer() {
      return referrer;
    }

    public String getFilename() {
      return filename;
    }

    public void setFilename(String filename) {
      this.filename = filename;
    }

    public List<SiteNode> getChildren() {
      return children;
    }

    public void addChild(SiteNode child) {
      this.children.add(child);
    }

    public SiteNode getParent() {
      return parent;
    }
  }

  public static class SitemapGenerator {

    public static String generateSitemapXml(SiteNode rootNode) {
      StringBuilder xml = new StringBuilder();
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

      appendUrl(xml, rootNode);

      xml.append("</urlset>");
      return xml.toString();
    }

    private static void appendUrl(StringBuilder xml, SiteNode node) {
      xml.append("  <url>\n");
      xml.append("    <loc>").append(escapeXml(node.getUrl())).append("</loc>\n");
      xml.append("    <priority>").append(calculatePriority(node.getCurrentDepth())).append("</priority>\n");
      xml.append("  </url>\n");

      for (SiteNode child : node.getChildren()) {
        appendUrl(xml, child);
      }
    }

    private static String escapeXml(String text) {
      return text.replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;")
          .replace("'", "&apos;");
    }

    private static String calculatePriority(int depth) {
      double priority = Math.max(0.0, Math.min(1.0, 1.0 - (depth * 0.1)));
      return String.format("%.1f", priority);
    }
  }

  public DocumentIterator documentIterator() { return new DocumentIterator(); }

  public class DocumentIterator implements Iterator<Document> {

    public DocumentIterator() {
      siteNodeQueue = new LinkedList<>();
    }

    @Override
    public boolean hasNext() {
      throw new UnsupportedOperationException("This method should be overridden by subclasses");
    }

    @Override
    public Document next() {
      throw new UnsupportedOperationException("This method should be overridden by subclasses");
    }
  }
}
