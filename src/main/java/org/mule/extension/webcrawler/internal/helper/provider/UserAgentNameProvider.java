package org.mule.extension.webcrawler.internal.helper.provider;

import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class UserAgentNameProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return ValueBuilder.getValuesFor(
        // Google Chrome User-Agents
        Constants.USER_AGENT_CHROME_WINDOWS,
        Constants.USER_AGENT_CHROME_MAC,
        Constants.USER_AGENT_CHROME_LINUX,
        // Mozilla Firefox User-Agents
        Constants.USER_AGENT_FIREFOX_WINDOWS,
        Constants.USER_AGENT_FIREFOX_MAC,
        Constants.USER_AGENT_FIREFOX_LINUX,
        // Safari User-Agents
        Constants.USER_AGENT_SAFARI_MAC,
        // Microsoft Edge User-Agents
        Constants.USER_AGENT_EDGE_WINDOWS,
        // Mobile User-Agents
        Constants.USER_AGENT_CHROME_ANDROID,
        Constants.USER_AGENT_SAFARI_IOS,
        // Googlebot User-Agent
        Constants.USER_AGENT_GOOGLEBOT,
        // Bingbot User-Agent
        Constants.USER_AGENT_BINGBOT,
        // General Mobile User-Agent
        Constants.USER_AGENT_MOBILE_GENERIC,
        // Custom User-Agent for fallback
        Constants.USER_AGENT_CUSTOM_DEFAULT);
  }
}
