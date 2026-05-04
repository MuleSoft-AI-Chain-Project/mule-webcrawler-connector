package org.mule.extension.webcrawler.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueResolvingException;

class AuthenticationMethodValueProviderTest {

    @Test
    void resolveReturnsExactlyTwoEntries() throws ValueResolvingException {
        AuthenticationMethodValueProvider provider = new AuthenticationMethodValueProvider();
        Set<Value> values = provider.resolve();

        assertNotNull(values);
        assertEquals(2, values.size(), "Expected exactly 2 authentication method entries");
    }

    @Test
    void resolveContainsBasicOrDigestAuthWithExpectedLabel() throws ValueResolvingException {
        AuthenticationMethodValueProvider provider = new AuthenticationMethodValueProvider();
        Set<Value> values = provider.resolve();

        Set<String> ids = values.stream().map(Value::getId).collect(Collectors.toSet());
        Set<String> labels = values.stream().map(Value::getDisplayName).collect(Collectors.toSet());

        assertTrue(ids.contains("basicOrDigestAuth"), "Missing id 'basicOrDigestAuth'");
        assertTrue(labels.contains("Basic Or Digest Authentication"), "Missing label for basicOrDigestAuth");
    }

    @Test
    void resolveContainsFormCookieAuthWithExpectedLabel() throws ValueResolvingException {
        AuthenticationMethodValueProvider provider = new AuthenticationMethodValueProvider();
        Set<Value> values = provider.resolve();

        Set<String> ids = values.stream().map(Value::getId).collect(Collectors.toSet());
        Set<String> labels = values.stream().map(Value::getDisplayName).collect(Collectors.toSet());

        assertTrue(ids.contains("formCookieAuth"), "Missing id 'formCookieAuth'");
        assertTrue(labels.contains("Form (Cookie Based) Authentication"), "Missing label for formCookieAuth");
    }

    @Test
    void staticDisplayMapHasExactKeys() {
        assertEquals(2, AuthenticationMethodValueProvider.valueDisplayMap.size());
        assertEquals("Basic Or Digest Authentication",
                AuthenticationMethodValueProvider.valueDisplayMap.get("basicOrDigestAuth"));
        assertEquals("Form (Cookie Based) Authentication",
                AuthenticationMethodValueProvider.valueDisplayMap.get("formCookieAuth"));
    }
}
