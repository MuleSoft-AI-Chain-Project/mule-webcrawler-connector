package org.mule.extension.webcrawler.internal.helper.search;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class SerperDev {

    /**
     * Shared HTTP client. Per okhttp guidance, an OkHttpClient holds connection / thread / dispatcher pools that should be reused
     * across calls. Building one per call leaks resources under load.
     */
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    private static final okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json");

    private SerperDev() {}

  public static String search(String query, String apiKey) throws IOException {

        // Encode the query through JSONObject — string concatenation lets any quote, backslash, or
        // newline in a user-supplied query produce malformed JSON or, worse, inject extra fields.
        String requestJson = new JSONObject().put("q", query == null ? "" : query).toString();

    Request request = new Request.Builder()
        .url("https://google.serper.dev/search")
        .post(RequestBody.create(requestJson, JSON))
        .addHeader("X-API-KEY", apiKey)
        .addHeader("Content-Type", "application/json")
        .build();

    try (Response response = CLIENT.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        return response.body() == null ? "" : response.body().string();
    }
  }
}
