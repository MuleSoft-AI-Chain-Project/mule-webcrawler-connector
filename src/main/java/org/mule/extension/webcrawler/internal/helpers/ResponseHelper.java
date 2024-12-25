package org.mule.extension.webcrawler.internal.helpers;

import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.IOUtils.toInputStream;

public class ResponseHelper {

  public static Result<InputStream, ResponseAttributes> createResponse(
      String response,
      Map<String, Object> documentAttributes) {

    return Result.<InputStream, ResponseAttributes>builder()
        .attributes(new ResponseAttributes((HashMap<String, Object>) documentAttributes))
        .attributesMediaType(MediaType.APPLICATION_JAVA)
        .output(toInputStream(response, StandardCharsets.UTF_8))
        .mediaType(MediaType.APPLICATION_JSON)
        .build();
  }
}
