package org.mule.extension.webcrawler.internal.helper;

import org.mule.extension.webcrawler.api.metadata.PageResponseAttributes;
import org.mule.extension.webcrawler.api.metadata.ResponseAttributes;
import org.mule.extension.webcrawler.api.metadata.SearchResponseAttributes;
import org.mule.extension.webcrawler.api.metadata.SitemapResponseAttributes;
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
      String output,
      Map<String, Object> responseAttributes) {

    return Result.<InputStream, ResponseAttributes>builder()
        .attributes(new ResponseAttributes((HashMap<String, Object>) responseAttributes))
        .attributesMediaType(MediaType.APPLICATION_JAVA)
        .output(toInputStream(output, StandardCharsets.UTF_8))
        .mediaType(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Result<InputStream, PageResponseAttributes> createPageResponse(
      String output,
      Map<String, Object> pageAttributes) {

    return Result.<InputStream, PageResponseAttributes>builder()
        .attributes(new PageResponseAttributes((HashMap<String, Object>) pageAttributes))
        .attributesMediaType(MediaType.APPLICATION_JAVA)
        .output(toInputStream(output, StandardCharsets.UTF_8))
        .mediaType(MediaType.APPLICATION_JSON)
        .build();
  }

  public static Result<InputStream, SitemapResponseAttributes> createSitemapResponse(
      String output,
      Map<String, Object> sitemapAttributes) {

    return Result.<InputStream, SitemapResponseAttributes>builder()
        .attributes(new SitemapResponseAttributes((HashMap<String, Object>) sitemapAttributes))
        .attributesMediaType(MediaType.APPLICATION_JAVA)
        .output(toInputStream(output, StandardCharsets.UTF_8))
        .mediaType(MediaType.APPLICATION_XML)
        .build();
  }

  public static Result<InputStream, SearchResponseAttributes> createSearchResponse(
      String output,
      Map<String, Object> searchAttributes) {

    return Result.<InputStream, SearchResponseAttributes>builder()
        .attributes(new SearchResponseAttributes((HashMap<String, Object>) searchAttributes))
        .attributesMediaType(MediaType.APPLICATION_JAVA)
        .output(toInputStream(output, StandardCharsets.UTF_8))
        .mediaType(MediaType.APPLICATION_JSON)
        .build();
  }

  public static List<Result<CursorProvider, PageResponseAttributes>> createPageResponse(
      String response,
      Map<String, Object> documentAttributes,
      StreamingHelper streamingHelper) {

    List<Result<CursorProvider, PageResponseAttributes>> page =  new LinkedList();

    page.add(Result.<CursorProvider, PageResponseAttributes>builder()
                 .attributes(new PageResponseAttributes((HashMap<String, Object>) documentAttributes))
                 .output((CursorProvider) streamingHelper.resolveCursorProvider(toInputStream(response, StandardCharsets.UTF_8)))
                 .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
                 .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
                 .build());

    return page;
  }
}
