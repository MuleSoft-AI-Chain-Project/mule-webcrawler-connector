package org.mule.extension.webcrawler.internal.metadata;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.json.api.JsonTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.io.InputStream;
import java.util.Optional;

public class CrawlWebSiteStreamingOutputTypeMetadataResolver implements OutputTypeResolver<Object> {

  @Override
  public String getCategoryName() {
    return "page";
  }

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, Object key)
      throws MetadataResolvingException, ConnectionException {

    InputStream resourceAsStream = Thread.currentThread()
        .getContextClassLoader()
        .getResourceAsStream("api/metadata/CrawlWebSiteStreaming.json");

    Optional<MetadataType> metadataType = new JsonTypeLoader(IOUtils.toString(resourceAsStream))
        .load(null, "Load Crawler Response");

    return metadataType.orElse(null);
  }
}
