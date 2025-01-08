package org.mule.extension.webcrawler.internal.helper.page;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.helper.crawler.CrawlerHelper;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.extension.webcrawler.internal.util.URLUtils;
import org.mule.extension.webcrawler.internal.util.Utils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class PageHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageHelper.class);

  public static Document getDocument(String url) throws IOException {
    // use jsoup to fetch the current page elements
    Document document = Jsoup.connect(url)
        //.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
        //.referrer("http://www.google.com")  // to prevent "HTTP error fetching URL. Status=403" error
        .get();

    return document;
  }

  public static Document getDocumentDynamic(String url) throws Exception {

    Document document = null;
    // Set ChromeOptions to enable headless mode
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless"); // Run in headless mode
    options.addArguments("--disable-gpu"); // Disable GPU acceleration (optional)
    options.addArguments("--no-sandbox"); // Recommended for headless mode in Docker or CI environments
    options.addArguments("--disable-dev-shm-usage"); // Recommended for limited resources
    options.addArguments("--allow-running-insecure-content"); // Allow HTTP content on HTTPS pages
    // Initialize WebDriver
    WebDriver driver = new ChromeDriver(options);


    try {

      // Load the dynamic page
      driver.get(url);
      // Retrieve the page source and parse with Jsoup
      String pageSource = driver.getPageSource();
      document = Jsoup.parse(pageSource, url);
    }
    catch (Exception e) {
      LOGGER.error("Error in loading dynamic content: " + e.toString());
      throw e;
    }
    finally {
      driver.quit();
    }

    return document;
  }

  public static Map<String, String> getPageMetaTags(Document document) {
    // Map to store meta tag data
    Map<String, String> metaTagData = new HashMap<>();

    // Select all meta tags
    Elements metaTags = document.select("meta");

    // Iterate through each meta tag
    for (Element metaTag : metaTags) {
      // Extract the 'name' or 'property' attribute and 'content' attribute
      String name = metaTag.attr("name");
      if (name.isEmpty()) {
        // If 'name' is not present, check for 'property' (e.g., Open Graph meta tags)
        name = metaTag.attr("property");
      }
      String content = metaTag.attr("content");

      // Only add to map if 'name' or 'property' and 'content' are present
      if (!name.isEmpty() && !content.isEmpty()) {
        metaTagData.put(name, content);
      }
    }
    return metaTagData;
  }

  public static Map<String, Object> getPageInsights(Document document, List<String> tags, Constants.PageInsightType insight) throws
      MalformedURLException {
    // Map to store page analysis
    Map<String, Object> pageInsightData = new HashMap<>();


    // doc-link set
    Set<String> documentLinks = new HashSet<>();

    // links set
    Set<String> internalLinks = new HashSet<>();
    Set<String> externalLinks = new HashSet<>();
    Set<String> referenceLinks = new HashSet<>();

    // image-links set
    Set<String> imageLinks = new HashSet<>();

    // All links Map
    Map<String, Set> linksMap = new HashMap<>();

    // Map to store the element counts
    Map<String, Integer> elementCounts = new HashMap<>();


    String baseUrl = document.baseUri();


    if (insight == Constants.PageInsightType.ALL ||
        insight == Constants.PageInsightType.DOCUMENTLINKS ||
        insight == Constants.PageInsightType.INTERNALLINKS ||
        insight == Constants.PageInsightType.REFERENCELINKS ||
        insight == Constants.PageInsightType.EXTERNALLINKS) {

      // Select all anchor tags with href attributes
      Elements links = document.select("a[href]");

      for (Element link : links) {
        String href = link.absUrl("href"); // get absolute URLs

        if(URLUtils.isDocumentUrl(href)) {
          documentLinks.add(href);
        } else if (URLUtils.isExternalLink(baseUrl, href)) {
          externalLinks.add(href);
        } else if (URLUtils.isReferenceLink(baseUrl, href)) {
          referenceLinks.add(href);
        } else {
          internalLinks.add(href);
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


    if (insight == Constants.PageInsightType.ALL || insight == Constants.PageInsightType.IMAGELINKS) {
      // images

      Elements images = document.select("img[src]");
      for (Element img : images) {
        String imageUrl = img.absUrl("src");
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
      elementCounts.put("images", imageLinks.size());
      elementCounts.put("wordCount", Utils.countWords(getPageContent(document, tags)));

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
        insight == Constants.PageInsightType.IMAGELINKS)

      pageInsightData.put("links", linksMap);

    return pageInsightData;
  }

  public static String getPageContent(Document document, List<String> tags) {

    StringBuilder collectedText = new StringBuilder();

    // check if crawl should only iterate over specified tags and extract contents from these tags only
    if (tags != null && !tags.isEmpty()) {
      for (String selector : tags) {
        Elements elements = document.select(selector);
        for (Element element : elements) {
          collectedText.append(element.text()).append(" ");
        }
      }
    }
    else {
      // Extract the text content of the page and add it to the collected text
      String textContent = document.text();
      collectedText.append(textContent);
    }

    return collectedText.toString().trim();
  }

  public static String savePageContents(Object results, String downloadPath, String title) throws IOException {

    String pageContents = JSONUtils.convertToJSON(results);

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

  public static Map<String, String> downloadWebsiteImages(Document document, String saveDirectory) throws IOException {
    // List to store image URLs
    Set<String> imageUrls = new HashSet<>();

    Map<String, String> linkFileMap = new HashMap<>();

    Map<String, Object> linksMap = (Map<String, Object>) PageHelper
        .getPageInsights(document, null, Constants.PageInsightType.IMAGELINKS).get("links");
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

  public static String downloadSingleImage(String imageUrl, String saveDirectory) throws IOException {
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
        String decodedUrl = URLUtils.extractAndDecodeUrl(imageUrl);
        // Extract the filename from the decoded URL
        String fileName = URLUtils.extractFileNameFromUrl(decodedUrl);

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
}
