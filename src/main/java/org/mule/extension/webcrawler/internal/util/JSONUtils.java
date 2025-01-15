package org.mule.extension.webcrawler.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {

  public static String convertToJSON(Object contentToSerialize) throws JsonProcessingException {

    // Convert the result to JSON
    ObjectMapper mapper = new ObjectMapper();
    //return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(contentToSerialize);
    return mapper.writeValueAsString(contentToSerialize);
  }
}
