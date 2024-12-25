package org.mule.extension.webcrawler.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum WebCrawlerErrorType implements ErrorTypeDefinition<WebCrawlerErrorType>  {
  INVALID_PARAMETERS_ERROR,
  WEBCRAWLER_OPERATIONS_FAILURE
}
