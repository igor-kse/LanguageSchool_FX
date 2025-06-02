package by.poskorbko.languageschool_fx;

import by.poskorbko.languageschool_fx.dto.UserDTO;
import by.poskorbko.languageschool_fx.util.Utils;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class AuthService {
    private final String serverUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    private static String sessionCookie = "123";

    public AuthService() {
        String host = AppConfig.get("server.host");
        String port = AppConfig.get("server.port");
        this.serverUrl = "http://" + host + ":" + port;
    }

    public void loginAsync(String email, String password, BiConsumer<Boolean, AuthResponse> onResult) {
            String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            new Thread(() -> {
               try {
                   HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                   boolean ok = response.statusCode() == 200;
                   if (ok) {
                       UserDTO user = Utils.jsonMapper.readValue(response.body(), UserDTO.class);
                       Platform.runLater(() -> onResult.accept(true, new AuthResponse(null, user)));
                   } else {
                       String message = bundle.getString("error.auth");
                       Platform.runLater(() -> onResult.accept(false, new AuthResponse(message, null)));
                   }
               } catch (Exception e) {
                   e.printStackTrace();
                   Platform.runLater(() -> onResult.accept(false, new AuthResponse(e.getMessage(), null)));
               }
            }).start();
    }

    public static String getSessionCookie() {
        return sessionCookie;
    }

    public record AuthResponse(String message, UserDTO user) {}
}
