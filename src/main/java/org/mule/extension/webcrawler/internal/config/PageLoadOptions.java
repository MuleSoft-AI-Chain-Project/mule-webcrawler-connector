package org.mule.extension.webcrawler.internal.config;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Map;

public class PageLoadOptions {

  @Parameter
  @Alias("waitOnPageLoad")
  @DisplayName("Wait on page load (millisecs)")
  @Summary("The time to wait on page load (not available for HTTP connection)")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("1000")
  @Optional
  private Long waitOnPageLoad;

  @Parameter
  @Alias("waitForXPath")
  @DisplayName("Wait for XPath")
  @Summary("The XPath to wait for (not available for HTTP connection)")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("//body")
  @Optional
  private String waitForXPath;

  @Parameter
  @Alias("extractShadowDom")
  @DisplayName("Extract Shadow DOM")
  @Summary("Extract the Shadow DOM content (not available for HTTP connection)")
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "false")
  private boolean extractShadowDom;

  @Parameter
  @Alias("shadowHostXPath")
  @DisplayName("Shadow Host(s) XPath")
  @Summary("Shadow host(s) to extract by XPath (not available for HTTP connection)")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("//results")
  @Optional
  private String shadowHostXPath;

  @Parameter
  @Alias("authenticationMethodId")
  @DisplayName("Custom Authentication Method ID")
  @Summary("Custom Authentication Method ID (not available for HTTP connection)")
  @Expression(ExpressionSupport.SUPPORTED)
  @Example("customBasicAuth")
  @Optional
  private String authenticationMethodId;

  @Parameter
  @Alias("authenticationConfiguration")
  @DisplayName("Custom Authentication Configuration")
  @Summary("Custom Authentication Configuration (not available for HTTP connection)")
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private Map<String, String> authenticationConfiguration;

  private String javascript;

  public PageLoadOptions() {

  }

  public PageLoadOptions(Long waitOnPageLoad, String waitForXPath, boolean extractShadowDom, String shadowHostXPath,
                         String authenticationMethodId, Map<String, String> authenticationConfiguration) {
    this.waitOnPageLoad = waitOnPageLoad;
    this.waitForXPath = waitForXPath;
    this.extractShadowDom = extractShadowDom;
    this.shadowHostXPath = shadowHostXPath;
    this.authenticationMethodId = authenticationMethodId;
    this.authenticationConfiguration = authenticationConfiguration;
  }

  public PageLoadOptions(Long waitOnPageLoad, String waitForXPath, boolean extractShadowDom, String shadowHostXPath,
                         String authenticationMethodId, Map<String, String> authenticationConfiguration, String javascript) {
    this.waitOnPageLoad = waitOnPageLoad;
    this.waitForXPath = waitForXPath;
    this.extractShadowDom = extractShadowDom;
    this.shadowHostXPath = shadowHostXPath;
    this.authenticationMethodId = authenticationMethodId;
    this.authenticationConfiguration = authenticationConfiguration;
    this.javascript = javascript;
  }

  public Long getWaitOnPageLoad() {
    return waitOnPageLoad;
  }

  public void setWaitOnPageLoad(Long waitOnPageLoad) {
    this.waitOnPageLoad = waitOnPageLoad;
  }

  public String getWaitForXPath() {
    return waitForXPath;
  }

  public void setWaitForXPath(String waitForXPath) {
    this.waitForXPath = waitForXPath;
  }

  public boolean isExtractShadowDom() {
    return extractShadowDom;
  }

  public void setExtractShadowDom(boolean extractShadowDom) {
    this.extractShadowDom = extractShadowDom;
  }

  public String getShadowHostXPath() {
    return shadowHostXPath;
  }

  public void setShadowHostXPath(String shadowHostXPath) {
    this.shadowHostXPath = shadowHostXPath;
  }

  public String getJavascript() { return javascript; }

  public void setJavascript(String javascript) { this.javascript = javascript; }

  public String getAuthenticationMethodId() {
    return authenticationMethodId;
  }

  public void setAuthenticationMethodId(String authenticationMethodId) {
    this.authenticationMethodId = authenticationMethodId;
  }

  public Map<String, String> getAuthenticationConfiguration() {
    return authenticationConfiguration;
  }

  public void setAuthenticationConfiguration(Map<String, String> authenticationConfiguration) {
    this.authenticationConfiguration = authenticationConfiguration;
  }

  @Override
  public String toString() {
    return "PageLoadOptions{" +
        "waitOnPageLoad=" + waitOnPageLoad +
        ", waitForXPath='" + waitForXPath + '\'' +
        ", extractShadowDom=" + extractShadowDom +
        ", shadowHostXPath='" + shadowHostXPath + '\'' +
        ", authenticationMethodId='" + authenticationMethodId + '\'' +
        ", javascript='" + javascript + '\'' +
        '}';
  }

}
