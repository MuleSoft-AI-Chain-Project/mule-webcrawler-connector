package org.mule.extension.webcrawler.internal.util;

import org.mule.extension.webcrawler.internal.html2markdown.HtmlToMarkdownConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  // Should be thread-safe as no state is maintained in any of the converter classes
  private static final HtmlToMarkdownConverter htmlToMarkdownConverter = new HtmlToMarkdownConverter(100);

  // Method to count words in a given text
  public static int countWords(String text) {

    if (text == null || text.trim().isEmpty()) {
      return 0;
    }
    // Split the text by whitespace and count the words
    String[] words = text.trim().split("\\s+");
    return words.length;
  }

  public static String getSanitizedFilename(String title) {

    // Replace invalid characters with underscores
    return title.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll(" ", "");
  }

  public static void addDelay(int delayMillis) {

    // add a delay if specified delay is >0 millisecs
    if (delayMillis > 0) {
      // Add delay before fetching the next URL
      try {
        LOGGER.info("Adding delay of " + delayMillis + " ms before fetching contents for the next URL.");
        Thread.sleep(delayMillis);
      } catch (InterruptedException e) {
        LOGGER.error("Thread interrupted during delay: " + e.getMessage());
        Thread.currentThread().interrupt(); // Ensure thread interruption status is reset
      }
    }
  }

  public static String convertHtmlToMarkdown(String html) {
    return htmlToMarkdownConverter.convert(html);
  }
}
