package org.mule.extension.webcrawler.internal.helper;

import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

  public static List<Result<CursorProvider, ResponseAttributes>> createPageResponse(
      String response,
      Map<String, Object> documentAttributes,
      StreamingHelper streamingHelper) {

    List<Result<CursorProvider, ResponseAttributes>> page =  new LinkedList();

    page.add(Result.<CursorProvider, ResponseAttributes>builder()
                 .attributes(new ResponseAttributes((HashMap<String, Object>) documentAttributes))
                 .output((CursorProvider) streamingHelper.resolveCursorProvider(toInputStream(response, StandardCharsets.UTF_8)))
                 .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
                 .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
                 .build());

    return page;
  }
}
