package org.mule.extension.webcrawler.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class JSONUtils {

  public static String convertToJSON(Object contentToSerialize) throws JsonProcessingException {

    // Convert the result to JSON
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY);
    return mapper.writeValueAsString(contentToSerialize);
  }
}
