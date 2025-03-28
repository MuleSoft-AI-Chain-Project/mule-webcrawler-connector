package org.mule.extension.webcrawler.internal.util;

import org.mule.extension.webcrawler.internal.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class URLUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(URLUtils.class);

  private static final Set<String> VALID_EXTENSIONS;
  private static final int MAX_EXTENSION_LENGTH = 5; // longest extension is "xlsx"/"pptx"
  private static final Map<String, String> MIME_TYPES = new HashMap<>();

  static {
    Set<String> extensions = new HashSet<>();
    for (Constants.DocumentExtension ext : Constants.DocumentExtension.values()) {
      extensions.add(ext.name().toLowerCase(Locale.ENGLISH));
    }
    VALID_EXTENSIONS = Collections.unmodifiableSet(extensions);
  }

  static {
    // Common file extension to MIME type mappings
    MIME_TYPES.put("jpg", "image/jpeg");
    MIME_TYPES.put("jpeg", "image/jpeg");
    MIME_TYPES.put("png", "image/png");
    MIME_TYPES.put("gif", "image/gif");
    MIME_TYPES.put("bmp", "image/bmp");
    MIME_TYPES.put("webp", "image/webp");
    MIME_TYPES.put("svg", "image/svg+xml");
    MIME_TYPES.put("pdf", "application/pdf");
    MIME_TYPES.put("txt", "text/plain");
    MIME_TYPES.put("html", "text/html");
    MIME_TYPES.put("xml", "application/xml");
    MIME_TYPES.put("json", "application/json");
    MIME_TYPES.put("mp4", "video/mp4");
    MIME_TYPES.put("mp3", "audio/mpeg");
    MIME_TYPES.put("wav", "audio/wav");
    MIME_TYPES.put("zip", "application/zip");
    MIME_TYPES.put("rar", "application/vnd.rar");
    MIME_TYPES.put("7z", "application/x-7z-compressed");
    // Add more mappings as needed
  }

  /**
   * Checks if the provided URL string points to a valid document.
   *
   * @param url The URL string to check
   * @return true if the URL points to a supported document type, false otherwise
   */
  public static boolean isDocumentUrl(String url) {
    if (url == null || url.isEmpty()) {
      return false;
    }

    try {
      // Remove query parameters and fragments from the URL
      String cleanUrl = url.split("[?#]")[0].toLowerCase(Locale.ENGLISH);

      // Basic URL validation
      if (cleanUrl.length() < 2) { // minimum valid case: "a.b"
        return false;
      }

      // Check if the URL has a file extension
      int lastDotIndex = cleanUrl.lastIndexOf('.');
      if (lastDotIndex == -1 || lastDotIndex == cleanUrl.length() - 1) {
        return false;
      }

      // Validate extension length
      String fileExtension = cleanUrl.substring(lastDotIndex + 1);
      if (fileExtension.length() > MAX_EXTENSION_LENGTH) {
        return false;
      }

      // Check for invalid characters in extension
      if (!fileExtension.matches("^[a-z0-9]+$")) {
        return false;
      }

      return VALID_EXTENSIONS.contains(fileExtension);

    } catch (Exception e) {
      // Catch all exceptions for robustness
      return false;
    }
  }

  // Method to determine if a link is a reference link to the same page
  // baseUrl: "https://docs.mulesoft.com/cloudhub-2/ch2-architecture"
  // linkToCheck: "https://docs.mulesoft.com/cloudhub-2/ch2-architecture#cluster-nodes"
  // If current page has a reference link to another page, this link will not be considered as a reference link
  public static boolean isReferenceLink(String baseUrl, String linkToCheck) {
    try {
      URI baseUri = URI.create(baseUrl);
      URI linkUri = URI.create(linkToCheck);

      // Check if the scheme, host, and path are the same, and the link has a fragment
      return baseUri.getScheme().equals(linkUri.getScheme()) &&
              baseUri.getHost().equals(linkUri.getHost()) &&
              baseUri.getPath().equals(linkUri.getPath()) &&
              linkUri.getFragment() != null;

    } catch (IllegalArgumentException e) {
        LOGGER.error("Invalid URL: {}", e.getMessage());
      return false;
    }
  }

  public static boolean isExternalLink(String baseUrl, String linkToCheck) throws MalformedURLException {
    // Extract the base domain from the base URI
    URL parsedUrl = new URL(baseUrl);
    String baseDomain = parsedUrl.getHost();

    return !linkToCheck.contains(baseDomain);

  }

  /*
          "https://wp.salesforce.com/en-ap/wp-content/uploads/sites/14/2024/02/php-marquee-starter-lg-bg.jpg?w=1024",
        "https://example.com/image?url=%2F_next%2Fstatic%2Fmedia%2Fcard-1.8b03e519.png&w=3840&q=75"
*/
  public static String extractAndDecodeUrl(String fullUrl) throws UnsupportedEncodingException, MalformedURLException {

    URL url = new URL(fullUrl);
    String query = url.getQuery(); // Extract the query string from the URL

    if (query != null) {
      // Extract and decode the 'url' parameter from the query string
      String[] params = query.split("&");
      for (String param : params) {
        String[] pair = param.split("=");
        if (pair.length == 2 && "url".equals(pair[0])) {
          return URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
        }
      }
      // If 'url' parameter not found, return the URL without changes
      return fullUrl;
    } else {
      // If there's no query string, return the URL as is
      return fullUrl;
    }
  }

  public static String extractFileNameFromUrl(String url) {
    // Extract the filename from the URL path
    String fileName = url.substring(url.lastIndexOf("/") + 1, url.indexOf('?') > 0 ? url.indexOf('?') : url.length());

    // if no extension for image found, then use .jpg as default
    return fileName.contains(".") ? fileName : fileName + ".jpg";
  }

  /**
   * Detects the MIME type of a file from its URL based on the file extension.
   *
   * @param fileName the filename from the URL path
   * @return The MIME type as a string (e.g., "image/jpeg"), or "application/octet-stream" if undetectable.
   */
  public static String detectMimeTypeFromFileName(String fileName) {

    String extension = getFileExtension(fileName);
    return MIME_TYPES.getOrDefault(extension.toLowerCase(), "application/octet-stream");
  }

  /**
   * Extracts the file extension from a URL path.
   *
   * @param path The URL path.
   * @return The file extension (e.g., "jpg"), or an empty string if no extension is found.
   */
  private static String getFileExtension(String path) {
    int lastDotIndex = path.lastIndexOf('.');
    if (lastDotIndex != -1 && lastDotIndex < path.length() - 1) {
      return path.substring(lastDotIndex + 1);
    }
    return "";
  }

  /**
   * Clean a URL.
   *
   * @param url The URL string.
   * @return The URL without the fragment part.
   */
  public static String cleanURL(String url) {

    try {
      URI uri = new URI(url);
      return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null).toString();

    } catch (URISyntaxException e) {

      throw new IllegalArgumentException("Invalid URL: " + url, e);
    }
  }
}
