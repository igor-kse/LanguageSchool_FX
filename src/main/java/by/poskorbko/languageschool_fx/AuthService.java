package by.poskorbko.languageschool_fx;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthService {
    private final String serverUrl;

    public AuthService() {
        String host = AppConfig.get("server.host");
        String port = AppConfig.get("server.port");
        this.serverUrl = "http://" + host + ":" + port;
    }
//
//    public boolean login(String email, String password) throws Exception {
//        try (HttpClient client = HttpClient.newHttpClient()) {
//            String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(serverUrl + "/api/auth/login"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            // Логика успеха зависит от API! Пример:
//            return response.statusCode() == 200;
//        }
//    }

    public boolean login(String email, String password) throws Exception {
        return true;
    }
}
