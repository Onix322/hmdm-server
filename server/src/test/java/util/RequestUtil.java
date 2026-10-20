package util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RequestUtil {

  public static HttpResponse<String> requestPost(URI uri, String body)
      throws IOException, InterruptedException {

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(uri)
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> requestGet(URI uri) throws IOException, InterruptedException {

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request =
        HttpRequest.newBuilder().uri(uri).header("accept", "application/json").GET().build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
