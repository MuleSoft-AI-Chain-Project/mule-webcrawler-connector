package org.mule.extension.webcrawler.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum WebCrawlerErrorType implements ErrorTypeDefinition<WebCrawlerErrorType>  {
  INVALID_PARAMETERS_ERROR,
  WEBCRAWLER_OPERATIONS_FAILURE,
  SEARCH_OPERATIONS_FAILURE,
  PAGE_OPERATIONS_FAILURE,
  JAVASCRIPT_EXECUTOR_FAILURE,
  DOWNLOAD_DOCUMENTS_OPERATION_FAILURE,
  CRAWL_OPERATIONS_FAILURE,
  CRAWL_ON_PAGE_DISALLOWED_ERROR
}
