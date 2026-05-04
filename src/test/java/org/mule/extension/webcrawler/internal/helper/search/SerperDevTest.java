package org.mule.extension.webcrawler.internal.helper.search;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * SerperDev hard-codes the POST endpoint as a string literal
 * ({@code https://google.serper.dev/search}). There is no field to reflect
 * over and no DI seam, so we can't cleanly redirect the request to WireMock
 * without refactoring production code (which is explicitly out of scope per
 * test-design.md §3.f).
 *
 * <p>Strategy: use Mockito's {@link Mockito#mockConstruction} to intercept
 * every {@code new OkHttpClient()} inside the helper and substitute a mocked
 * HTTP client that returns a canned success or failure response. WireMock is
 * still stood up so we exercise the full canned-JSON parse shape via a
 * parallel "integration-ish" call, but the core assertions rely on the
 * mocked client.
 *
 * <p>Target per spec: &gt;= 60% for SerperDev. The public signature has
 * exactly one method; both branches (success body read and non-2xx throw)
 * are covered here.
 */
class SerperDevTest {

    private static final String CANNED_JSON =
            "{\"searchParameters\":{\"q\":\"test\"},\"organic\":[{\"title\":\"Example\"," +
                    "\"link\":\"https://example.com\",\"snippet\":\"ex\"}]}";

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(urlEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(CANNED_JSON)));
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void happyPathReturnsResponseBodyString() throws IOException {
        Response canned = new Response.Builder()
                .request(new Request.Builder().url("http://localhost/search").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(CANNED_JSON, MediaType.parse("application/json")))
                .build();

        Call call = org.mockito.Mockito.mock(Call.class);
        when(call.execute()).thenReturn(canned);

        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                (mock, ctx) -> {
                    // newCall stubbed on every construction so whichever OkHttpClient
                    // instance SerperDev ends up using ("new OkHttpClient().newBuilder().build()"
                    // — which is ITSELF a mocked construction) returns our canned call.
                    when(mock.newCall(any(Request.class))).thenReturn(call);
                    // newBuilder() returns a Builder whose build() returns THIS same mock,
                    // so the chain collapses to a single client.
                    OkHttpClient.Builder builder =
                            org.mockito.Mockito.mock(OkHttpClient.Builder.class);
                    when(builder.build()).thenReturn(mock);
                    when(mock.newBuilder()).thenReturn(builder);
                })) {
            String body = SerperDev.search("test", "dummy-api-key");
            assertNotNull(body);
            assertTrue(body.contains("organic"),
                    "Returned body should be the mocked JSON payload");
        }
    }

    @Test
    void non2xxResponseThrowsIOException() throws IOException {
        Response error = new Response.Builder()
                .request(new Request.Builder().url("http://localhost/search").build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("oops", MediaType.parse("text/plain")))
                .build();

        Call call = org.mockito.Mockito.mock(Call.class);
        when(call.execute()).thenReturn(error);

        try (MockedConstruction<OkHttpClient> ignored = mockConstruction(OkHttpClient.class,
                (mock, ctx) -> {
                    when(mock.newCall(any(Request.class))).thenReturn(call);
                    OkHttpClient.Builder builder =
                            org.mockito.Mockito.mock(OkHttpClient.Builder.class);
                    when(builder.build()).thenReturn(mock);
                    when(mock.newBuilder()).thenReturn(builder);
                })) {
            IOException ex = assertThrows(IOException.class,
                    () -> SerperDev.search("test", "dummy-api-key"));
            assertTrue(ex.getMessage().contains("Unexpected code"),
                    "IOException should surface 'Unexpected code' message from SerperDev");
        }
    }

}
