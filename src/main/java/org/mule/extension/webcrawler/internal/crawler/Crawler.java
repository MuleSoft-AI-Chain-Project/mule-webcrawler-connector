package org.mule.extension.webcrawler.internal.crawler;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
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

  protected Set<String> visitedLinksGlobal;
  protected Map<Integer, Set<String>> visitedLinksByDepth;

  protected WebCrawlerConnection connection;
  protected String rootURL;
  protected int maxDepth;
  protected boolean restrictToPath;
  protected int delayMillis;
  protected boolean enforceRobotsTxt;
  protected boolean downloadImages;
  protected int maxImageNumber;
  protected boolean downloadDocuments;
  protected int maxDocumentNumber;
  protected String downloadPath;
  protected List<String> contentTags;
  protected boolean rawHtml;
  protected boolean getMetaTags;
  protected RegexUrlsFilterLogic regexUrlsFilterLogic;
  protected List<String> regexUrls;

  public Crawler(WebCrawlerConnection connection, String rootURL, int maxDepth, boolean restrictToPath, int delayMillis,
                 boolean enforceRobotsTxt, boolean downloadImages, int maxImageNumber, boolean downloadDocuments,
                 int maxDocumentNumber, String downloadPath, List<String> contentTags, boolean rawHtml, boolean getMetaTags,
                 RegexUrlsFilterLogic regexUrlsFilterLogic, List<String> regexUrls) {

    this.connection = connection;
    this.rootURL = rootURL;
    this.maxDepth = maxDepth;
    this.restrictToPath = restrictToPath;
    this.delayMillis = delayMillis;
    this.enforceRobotsTxt = enforceRobotsTxt;
    this.downloadImages = downloadImages;
    this.maxImageNumber = maxImageNumber;
    this.downloadDocuments = downloadDocuments;
    this.maxDocumentNumber = maxDocumentNumber;
    this.downloadPath = downloadPath;
    this.contentTags = contentTags;
    this.rawHtml = rawHtml;
    this.getMetaTags = getMetaTags;
    this.regexUrlsFilterLogic = regexUrlsFilterLogic;
    this.regexUrls = regexUrls;
  }

  public abstract SiteNode crawl();

  public abstract SiteNode map();

  public static Crawler.Builder builder() {

    return new Crawler.Builder();
  }

  public static class Builder {

    private WebCrawlerConnection connection;
    private String rootURL;
    private int maxDepth;
    private boolean restrictToPath = false;
    private int delayMillis;
    private boolean enforceRobotsTxt;
    private boolean downloadImages;
    private int maxImageNumber;
    private boolean downloadDocuments;
    private int maxDocumentNumber;
    private String downloadPath;
    private List<String> contentTags;
    private boolean rawHtml = false;
    private boolean getMetaTags = false;
    private RegexUrlsFilterLogic regexUrlsFilterLogic;
    private List<String> regexUrls;

    public Crawler.Builder connection(WebCrawlerConnection connection) {
      this.connection = connection;
      return this;
    }


    public Crawler.Builder rootURL(String rootURL) {
      this.rootURL = rootURL;
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

    public Crawler.Builder delayMillis(int delayMillis) {
      this.delayMillis = delayMillis;
      return this;
    }

    public Crawler.Builder enforceRobotsTxt(boolean enforceRobotsTxt) {
      this.enforceRobotsTxt = enforceRobotsTxt;
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

    public Crawler.Builder rawHtml(boolean rawHtml) {
      this.rawHtml = rawHtml;
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

        crawler = new MuleCrawler(connection, rootURL, maxDepth, restrictToPath, delayMillis,
                                  enforceRobotsTxt, downloadImages, maxImageNumber, downloadDocuments, maxDocumentNumber,
                                  downloadPath, contentTags, rawHtml, getMetaTags, regexUrlsFilterLogic, regexUrls);

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

    public SiteNode(String url, int currentDepth, String referrer) {

      this.url = url;
      this.currentDepth = currentDepth;
      this.referrer = referrer;
      this.children = new ArrayList<>();
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

    public List<SiteNode> getChildren() {
      return children;
    }

    public void addChild(SiteNode child) {
      this.children.add(child);
    }
  }

  public DocumentIterator documentIterator() { return new DocumentIterator(); }

  public class DocumentIterator implements Iterator<Document> {

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
