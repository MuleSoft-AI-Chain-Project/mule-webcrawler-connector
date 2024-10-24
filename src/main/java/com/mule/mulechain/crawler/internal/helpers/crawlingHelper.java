package com.mule.mulechain.crawler.internal.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class crawlingHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(crawlingHelper.class);

    public enum PageInsightType {
        ALL,
        INTERNALLINKS,
        EXTERNALLINKS,
        REFERENCELINKS,
        IMAGELINKS,
        ELEMENTCOUNTSTATS
    }


    public static Document getDocument(String url) throws IOException {
        // use jsoup to fetch the current page elements
        Document document = Jsoup.connect(url)
                //.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                //.referrer("http://www.google.com")  // to prevent "HTTP error fetching URL. Status=403" error
                .get();

        return document;
    }


    public static String extractFileNameFromUrl(String url) {
        // Extract the filename from the URL path
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.indexOf('?') > 0 ? url.indexOf('?') : url.length());

        // if no extension for image found, then use .jpg as default
        return fileName.contains(".") ? fileName : fileName + ".jpg";
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


    public static String convertToJSON(Object contentToSerialize) throws JsonProcessingException{
        // Convert the result to JSON
        ObjectMapper mapper = new ObjectMapper();
        //return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(contentToSerialize);
        return mapper.writeValueAsString(contentToSerialize);
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

    public static Map<String, Object> getPageInsights(Document document, List<String> tags, PageInsightType insight) throws MalformedURLException{
        // Map to store page analysis
        Map<String, Object> pageInsightData = new HashMap<>();


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

        if (insight == PageInsightType.ALL || insight == PageInsightType.INTERNALLINKS || insight == PageInsightType.REFERENCELINKS || insight == PageInsightType.EXTERNALLINKS) {
            // Select all anchor tags with href attributes
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String href = link.absUrl("href"); // get absolute URLs
                if (isExternalLink(baseUrl, href)) {
                    externalLinks.add(href);
                } else if (isReferenceLink(baseUrl, href)) {
                    referenceLinks.add(href);
                } else {
                    internalLinks.add(href);
                }
            }

            if (insight == PageInsightType.ALL || insight == PageInsightType.INTERNALLINKS)
                linksMap.put("internal", internalLinks);
            if (insight == PageInsightType.ALL || insight == PageInsightType.EXTERNALLINKS)
                linksMap.put("external", externalLinks);
            if (insight == PageInsightType.ALL || insight == PageInsightType.REFERENCELINKS)
                linksMap.put("reference", referenceLinks);
        }


        if (insight == PageInsightType.ALL || insight == PageInsightType.IMAGELINKS) {
                // images

            Elements images = document.select("img[src]");
            for (Element img : images) {
                String imageUrl = img.absUrl("src");
                imageLinks.add(imageUrl);
            }

            linksMap.put("images", imageLinks);

        }

        if (insight == PageInsightType.ALL || insight == PageInsightType.ELEMENTCOUNTSTATS) {
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
            elementCounts.put("wordCount", countWords(getPageContent(document,tags)));

            pageInsightData.put("pageStats", elementCounts);
        }

        pageInsightData.put("url", document.baseUri());
        pageInsightData.put("title", document.title());

        // only add links if any of the types in condition has been requested
        if (insight == PageInsightType.ALL || insight == PageInsightType.INTERNALLINKS || insight == PageInsightType.REFERENCELINKS || insight == PageInsightType.EXTERNALLINKS || insight == PageInsightType.IMAGELINKS)
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

    // Method to count words in a given text
    private static int countWords(String text) {
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

    // Method to determine if a link is a reference link to the same page
    // baseUrl: "https://docs.mulesoft.com/cloudhub-2/ch2-architecture"
    // linkToCheck: "https://docs.mulesoft.com/cloudhub-2/ch2-architecture#cluster-nodes"
    // If current page has a reference link to another page, this link will not be considered as a reference link
    private static boolean isReferenceLink(String baseUrl, String linkToCheck) {
        try {
            URI baseUri = new URI(baseUrl);
            URI linkUri = new URI(linkToCheck);

            // Check if the scheme, host, and path are the same, and the link has a fragment
            return baseUri.getScheme().equals(linkUri.getScheme()) &&
                    baseUri.getHost().equals(linkUri.getHost()) &&
                    baseUri.getPath().equals(linkUri.getPath()) &&
                    linkUri.getFragment() != null;

        } catch (URISyntaxException e) {
            LOGGER.error(e.toString());
            return false;
        }
    }

    private static boolean isExternalLink(String baseUrl, String linkToCheck) throws MalformedURLException {
        // Extract the base domain from the base URI
        URL parsedUrl = new URL(baseUrl);
        String baseDomain = parsedUrl.getHost();

        return !linkToCheck.contains(baseDomain);

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
}
