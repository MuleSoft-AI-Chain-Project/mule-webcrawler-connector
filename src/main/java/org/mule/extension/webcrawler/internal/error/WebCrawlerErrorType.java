package org.mule.extension.webcrawler.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum WebCrawlerErrorType implements ErrorTypeDefinition<WebCrawlerErrorType>  {
  INVALID_PARAMETERS_ERROR,
  WEBCRAWLER_OPERATIONS_FAILURE,
  SEARCH_OPERATIONS_FAILURE,
  PAGE_OPERATIONS_FAILURE,
  CRAWL_OPERATIONS_FAILURE
}
