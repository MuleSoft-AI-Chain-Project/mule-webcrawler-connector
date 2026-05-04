package org.mule.extension.webcrawler.internal.connection.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;

class HttpConnectionProviderTest {

    private HttpConnectionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new HttpConnectionProvider();
    }

    @Test
    void validateHappyPath() throws ConnectionException {
        HttpConnection conn = mock(HttpConnection.class);
        when(conn.validate()).thenReturn(true);

        ConnectionValidationResult result = provider.validate(conn);
        assertTrue(result.isValid());
    }

    @Test
    void validateFailureWhenConnectionThrows() throws ConnectionException {
        HttpConnection conn = mock(HttpConnection.class);
        when(conn.validate()).thenThrow(new ConnectionException("broken"));

        ConnectionValidationResult result = provider.validate(conn);
        assertFalse(result.isValid());
    }

    @Test
    void disconnectIsNoOpAndDoesNotThrow() {
        HttpConnection conn = mock(HttpConnection.class);
        assertDoesNotThrow(() -> provider.disconnect(conn));
    }

    @Test
    void stopWithNullHttpClientIsNoOp() {
        // When httpClient is null (not started), stop() should guard and not throw.
        assertDoesNotThrow(() -> provider.stop());
    }

    @Test
    void stopWithValidHttpClientCallsStop() throws Exception {
        HttpClient client = mock(HttpClient.class);
        setHttpClient(provider, client);

        provider.stop();
        verify(client).stop();
    }

    @Test
    void stopPropagatesHttpClientStopException() throws Exception {
        HttpClient client = mock(HttpClient.class);
        doThrow(new RuntimeException("shutdown failed")).when(client).stop();
        setHttpClient(provider, client);

        // stop() does not catch; the runtime exception should propagate.
        try {
            provider.stop();
        } catch (RuntimeException expected) {
            // ok
            return;
        }
        throw new AssertionError("Expected RuntimeException from client.stop()");
    }

    // ---------- createClientConfiguration TLS branches ----------
    //
    // Covers HttpConnectionProvider.createClientConfiguration():
    //   - injected-TLS branch at line 103: builder.setTlsContextFactory(tlsContext)
    //   - default-TLS branch at line 105: builder.setTlsContextFactory(TlsContextFactory.builder().buildDefault())
    //
    // Exercised via reflection because the method is private and start() requires a
    // full HttpService which isn't available in unit-test context. This closes the
    // coverage gap that MUnit CP-T02 was skipped against (Mule 4.6 runtime cannot
    // boot an inline <tls:context> without a classpath tls-default.conf).

    @Test
    void createClientConfigurationUsesInjectedTlsContextWhenProvided() throws Exception {
        TlsContextFactory injected = mock(TlsContextFactory.class);
        setField(provider, "configName", "test-cfg");
        setField(provider, "tlsContext", injected);

        HttpClientConfiguration config = invokeCreateClientConfiguration(provider);

        assertNotNull(config);
        // Confirm the builder wired the injected instance through (not the default).
        assertSame(injected, config.getTlsContextFactory(),
                "Expected injected TlsContextFactory to be used when provider.tlsContext != null");
    }

    @Test
    void createClientConfigurationUsesDefaultTlsContextWhenNotProvided() throws Exception {
        setField(provider, "configName", "test-cfg");
        // tlsContext left unset (null) — default branch

        HttpClientConfiguration config = invokeCreateClientConfiguration(provider);

        assertNotNull(config);
        assertNotNull(config.getTlsContextFactory(),
                "Default TlsContextFactory should be populated when provider.tlsContext is null");
    }

    private static HttpClientConfiguration invokeCreateClientConfiguration(HttpConnectionProvider provider)
            throws Exception {
        Method m = HttpConnectionProvider.class.getDeclaredMethod("createClientConfiguration");
        m.setAccessible(true);
        return (HttpClientConfiguration) m.invoke(provider);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = HttpConnectionProvider.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setHttpClient(HttpConnectionProvider provider, HttpClient client) throws Exception {
        Field f = HttpConnectionProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(provider, client);
    }
}
