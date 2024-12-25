package org.mule.extension.webcrawler.internal.error.provider;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType.INVALID_PARAMETERS_ERROR;
import static org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType.WEBCRAWLER_OPERATIONS_FAILURE;

public class WebCrawlerErrorTypeProvider implements ErrorTypeProvider {

  @SuppressWarnings("rawtypes")
  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return unmodifiableSet(new HashSet<>(asList(
        INVALID_PARAMETERS_ERROR,
        WEBCRAWLER_OPERATIONS_FAILURE
    )));
  }
}
