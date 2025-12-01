package org.mule.extension.webcrawler.internal.extension;

import org.mule.extension.webcrawler.api.CustomAuthenticator;
import org.mule.extension.webcrawler.api.authenticator.BasicOrDigestAuthenticator;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.error.WebCrawlerErrorType;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.openqa.selenium.WebDriver;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;

/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "ms-webcrawler")
@Extension(name = "MuleSoft WebCrawler Connector")
@Configurations({WebCrawlerConfiguration.class})
@JavaVersionSupport({JAVA_17})
@ErrorTypes(WebCrawlerErrorType.class)
@Export(classes = {CustomAuthenticator.class, WebDriver.class})
public class Connector {

}
