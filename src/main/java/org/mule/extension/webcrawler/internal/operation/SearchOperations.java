package org.mule.extension.webcrawler.internal.operation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.internal.config.Configuration;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.extension.webcrawler.internal.crawler.Crawler;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.extension.webcrawler.internal.error.provider.WebCrawlerErrorTypeProvider;
import org.mule.extension.webcrawler.internal.helper.ResponseHelper;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.helper.search.SerperDev;
import org.mule.extension.webcrawler.internal.util.JSONUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class SearchOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchOperations.class);

  /**
   * Perform a Google search using the SERP API.
   *
   * @throws IOException
   */
  @MediaType(value = MediaType.APPLICATION_JSON, strict = false)
  @Alias("search-google")
  @DisplayName("[Search] Google")
  @Throws(WebCrawlerErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/SearchGoogle.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, ResponseAttributes>
      googleSearch(
          @DisplayName("Search Query") @Placement(order = 1) @Example("apple inc") String query,
          @DisplayName("API Key") @Placement(order = 2) @Example("your_api_key_here") String apiKey) throws IOException {

    try {

      LOGGER.info("Performing Google search for query: " + query);

      String responseBody = SerperDev.search(query, apiKey);

      JSONObject jsonResponse = new JSONObject(responseBody);

      return ResponseHelper.createResponse(
          jsonResponse.toString(),
          new HashMap<String, Object>() {{
            put("query", query);
          }}
      );

    } catch (ModuleException me) {

      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while searching '%s'.", query),
          WebCrawlerErrorType.SEARCH_OPERATIONS_FAILURE,
          e);
    }
  }
}
