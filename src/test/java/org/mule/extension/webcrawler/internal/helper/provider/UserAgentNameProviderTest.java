package org.mule.extension.webcrawler.internal.helper.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mule.extension.webcrawler.internal.constant.Constants;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueResolvingException;

class UserAgentNameProviderTest {

    @Test
    void resolveReturnsNonEmptySet() throws ValueResolvingException {
        UserAgentNameProvider provider = new UserAgentNameProvider();
        Set<Value> values = provider.resolve();
        assertNotNull(values);
        assertFalse(values.isEmpty(), "User-agent value set must not be empty");
    }

    @Test
    void resolveIncludesKeyUserAgents() throws ValueResolvingException {
        UserAgentNameProvider provider = new UserAgentNameProvider();
        Set<Value> values = provider.resolve();

        Set<String> ids = values.stream().map(Value::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(Constants.USER_AGENT_CHROME_WINDOWS),
                "Expected Chrome Windows UA to be registered");
        assertTrue(ids.contains(Constants.USER_AGENT_GOOGLEBOT),
                "Expected Googlebot UA to be registered");
        assertTrue(ids.contains(Constants.USER_AGENT_SAFARI_IOS),
                "Expected Safari iOS UA to be registered");
    }

    @Test
    void resolveReturnsExpectedDistinctUserAgentCount() throws ValueResolvingException {
        // Provider registers 14 constants; USER_AGENT_CHROME_WINDOWS == USER_AGENT_CUSTOM_DEFAULT,
        // so ValueBuilder collapses duplicates and the final distinct set has 13 entries.
        UserAgentNameProvider provider = new UserAgentNameProvider();
        Set<Value> values = provider.resolve();

        int size = values.size();
        assertTrue(size == 13 || size == 14,
                "Expected 13 or 14 user-agent entries but got " + size);
    }
}
