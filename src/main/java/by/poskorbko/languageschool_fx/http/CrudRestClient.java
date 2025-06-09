package by.poskorbko.languageschool_fx.http;

import by.poskorbko.languageschool_fx.AppConfig;
import by.poskorbko.languageschool_fx.AuthService;
import by.poskorbko.languageschool_fx.util.JsonObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class CrudRestClient {
    private static final String serverUrl = "http://" + AppConfig.get("server.host") + ":" + AppConfig.get("server.port");
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void getCall(String path, Consumer<HttpResponse<String>> onSuccess, Consumer<HttpResponse<String>> onFailure) {
        new Thread(() -> {
            try {
                System.out.println("Get call: " + path);
                var requestBuilder = getBuilder(path);
                var response = client.send(requestBuilder.GET().build(), HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());
                if (response.statusCode() == 200) {
                    onSuccess.accept(response);
                } else {
                    onFailure.accept(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void addPostCall(String path, Object toJson, Consumer<HttpResponse<String>> onSuccess, Consumer<HttpResponse<String>> onFail) {
        new Thread(() -> {
            try {
                System.out.println("Add call: " + path);
                var json = JsonObjectMapper.getInstance().writeValueAsString(toJson);
                var bodyPublisher = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
                var requestBuilder = getBuilder(path);
                var request = requestBuilder.POST(bodyPublisher).build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    onSuccess.accept(response);
                } else {
                    onFail.accept(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
                onFail.accept(null);
            }
        }).start();
    }

    public static void patchCall(String path, Object toJson, Consumer<HttpResponse<String>> onSuccess, Consumer<HttpResponse<String>> onFail) {
        new Thread(() -> {
            try {
                String json = JsonObjectMapper.getInstance().writeValueAsString(toJson);
                var bodyPublisher = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
                var requestBuilder = getBuilder(path);
                var request = requestBuilder.method("PATCH", bodyPublisher).build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());
                if (response.statusCode() == 200 || response.statusCode() == 204) {
                    onSuccess.accept(response);
                } else {
                    onFail.accept(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
                onFail.accept(null);
            }
        }).start();
    }

    public static void putCall(String path, Object toJson, Consumer<HttpResponse<String>> onSuccess, Consumer<HttpResponse<String>> onFailure) {
        new Thread(() -> {
            try {
                var json = JsonObjectMapper.getInstance().writeValueAsString(toJson);
                var requestBuilder = getBuilder(path);
                var bodyPublisher = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
                var request = requestBuilder.PUT(bodyPublisher).build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());
                if (response.statusCode() == 204) {
                    onSuccess.accept(response);
                } else {
                    onFailure.accept(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void deleteCall(String path, Consumer<HttpResponse<String>> onSuccess, Consumer<HttpResponse<String>> onFailure) {
        new Thread(() -> {
            try {
                var requestBuilder = getBuilder(path);
                var request = requestBuilder.DELETE().build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());
                if (response.statusCode() == 204) {
                    onSuccess.accept(response);
                } else {
                    onFailure.accept(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static HttpRequest.Builder getBuilder(String path) {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .header("Content-Type", "application/json")
                .header("SESSIONID", AuthService.getSessionCookie());
        addSessionCookie(requestBuilder);
        return requestBuilder;
    }

    private static void addSessionCookie(HttpRequest.Builder builder) {
        String cookie = AuthService.getSessionCookie();
        if (cookie != null && !cookie.isBlank()) {
            builder.header("SESSIONID", cookie);
        }
    }
}
