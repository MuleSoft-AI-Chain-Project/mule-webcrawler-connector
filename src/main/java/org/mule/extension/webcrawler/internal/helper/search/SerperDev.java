package org.mule.extension.webcrawler.internal.helper.search;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class SerperDev {

  public static String search(String query, String apiKey) throws IOException {

    OkHttpClient client = new OkHttpClient().newBuilder().build();
    okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
    RequestBody body = RequestBody.create("{\"q\":\"" + query + "\"}", mediaType);
    Request request = new Request.Builder()
        .url("https://google.serper.dev/search")
        .method("POST", body)
        .addHeader("X-API-KEY", apiKey)
        .addHeader("Content-Type", "application/json")
        .build();
    Response response = client.newCall(request).execute();

    if (!response.isSuccessful()) {
      throw new IOException("Unexpected code " + response);
    }

    return response.body().string();
  }
}
