package org.mule.extension.webcrawler.api;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Map;
import java.util.Set;

public class AuthenticationMethodValueProvider implements ValueProvider {

    static Map<String, String> valueDisplayMap = Map.of(
            "basicOrDigestAuth", "Basic Or Digest Authentication",
            "formCookieAuth", "Form (Cookie Based) Authentication",
            "apiKey", "API Key Authentication",
            "jwtAuth", "JSON Web Token (JWT) Authentication",
            "bearerTokenAuth", "OAuth (Bearer Token) Authentication"
    );

    @Override
    public Set<Value> resolve() throws ValueResolvingException {
        return ValueBuilder.getValuesFor(valueDisplayMap);
    }
}