package org.mule.extension.webcrawler.internal.connection.webdriver;

import org.openqa.selenium.WebDriver;

/**
 * Lambda body executed inside {@link RemoteWebDriverConnection#withDriver(WebDriverExecutable)} so the connection can
 * centralise session-fatal recovery around any caller's driver interaction.
 */
@FunctionalInterface
public interface WebDriverExecutable<T> {

  T execute(WebDriver driver) throws Exception;
}
