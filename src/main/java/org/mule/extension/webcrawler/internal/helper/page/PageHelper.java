package org.mule.extension.webcrawler.internal.helper.page;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.util.URLUtils;
import org.mule.extension.webcrawler.internal.util.Utils;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.SwitchPoint;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static org.mule.extension.webcrawler.internal.constant.Constants.OutputFormat.TEXT;

public class PageHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageHelper.class);

  private static final ConcurrentHashMap<String, String> robotsTxtCache = new ConcurrentHashMap<>();

  public static Document getDocument(WebCrawlerConfiguration webCrawlerConfiguration,
                                     WebCrawlerConnection connection,
                                     String url,
                                     Long waitDuration,
                                     String waitUntilXPath) throws IOException {

    LOGGER.debug(String.format("Retrieving JSoup Document for url %s", url));
    try (InputStream pageSourceInputStream = connection.getPageSource(url, waitDuration, waitUntilXPath).get()) { // Blocks until complete
      String pageSource = new String(pageSourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
      return Jsoup.parse(pageSource, url);
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException("Error fetching page source", e);
    }
  }

  public static Document getDocument(WebCrawlerConfiguration webCrawlerConfiguration,
                                     WebCrawlerConnection connection,
                                     String url,
                                     String referrer,
                                     Long waitDuration,
                                     String waitUntilXPath) throws IOException {

    LOGGER.debug(String.format("Retrieving JSoup Document for url %s and referer %s", url, referrer));
    try (InputStream pageSourceInputStream = connection.getPageSource(url, referrer, waitDuration, waitUntilXPath).get()) { // Blocks until complete
      String pageSource = new String(pageSourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
      return Jsoup.parse(pageSource, url);
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(String.format("Error fetching page source for %s", url), e);
    }
  }

  public static JSONArray getPageMetaTags(Document document) {
    // Create a JSONArray to hold the structured meta tags
    JSONArray metaTagArray = new JSONArray();

    // Select all meta tags
    Elements metaTags = document.select("meta");

    // Iterate through each meta tag
    for (Element metaTag : metaTags) {
      // Extract the 'name', 'property', and 'content' attributes
      String name = metaTag.attr("name");
      String property = metaTag.attr("property");
      String content = metaTag.attr("content");

      // Only add to the array if 'name' or 'property' and 'content' are present
      if ((!name.isEmpty() || !property.isEmpty()) && !content.isEmpty()) {
        // Create a JSONObject for each meta tag
        JSONObject metaTagObject = new JSONObject();
        if (!property.isEmpty()) {
          metaTagObject.put("property", property);
        } else {
          metaTagObject.put("name", name);
        }
        metaTagObject.put("content", content);

        // Add the meta tag object to the array
        metaTagArray.put(metaTagObject);
      }
    }

    // Return the array directly
    return metaTagArray;
  }

  public static HashMap<String, Object> getPageInsights(
      Document document,
      List<String> tags,
      Constants.PageInsightType insight) {

    return getPageInsights(document, tags, insight, null, null);
  }

  public static HashMap<String, Object> getPageInsights(
      Document document,
      List<String> tags,
      Constants.PageInsightType insight,
      Constants.RegexUrlsFilterLogic regexUrlsFilterLogic,
      List<String> regexUrls) {

    // Map to store page analysis
    HashMap<String, Object> pageInsightData = new HashMap<>();

    try {

      // doc-link set
      Set<String> documentLinks = new HashSet<>();

      // links set
      Set<String> internalLinks = new HashSet<>();
      Set<String> externalLinks = new HashSet<>();
      Set<String> referenceLinks = new HashSet<>();

      // iframe
      Set<String> iframeLinks = new HashSet<>();

      // image-links set
      Set<String> imageLinks = new HashSet<>();

      // All links Map
      HashMap<String, Set> linksMap = new HashMap<>();

      // Map to store the element counts
      Map<String, Integer> elementCounts = new HashMap<>();


      String baseUrl = document.baseUri();


      if (insight == Constants.PageInsightType.ALL ||
          insight == Constants.PageInsightType.DOCUMENTLINKS ||
          insight == Constants.PageInsightType.INTERNALLINKS ||
          insight == Constants.PageInsightType.REFERENCELINKS ||
          insight == Constants.PageInsightType.EXTERNALLINKS) {

        // Select all anchor tags with href attributes
        Elements linkElements = document.select("a[href]");

        for (Element linkElement : linkElements) {
          String link = linkElement.absUrl("href"); // get absolute URLs

          // Check if the URL matches any regex pattern in the regex list
          if(skipUrl(link, regexUrlsFilterLogic, regexUrls))  continue;

          if(URLUtils.isDocumentUrl(link)) {
            documentLinks.add(link);
          } else if (URLUtils.isExternalLink(baseUrl, link)) {
            externalLinks.add(link);
          } else if (URLUtils.isReferenceLink(baseUrl, link)) {
            referenceLinks.add(link);
          } else {
            internalLinks.add(link);
          }
        }

        if (insight == Constants.PageInsightType.ALL || insight == Constants.PageInsightType.DOCUMENTLINKS)
          linksMap.put("documents", documentLinks);
        if (insight == Constants.PageInsightType.ALL || insight == Constants.PageInsightType.INTERNALLINKS)
          linksMap.put("internal", internalLinks);
        if (insight == Constants.PageInsightType.ALL || insight == Constants.PageInsightType.EXTERNALLINKS)
          linksMap.put("external", externalLinks);
        if (insight == Constants.PageInsightType.ALL || insight == Constants.PageInsightType.REFERENCELINKS)
          linksMap.put("reference", referenceLinks);
      }

      // Handle iframe
      if (insight == Constants.PageInsightType.ALL ||
          insight == Constants.PageInsightType.IFRAMELINKS) {

        // Select all iframe tags with src attributes
        Elements linkElements = document.select("iframe[src]");
        for (Element linkElement : linkElements) {

          String iFrameUrl = linkElement.absUrl("src");

          // Check if the URL matches any regex pattern in the regex list
          if(skipUrl(iFrameUrl, regexUrlsFilterLogic, regexUrls))  continue;

          iframeLinks.add(iFrameUrl);
        }

        linksMap.put("iframe", iframeLinks);
      }

      // Handle images
      if (insight == Constants.PageInsightType.ALL || insight == Constants.PageInsightType.IMAGELINKS) {

        // Select all img tags with src attributes
        Elements images = document.select("img[src]");
        for (Element img : images) {
          String imageUrl = img.absUrl("src");

          // Check if the URL matches any regex pattern in the regex list
          if(skipUrl(imageUrl, regexUrlsFilterLogic, regexUrls))  continue;

          imageLinks.add(imageUrl);
        }

        linksMap.put("images", imageLinks);

      }

      if (insight == Constants.PageInsightType.ALL ||
          insight == Constants.PageInsightType.ELEMENTCOUNTSTATS) {

        String[] elementsToCount = {"div", "p", "h1", "h2", "h3", "h4", "h5"}; // default list of elements to retrieve stats for. Used if no specific tags provided

        if (tags != null && !tags.isEmpty()) {
          elementsToCount = tags.toArray(new String[tags.size()]);
        }

        // Loop through each element type and count its occurrences
        for (String tag : elementsToCount) {
          Elements elements = document.select(tag);
          elementCounts.put(tag, elements.size());
        }

        elementCounts.put("internal", internalLinks.size());
        elementCounts.put("external", externalLinks.size());
        elementCounts.put("reference", referenceLinks.size());
        elementCounts.put("iframe", iframeLinks.size());
        elementCounts.put("images", imageLinks.size());
        elementCounts.put("wordCount", Utils.countWords(getPageContent(document, tags, TEXT)));

        pageInsightData.put("pageStats", elementCounts);

    }

    pageInsightData.put("url", document.baseUri());
    pageInsightData.put("title", document.title());

    // only add links if any of the types in condition has been requested
    if (insight == Constants.PageInsightType.ALL ||
        insight == Constants.PageInsightType.DOCUMENTLINKS ||
        insight == Constants.PageInsightType.INTERNALLINKS ||
        insight == Constants.PageInsightType.REFERENCELINKS ||
        insight == Constants.PageInsightType.EXTERNALLINKS ||
        insight == Constants.PageInsightType.IFRAMELINKS ||
        insight == Constants.PageInsightType.IMAGELINKS)

      pageInsightData.put("links", linksMap);

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while getting page insights for %s.", document.baseUri()),
          WebCrawlerErrorType.PAGE_OPERATIONS_FAILURE,
          e);
    }

    return pageInsightData;
  }

  /**
   * Determines whether a given URL should be skipped based on the provided filter logic and regex patterns.
   *
   * @param url                  The URL to evaluate.
   * @param regexUrlsFilterLogic The logic to apply for filtering URLs. It can either be INCLUDE or EXCLUDE.
   * @param regexUrls            A list of regex patterns to match against the URL.
   * @return {@code true} if the URL should be skipped according to the filter logic, {@code false} otherwise.
   */
  private static boolean skipUrl(String url, Constants.RegexUrlsFilterLogic regexUrlsFilterLogic, List<String> regexUrls) {
    if (regexUrlsFilterLogic != null && regexUrls != null && !regexUrls.isEmpty()) {
      boolean matchesPattern = regexUrls.stream().anyMatch(pattern -> Pattern.matches(pattern, url));
      if ((regexUrlsFilterLogic == Constants.RegexUrlsFilterLogic.INCLUDE && !matchesPattern) ||
          (regexUrlsFilterLogic == Constants.RegexUrlsFilterLogic.EXCLUDE && matchesPattern)) {
        return true;
      }
    }
    return false;
  }

  public static String getPageContent(Document document, List<String> tags, Constants.OutputFormat outputFormat) {

    switch(outputFormat) {
      case TEXT:
        return getPageContent(document, tags);
      case HTML:
        return getPageRawHtmlContent(document, tags);
      case MARKDOWN:
        return Utils.convertHtmlToMarkdown(getPageRawHtmlContent(document, tags));
      default:
        return getPageRawHtmlContent(document, tags);
    }
  }

  private static String getPageContent(Document document, List<String> tags) {
    StringBuilder collectedText = new StringBuilder();
    Set<Element> selectedElements = new HashSet<>();

    if (tags != null && !tags.isEmpty()) {
      for (String selector : tags) {
        Elements elements = document.select(selector);
        for (Element element : elements) {
          // Only add text if the element is not inside an already selected one
          if (!isNestedInsideAnotherSelected(element, selectedElements)) {
            collectedText.append(element.text()).append(" ");
            selectedElements.add(element);
          }
        }
      }
    } else {
      collectedText.append(document.text());
    }

    return collectedText.toString().trim();
  }

  /**
   * Extracts and returns the HTML content of specific elements from an HTML document based on given tags. If no tags are
   * provided, returns the entire raw HTML content of the document.
   *
   * @param document the HTML document to extract content from
   * @param tags     a list of CSS selectors specifying which elements to extract; if null or empty, extracts the full document's
   *                 HTML
   * @return a String containing the concatenated HTML content of the matching elements, or the full document's HTML if no tags
   * are provided
   */
  private static String getPageRawHtmlContent(Document document, List<String> tags) {
    StringBuilder collectedHtml = new StringBuilder();
    Set<Element> selectedElements = new HashSet<>();

    if (tags != null && !tags.isEmpty()) {
      for (String selector : tags) {
        Elements elements = document.select(selector);
        for (Element element : elements) {
          // Only add the element if it's not inside an already selected one
          if (!isNestedInsideAnotherSelected(element, selectedElements)) {
            collectedHtml.append(element.outerHtml()).append("\n");
            selectedElements.add(element);
          }
        }
      }
    } else {
      collectedHtml.append(document.html());
    }

    return collectedHtml.toString().trim();
  }

  private static boolean isNestedInsideAnotherSelected(Element element, Set<Element> selectedElements) {
    // Check if the element is inside any of the already selected elements (by checking parent hierarchy)
    for (Element selected : selectedElements) {
      if (isDescendant(selected, element)) {
        return true; // This element is inside an already selected parent
      }
    }
    return false;
  }

  private static boolean isDescendant(Element parent, Element element) {
    // Check if the element is a descendant of the parent element by traversing its parents
    for (Element e : element.parents()) {
      if (e == parent) {
        return true;
      }
    }
    return false;
  }

  public static String savePageContents(JSONObject results, String downloadPath, String title) throws IOException {

    String pageContents = results.toString();

    String fileName = "";

    // Generate a unique filename using the current timestamp
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());

    // Create a unique filename based on the sanitized title
    fileName = Utils.getSanitizedFilename(title) + "_" + timestamp + ".json";

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

  public static JSONArray downloadWebsiteImages(Document document, String saveDirectory, int maxNumber) throws IOException {
    return downloadWebsiteImages(document, saveDirectory, "", maxNumber);
  }

  public static JSONArray downloadWebsiteImages(
      Document document,
      String saveDirectory,
      String imagesSubFolder,
      int maxNumber) throws IOException {

    JSONArray imagesJSONArray = new JSONArray();

    // List to store image URLs
    Set<String> imageUrls = new HashSet<>();
    Map<String, Object> linksMap = (Map<String, Object>) PageHelper
        .getPageInsights(document, null, Constants.PageInsightType.IMAGELINKS).get("links");
    if (linksMap != null) {
      imageUrls = (Set<String>) linksMap.get("images"); // Cast to Set<String>
    }

    if (imageUrls != null) {

      // Save all images found on the page
      LOGGER.info("Number of img[src] elements found : " + imageUrls.size());
      for (String imageUrl : imageUrls) {
        JSONObject imageJSONObject = downloadSingleImage(imageUrl, saveDirectory, imagesSubFolder);
        if(imageJSONObject != null) imagesJSONArray.put(imageJSONObject);
        if(maxNumber>0 && imagesJSONArray.length() >= maxNumber) break;
      }
    }

    return imagesJSONArray;
  }

  public static JSONObject downloadSingleImage(String imageUrl, String saveDirectory) throws IOException {

    return downloadSingleImage(imageUrl, saveDirectory, "");
  }

  public static JSONObject downloadSingleImage(String imageUrl, String saveDirectory, String imagesSubFolder) throws IOException {

    LOGGER.info("Processing image: " + imageUrl);

    String imagesSaveDirectory = saveDirectory + "/" + imagesSubFolder;

    JSONObject jsonObject = new JSONObject();
    File file;

    try {

      jsonObject.put("url", imageUrl);
      if(imagesSubFolder.compareTo("") != 0) jsonObject.put("relativePath", imagesSubFolder);

      // Check if the URL is a Data URL
      if (imageUrl.startsWith("data:image/")) {
        // Extract base64 data from the Data URL
        String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);

        if (base64Data.isEmpty()) {
          LOGGER.info("Base64 data is empty for URL: " + imageUrl);
          return null;
        }

        // Decode the base64 data
        byte[] imageBytes;
        try {
          imageBytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
          LOGGER.info("Error decoding base64 data: " + e.getMessage());
          return null;
        }

        if (imageBytes.length == 0) {
          LOGGER.info("Decoded image bytes are empty for URL: " + imageUrl);
          return null;
        }

        // Determine the MIME type and file extension from the Data URL
        String mimeType = imageUrl.substring(5, imageUrl.indexOf(";"));
        String fileExtension = mimeType.split("/")[1];

        // Generate a unique filename using the current timestamp
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String fileName = "image_" + timestamp + "." + fileExtension;
        file = new File(imagesSaveDirectory, fileName);

        // Ensure the directory exists
        file.getParentFile().mkdirs();

        // Write the decoded bytes to the file
        try (FileOutputStream out = new FileOutputStream(file)) {
          out.write(imageBytes);
          LOGGER.info("Data URL image saved: " + file.getAbsolutePath());
        }

        jsonObject.put("fileName", fileName);
        jsonObject.put("mimeType", mimeType);

      } else {
        // Handle standard image URLs
        URL url = new URL(imageUrl);

        // Extract the 'url' parameter from the query string
        String decodedUrl = URLUtils.extractAndDecodeUrl(imageUrl);
        // Extract the filename from the decoded URL
        String fileName = URLUtils.extractFileNameFromUrl(decodedUrl);
        String mimeType = URLUtils.detectMimeTypeFromFileName(fileName);

        file = new File(imagesSaveDirectory, fileName);

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

        LOGGER.debug("Image saved: " + file.getAbsolutePath());

        jsonObject.put("fileName", fileName);
        jsonObject.put("mimeType", mimeType);
      }
    } catch (IOException e) {
      LOGGER.error("Error saving image: " + imageUrl, e);
      return null;
    }

    return jsonObject;
  }

  public static JSONArray downloadFiles(Document document, String saveDir, int maxNumber) throws IOException {

    return downloadFiles(document, saveDir, "", maxNumber);
  }

  public static JSONArray downloadFiles(Document document, String saveDir, String filesSubFolder, int maxNumber) throws IOException {

    JSONArray documentsJSONArray = new JSONArray();

    // List to store image URLs
    Set<String> documentURLs = new HashSet<>();

    Map<String, String> linkFileMap = new HashMap<>();
    Map<String, Object> linksMap = (Map<String, Object>) PageHelper
        .getPageInsights(document, null, Constants.PageInsightType.DOCUMENTLINKS).get("links");
    if (linksMap != null) {
      documentURLs = (Set<String>) linksMap.get("documents"); // Cast to Set<String>
    }

    if (documentURLs != null) {

      // Save all images found on the page
      LOGGER.debug("Number of documents found : " + documentURLs.size());
      for (String documentURL : documentURLs) {

        JSONObject documentJSONObject = downloadFile(documentURL, saveDir, filesSubFolder);
        if(documentJSONObject != null) documentsJSONArray.put(documentJSONObject);
        if(maxNumber>0 && documentsJSONArray.length() >= maxNumber) break;
      }
    }
    return documentsJSONArray;
  }

  public static JSONObject downloadFile(String fileURL, String saveDir) {

    return downloadFile(fileURL, saveDir, "");
  }

  public static JSONObject downloadFile(String fileURL, String saveDir, String filesSubFolder) {
    String docsSaveDirectory = saveDir + "/" + filesSubFolder;
    HttpURLConnection httpConn = null;
    String fileName = null;
    JSONObject jsonObject = new JSONObject();

    try {

      jsonObject.put("url", fileURL);
      if(filesSubFolder.compareTo("") != 0) jsonObject.put("relativePath", filesSubFolder);

      // Open connection to the URL
      URL url = new URL(fileURL);
      httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod("GET");

      // Check HTTP response code
      int responseCode = httpConn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        String disposition = httpConn.getHeaderField("Content-Disposition");

        if (disposition != null && disposition.contains("filename=")) {
          // Extracts file name from header field
          int index = disposition.indexOf("filename=");
          fileName = disposition.substring(index + 9).replaceAll("\"", "");
        } else {
          // Fallback: extract file name from URL without query parameters
          String urlPath = fileURL.split("\\?")[0]; // Remove query parameters
          fileName = urlPath.substring(urlPath.lastIndexOf("/") + 1);
        }

        // Decode the file name
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());

        LOGGER.debug(String.format("Downloading file %s at %s", fileName, fileURL));

        // Ensure directory exists
        File directory = new File(docsSaveDirectory);
        if (!directory.exists()) {
          if (directory.mkdirs()) {
            LOGGER.debug("Directory created: " + directory.getAbsolutePath());
          } else {
            LOGGER.error("Failed to create directory: " + directory.getAbsolutePath());
            return null;
          }
        }

        // Open input stream from connection
        try (InputStream inputStream = new BufferedInputStream(httpConn.getInputStream());
            FileOutputStream outputStream = new FileOutputStream(docsSaveDirectory + fileName)) {

          // Buffer for data transfer
          byte[] buffer = new byte[4096];
          int bytesRead;

          // Write data to the file
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }

          LOGGER.debug("File downloaded: " + docsSaveDirectory + fileName);
        }
      } else {
        LOGGER.debug("No file to download. Server replied HTTP code: " + responseCode);
      }

    } catch (IOException e) {
      LOGGER.error("Error downloading file: " + e.getMessage());
    } finally {
      if (httpConn != null) {
        httpConn.disconnect();
      }
    }

    // Prepare and return the result as JSONObject
    if (fileName != null) {

      jsonObject.put("fileName", fileName);
      String mimeType = URLUtils.detectMimeTypeFromFileName(fileName);
      jsonObject.put("mimeType", mimeType);

    } else {
      return null;
    }

    return jsonObject;
  }

  public static boolean canCrawl(String url, String userAgent) {

    String robotsTxtContent = getRobotsTxt(url);
    if (robotsTxtContent == null) {
      return true; // If robots.txt cannot be retrieved, assume crawling is allowed
    }

    String baseUrl;
    try {
      URL base = new URL(url);
      baseUrl = base.getProtocol() + "://" + base.getHost();
    } catch (MalformedURLException e) {
      LOGGER.error("Invalid URL: " + url, e);
      return false;
    }

    // Parse robots.txt content
    String[] lines = robotsTxtContent.split("\n");
    boolean userAgentMatched = false;
    boolean isAllowed = true;

    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue; // Skip empty lines and comments
      }

      if (line.toLowerCase().startsWith("user-agent:")) {
        String agent = line.substring(11).trim();
        userAgentMatched = agent.equals("*") || agent.equalsIgnoreCase(userAgent);
      } else if (userAgentMatched) {
        if (line.toLowerCase().startsWith("disallow:")) {
          String path = line.substring(9).trim();
          if (path.isEmpty() || url.startsWith(baseUrl + path)) {
            isAllowed = false;
            break;
          }
        } else if (line.toLowerCase().startsWith("allow:")) {
          String path = line.substring(6).trim();
          if (url.startsWith(baseUrl + path)) {
            isAllowed = true;
            break;
          }
        }
      }
    }

    return isAllowed;
  }

  public static String getRobotsTxt(String url) {
    try {
      URL baseUrl = new URL(url);
      String baseUrlString = baseUrl.getProtocol() + "://" + baseUrl.getHost();

      // Check if the robots.txt content is already cached
      if (robotsTxtCache.containsKey(baseUrlString)) {
        return robotsTxtCache.get(baseUrlString);
      }

      String robotsTxtUrl = baseUrlString + "/robots.txt";
      Document document = Jsoup.connect(robotsTxtUrl).get();
      String robotsTxtContent = Jsoup.connect(robotsTxtUrl)
          .ignoreContentType(true) // Ensures it handles plain text
          .execute()
          .body();

      LOGGER.debug("Retrieved robots.txt content:\n\n " + robotsTxtContent + "\n\n");

      // Cache the robots.txt content
      robotsTxtCache.put(baseUrlString, robotsTxtContent);

      return robotsTxtContent;

    } catch (Exception e) {

      LOGGER.debug("Error retrieving robots.txt from " + url, e);
      return null;
    }
  }
}
