package org.mule.extension.webcrawler.internal.connection.http;

import org.mule.extension.webcrawler.internal.helper.provider.UserAgentNameProvider;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;

import javax.inject.Inject;

@Alias("http")
@DisplayName("HTTP")
public class HttpConnectionProvider implements CachedConnectionProvider<HttpConnection>, Startable, Stoppable {

  private HttpClient httpClient;

  @RefName
  private String configName;

  @Inject
  private HttpService httpService;

  @Parameter
  @Alias("userAgent")
  @DisplayName("User agent")
  @Summary("The request user-agent header.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(UserAgentNameProvider.class)
  @Optional
  private String userAgent;

  @Parameter
  @Alias("referrer")
  @DisplayName("Referrer")
  @Summary("The request referrer (aka \"referer\") header..")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("https://www.google.com")
  @Optional
  private String referrer;

  @Parameter
  @Placement(order = 2)
  @Optional(defaultValue = "10000")
  private int timeout;

  @Parameter
  @Placement(order = 2, tab = "Advanced")
  @Optional
  private TlsContextFactory tlsContext;

  @Override
  public HttpConnection connect() throws ConnectionException {
    return new HttpConnection(httpClient, timeout, userAgent, referrer);
  }

  @Override
  public void disconnect(HttpConnection httpConnection) {

  }

  @Override
  public ConnectionValidationResult validate(HttpConnection httpConnection) {
    try {
      httpConnection.validate();
      return ConnectionValidationResult.success();
    }
    catch (ConnectionException e) {
      return ConnectionValidationResult.failure(e.getMessage(), e);
    }
  }

  @Override
  public void start() throws MuleException {

    HttpClientConfiguration config = createClientConfiguration();
    httpClient = httpService.getClientFactory().create(config);
    httpClient.start();
  }

  private HttpClientConfiguration createClientConfiguration() {

    HttpClientConfiguration.Builder builder = new HttpClientConfiguration.Builder().setName(configName);
    if (null != tlsContext) {
      builder.setTlsContextFactory(tlsContext);
    } else {
      builder.setTlsContextFactory(TlsContextFactory.builder().buildDefault());
    }
    return builder.build();
  }

  @Override
  public void stop() throws MuleException {
    if (httpClient != null) {
      httpClient.stop();
    }
  }
}
