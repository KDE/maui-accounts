package org.mauikit.accounts.dav.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkHelper {
  private String host;
  private String username;
  private String password;
  private OkHttpClient httpClient;

  public NetworkHelper(String host, String username, String password) {
    this.host = host;
    this.username = username;
    this.password= password;

    httpClient = new OkHttpClient.Builder().build();
  }

  public Response makeRequest(String method, Headers headers) throws IOException, URISyntaxException {
    return makeRequest(method, new URI(host), headers, "");
  }

  public Response makeRequest(String method, Headers headers, String body) throws IOException, URISyntaxException {
    return makeRequest(method, new URI(host), headers, body);
  }

  public Response makeRequest(String method, URI path, Headers headers, String body) throws IOException, URISyntaxException {
    URI url = new URI(host);
    String port = url.getPort() != -1 ? ":" + url.getPort() : "";
    URI requestUrl = new URI(url.getScheme() + "://" + url.getHost() + port + "/" + path.getPath());

    Request.Builder requestBuilder = new Request.Builder()
            .method(method, body == null ? null : RequestBody.create(MediaType.parse(body), body))
            .url(requestUrl.toURL());

    if (headers != null) {
      requestBuilder.headers(headers);
    }

    Request request = requestBuilder.header("Authorization", Credentials.basic(username, password)).build();

    Response response = httpClient.newCall(request).execute();

    return response;
  }
}
