package com.mule.mulechain.crawler.internal;

import com.mule.mulechain.crawler.internal.helpers.CrawlResult;
import com.mule.mulechain.crawler.internal.helpers.SiteMapNode;
import com.mule.mulechain.crawler.internal.helpers.crawlingHelper;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import okhttp3 library for google search operation
import okhttp3.RequestBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class MulechainwebcrawlerOperations {

  private enum CrawlType {
    CONTENT,
    LINK
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MulechainwebcrawlerOperations.class);

  /**
   * Crawl a website at a specified depth and fetch contents. Specify tags and
   * classes in the configuration to fetch contents from those elements only.
   *
   * @throws IOException
   */

  /*
   * JSoup limitiations / web crawl challenges
   * - some sites prevent robots - use of User-Agent may be required but not
   * always guaranteed to work
   * - JavaScript generated content is not read by jsoup
   * - some sites require cookies or sessions to be present
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Crawl-website")
  public String crawlWebsite(@Config MulechainwebcrawlerConfiguration configuration,
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Maximum Depth") @Placement(order = 2) @Example("2") int maxDepth,
      @DisplayName("Retrieve Meta Tags") @Placement(order = 3) @Example("Yes") boolean getMetaTags,
      @DisplayName("Download Images") @Placement(order = 4) @Example("Yes") boolean downloadImages,
      @DisplayName("Download Location") @Placement(order = 5) @Example("/users/mulesoft/downloads") String downloadPath)
      throws IOException {
    LOGGER.info("Website crawl action");

    // initialise variables
    Set<String> visitedLinksGlobal = new HashSet<>();
    Map<Integer, Set<String>> visitedLinksByDepth = new HashMap<>();
    List<String> specificTags = configuration.getTags();

    SiteMapNode root = startCrawling(url, 0, maxDepth, visitedLinksByDepth, visitedLinksGlobal, downloadImages,
        downloadPath, specificTags, getMetaTags, CrawlType.CONTENT);

    return crawlingHelper.convertToJSON(root);
  }

  /**
   * Fetch the meta tags from a web page.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Get-page-meta-tags")
  public String getMetaTags(
      @DisplayName("Page URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url)
      throws IOException {
    LOGGER.info("Get meta tags");

    Document document = crawlingHelper.getDocument(url);

    return crawlingHelper.convertToJSON(crawlingHelper.getPageMetaTags(document));
  }

  /**
   * Retrieve internal links as a site map from the specified url and depth.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Generate-sitemap")
  public String getSiteMap(
      @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Maximum Depth") @Placement(order = 2) @Example("2") int maxDepth) throws IOException {
    LOGGER.info("Generate sitemap");

    // initialise variables
    Set<String> visitedLinksGlobal = new HashSet<>();
    Map<Integer, Set<String>> visitedLinksByDepth = new HashMap<>();

    SiteMapNode root = startCrawling(url, 0, maxDepth, visitedLinksByDepth, visitedLinksGlobal, false, null, null,
        false, CrawlType.LINK);

    return crawlingHelper.convertToJSON(root);
  }

  /**
   * Download all images from a web page, or download a single image at the
   * specified link.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Download-image")
  public String downloadWebsiteImages(
      @DisplayName("Page Or Image URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
      @DisplayName("Download Location") @Placement(order = 2) @Example("/users/mulesoft/downloads") String downloadPath)
      throws IOException {

    String result = "";

    try {
      // url provided is a website url, so download all images from this document
      Document document = crawlingHelper.getDocument(url);
      result = crawlingHelper.convertToJSON(downloadWebsiteImages(document, downloadPath));
    } catch (UnsupportedMimeTypeException e) {
      // url provided is direct link to image, so download single image

      Map<String, String> linkFileMap = new HashMap<>();
      linkFileMap.put(url, downloadSingleImage(url, downloadPath));
      result = crawlingHelper.convertToJSON(linkFileMap);
    }
    return result;
  }

  /**
   * Get insights from a web page including links, word count, number of
   * occurrences of elements. Restrict insights to specific elements in the
   * configuration.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Get-page-insights")
  public String getPageInsights(
      @Config MulechainwebcrawlerConfiguration configuration,
      @DisplayName("Page Url") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url)
      throws IOException {
    LOGGER.info("Analyze page");

    Document document = crawlingHelper.getDocument(url);

    return crawlingHelper.convertToJSON(
        crawlingHelper.getPageInsights(document, configuration.getTags(), crawlingHelper.PageInsightType.ALL));
  }

  /**
   * Get contents of a web page. Content is returned in the resulting payload.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Get-page-content")
  public String getPageContent(
      @Config MulechainwebcrawlerConfiguration configuration,
      @DisplayName("Page Url") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url)
      throws IOException {
    LOGGER.info("Get page content");

    Map<String, String> contents = new HashMap<String, String>();

    Document document = crawlingHelper.getDocument(url);

    contents.put("url", document.baseUri());
    contents.put("title", document.title());
    contents.put("content", crawlingHelper.getPageContent(document, configuration.getTags()));

    return crawlingHelper.convertToJSON(contents);
  }

  private String savePageContents(Object results, String downloadPath, String title) throws IOException {

    String pageContents = crawlingHelper.convertToJSON(results);

    String fileName = "";

    // Generate a unique filename using the current timestamp
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());

    // Create a unique filename based on the sanitized title
    fileName = crawlingHelper.getSanitizedFilename(title) + "_" + timestamp + ".json";

    // Write JSON content to the file
    // Ensure the output directory exists
    File file = new File(downloadPath, fileName);
    // Ensure the directory exists
    file.getParentFile().mkdirs();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      // Write content to the file
      writer.write(pageContents);
      LOGGER.info("Saved content to file: " + fileName);
    } catch (IOException e) {
      LOGGER.error("An error occurred while writing to the file: " + e.getMessage());
    }

    return (file != null) ? file.getName() : "File is null";
  }

  // private String startCrawling(String url, int depth, int maxDepth, Set<String>
  // visitedLinks, boolean downloadImages, String downloadPath, List<String> tags)
  // {
  private SiteMapNode startCrawling(String url, int depth, int maxDepth, Map<Integer, Set<String>> visitedLinksByDepth,
      Set<String> visitedLinksGlobal, boolean downloadImages, String downloadPath, List<String> contentTags,
      boolean getMetaTags, CrawlType crawlType) {

    // return if maxDepth reached
    if (depth > maxDepth) {
      return null;
    }

    // Initialize the set for the current depth if not already present
    visitedLinksByDepth.putIfAbsent(depth, new HashSet<>());

    // Check if this URL has already been visited at this depth
    if (visitedLinksByDepth.get(depth).contains(url)) {
      return null;
    }

    // crawl & extract current page
    try {

      // Mark the URL as visited for this depth
      visitedLinksByDepth.get(depth).add(url);

      SiteMapNode node = null;

      // get page as a html document
      Document document = crawlingHelper.getDocument(url);

      // check if url contents have been downloaded before ie applied globally (at all
      // depths). Note, we don't want to do this globally for CrawlType.LINK because
      // we want a link to be unique only at the depth level and not globally (at all
      // depths)
      if (!visitedLinksGlobal.contains(url) && crawlType == CrawlType.CONTENT) {

        // add url to urlContentFetched to indicate content has been fetched.
        visitedLinksGlobal.add(url);

        // Create Map to hold all data for the current page - this will be serialized to
        // JSON and saved to file
        Map<String, Object> pageData = new HashMap<>();

        LOGGER.info("Fetching content for : " + url);

        String title = document.title();

        pageData.put("url", url);
        pageData.put("title", title);

        // check if need to download images in the current page
        if (downloadImages) {
          LOGGER.info("Downloading images for : " + url);
          pageData.put("imageFiles", downloadWebsiteImages(document, downloadPath));
        }

        // get all meta tags from the document
        if (getMetaTags) {
          // Iterating over each entry in the map
          for (Map.Entry<String, String> entry : crawlingHelper.getPageMetaTags(document).entrySet()) {
            pageData.put(entry.getKey(), entry.getValue());
          }
        }

        // get page contents
        pageData.put("content", crawlingHelper.getPageContent(document, contentTags));

        // save gathered data of page to file
        String filename = savePageContents(pageData, downloadPath, title);

        // Create a new node for this URL
        node = new CrawlResult(url, filename);

      } else if (crawlType == CrawlType.LINK) {
        node = new SiteMapNode(url);
        LOGGER.info("Found url : " + url);
      } else {
        // content previously downloaded, so setting file name as such
        node = new CrawlResult(url, "Duplicate.");
      }

      // If not at max depth, find and crawl the links on the page
      if (depth <= maxDepth) {
        // get all links on the current page
        Set<String> links = new HashSet<>();

        Map<String, Object> linksMap = (Map<String, Object>) crawlingHelper
            .getPageInsights(document, null, crawlingHelper.PageInsightType.INTERNALLINKS).get("links");
        if (linksMap != null) {
          links = (Set<String>) linksMap.get("internal"); // Cast to Set<String>
        }

        if (links != null) {
          for (String nextUrl : links) {

            // Recursively crawl the link and add as a child
            SiteMapNode childNode = startCrawling(nextUrl, depth + 1, maxDepth, visitedLinksByDepth, visitedLinksGlobal,
                downloadImages, downloadPath, contentTags, getMetaTags, crawlType);
            if (childNode != null) {
              node.addChild(childNode);
            }
          }
        }
      }
      return node;
    } catch (Exception e) {
      LOGGER.error(e.toString());
    }
    return null;
  }

  private Map<String, String> downloadWebsiteImages(Document document, String saveDirectory) throws IOException {
    // List to store image URLs
    Set<String> imageUrls = new HashSet<>();

    Map<String, String> linkFileMap = new HashMap<>();

    Map<String, Object> linksMap = (Map<String, Object>) crawlingHelper
        .getPageInsights(document, null, crawlingHelper.PageInsightType.IMAGELINKS).get("links");
    if (linksMap != null) {
      imageUrls = (Set<String>) linksMap.get("images"); // Cast to Set<String>
    }

    if (imageUrls != null) {

      // Save all images found on the page
      LOGGER.info("Number of img[src] elements found : " + imageUrls.size());
      for (String imageUrl : imageUrls) {
        linkFileMap.put(imageUrl, downloadSingleImage(imageUrl, saveDirectory));
      }
    }
    return linkFileMap;
  }

  private String downloadSingleImage(String imageUrl, String saveDirectory) throws IOException {
    LOGGER.info("Found image : " + imageUrl);
    File file;
    try {
      // Check if the URL is a Data URL
      if (imageUrl.startsWith("data:image/")) {
        // Extract base64 data from the Data URL
        String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);

        if (base64Data.isEmpty()) {
          LOGGER.info("Base64 data is empty for URL: " + imageUrl);
          return "";
        }

        // Decode the base64 data
        byte[] imageBytes;

        try {
          imageBytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
          LOGGER.info("Error decoding base64 data: " + e.getMessage());
          return "";
        }

        if (imageBytes.length == 0) {
          LOGGER.info("Decoded image bytes are empty for URL: " + imageUrl);
          return "";
        }

        // Determine the file extension from the Data URL
        String fileType = imageUrl.substring(5, imageUrl.indexOf(";"));
        String fileExtension = fileType.split("/")[1];

        // Generate a unique filename using the current timestamp
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String fileName = "image_" + timestamp + "." + fileExtension;
        file = new File(saveDirectory, fileName);

        // Ensure the directory exists
        file.getParentFile().mkdirs();

        // Write the decoded bytes to the file
        try (FileOutputStream out = new FileOutputStream(file)) {
          out.write(imageBytes);
          LOGGER.info("DataImage saved: " + file.getAbsolutePath());
        }
      } else {
        // Handle standard image URLs
        URL url = new URL(imageUrl);

        // Extract the 'url' parameter from the query string
        String decodedUrl = crawlingHelper.extractAndDecodeUrl(imageUrl);
        // Extract the filename from the decoded URL
        String fileName = crawlingHelper.extractFileNameFromUrl(decodedUrl);

        // String fileName = decodedUrl.substring(imageUrl.lastIndexOf("/") + 1);
        file = new File(saveDirectory, fileName);

        // Ensure the directory exists
        file.getParentFile().mkdirs();

        // Download and save the image
        try (InputStream in = url.openStream();
            FileOutputStream out = new FileOutputStream(file)) {

          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
          }
        }
        LOGGER.info("Image saved: " + file.getAbsolutePath());

      }
    } catch (IOException e) {
      LOGGER.error("Error saving image: " + imageUrl);
      throw e;
    }

    return (file != null) ? file.getName() : "File is null";
  }

  /**
   * Perform a Google search using the SERP API.
   *
   * @throws IOException
   */
  @MediaType(value = MediaType.APPLICATION_JSON, strict = false)
  @Alias("Google-search")
  public String googleSearch(
      @DisplayName("Search Query") @Placement(order = 1) @Example("apple inc") String query,
      @DisplayName("API Key") @Placement(order = 2) @Example("your_api_key_here") String apiKey) throws IOException {
    LOGGER.info("Performing Google search for query: " + query);

    OkHttpClient client = new OkHttpClient().newBuilder().build();
    okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
    RequestBody body = RequestBody.create("{\"q\":\"" + query + "\"}", mediaType);
    Request request = new Request.Builder()
        .url("https://google.serper.dev/search")
        .method("POST", body)
        .addHeader("X-API-KEY", apiKey)
        .addHeader("Content-Type", "application/json")
        .build();
    Response response = client.newCall(request).execute();

    if (!response.isSuccessful()) {
      throw new IOException("Unexpected code " + response);
    }

    String responseBody = response.body().string();
    JSONObject jsonResponse = new JSONObject(responseBody);

    return jsonResponse.toString(4); // Pretty print with an indentation of 4 spaces
  }
}
