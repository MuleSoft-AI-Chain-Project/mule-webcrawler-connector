package org.mule.extension.webcrawler.internal.crawler;

import org.mule.extension.webcrawler.internal.crawler.mule.MuleCrawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Crawler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);

  protected Set<String> visitedLinksGlobal;
  protected Map<Integer, Set<String>> visitedLinksByDepth;

  protected String rootURL;
  protected int maxDepth;
  protected boolean restrictToPath;
  protected boolean dynamicContent;
  protected int delayMillis;
  protected boolean downloadImages;
  protected boolean downloadDocuments;
  protected String downloadPath;
  protected List<String> contentTags;
  protected boolean getMetaTags;

  public Crawler(String rootURL, int maxDepth, boolean restrictToPath, boolean dynamicContent, int delayMillis, boolean downloadImages,
                 boolean downloadDocuments, String downloadPath, List<String> contentTags, boolean getMetaTags) {

    this.rootURL = rootURL;
    this.maxDepth = maxDepth;
    this.restrictToPath = restrictToPath;
    this.dynamicContent = dynamicContent;
    this.delayMillis = delayMillis;
    this.downloadImages = downloadImages;
    this.downloadDocuments = downloadDocuments;
    this.downloadPath = downloadPath;
    this.contentTags = contentTags;
    this.getMetaTags = getMetaTags;
  }

  public abstract CrawlNode crawl();

  public abstract MapNode map();

  public static Crawler.Builder builder() {

    return new Crawler.Builder();
  }

  public static class Builder {

    private String rootURL;
    private int maxDepth;
    private boolean restrictToPath = false;
    private boolean dynamicContent = false;
    private int delayMillis;
    private boolean downloadImages = false;
    private boolean downloadDocuments = false;
    private String downloadPath;
    private List<String> contentTags;
    private boolean getMetaTags = false;

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

    public Crawler.Builder dynamicContent(boolean dynamicContent) {
      this.dynamicContent = dynamicContent;
      return this;
    }

    public Crawler.Builder delayMillis(int delayMillis) {
      this.delayMillis = delayMillis;
      return this;
    }

    public Crawler.Builder downloadImages(boolean downloadImages) {
      this.downloadImages = downloadImages;
      return this;
    }

    public Crawler.Builder downloadDocuments(boolean downloadDocuments) {
      this.downloadDocuments = downloadDocuments;
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

    public Crawler.Builder getMetaTags(boolean getMetaTags) {
      this.getMetaTags = getMetaTags;
      return this;
    }

    public Crawler build() {

      Crawler crawler;

      try {

        crawler = new MuleCrawler(rootURL, maxDepth, restrictToPath, dynamicContent, delayMillis, downloadImages,
                                  downloadDocuments, downloadPath, contentTags, getMetaTags);

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

  public static class MapNode {

    private String url;
    private List<MapNode> children;

    public MapNode(String url) {
      this.url = url;
      this.children = new ArrayList<>();
    }

    public String getUrl() {
      return url;
    }

    public List<MapNode> getChildren() {
      return children;
    }

    public void addChild(MapNode child) {
      this.children.add(child);
    }
  }

  public static class CrawlNode extends MapNode {

    private final String pageContentFile;

    public CrawlNode(String pageURL, String pageContentFile) {

      super(pageURL);
      this.pageContentFile = pageContentFile;

    }

    public String getPageContentFile() {
      return pageContentFile;
    }
  }

}
