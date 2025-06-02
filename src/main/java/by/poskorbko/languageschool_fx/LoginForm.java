package by.poskorbko.languageschool_fx;

import by.poskorbko.languageschool_fx.dto.UserDTO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.ResourceBundle;

public class LoginForm extends Application {
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    private VBox loginFormBox;
    private Button loginButton;
    private TextField emailField;
    private PasswordField passwordField;
    private ProgressIndicator loginSpinner;
    private Label errorLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        start(stage, "");
    }

    public void start(Stage authStage, String errorMsg) {
        Scene scene = createMainScene(errorMsg);

        // --- Логика авторизации ---
        EventHandler<ActionEvent> loginEventHandler = e -> getLoginAction(authStage, new AuthService());
        loginButton.setOnAction(loginEventHandler);

        authStage.setTitle(bundle.getString("window.title"));
        authStage.setScene(scene);
        authStage.setResizable(false);
        authStage.show();
        authStage.centerOnScreen();
    }

    private void getLoginAction(Stage primaryStage, AuthService authService) {
        errorLabel.setVisible(false);

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError(loginFormBox, errorLabel, bundle.getString("error.empty.fields"));
            return;
        }
        if (!email.matches("^[\\w-.]+@[\\w-]+\\.[a-z]{2,}$")) {
            showError(loginFormBox, errorLabel, bundle.getString("error.invalid.email"));
            return;
        }
        loginButton.setDisable(true);
        loginSpinner.setVisible(true);

        authService.loginAsync(email, password, (ok, authResponse) -> {
            loginSpinner.setVisible(false);
            loginButton.setDisable(false);
            if (ok) {
                showMainWindow(primaryStage, authResponse.user());
            } else {
                String authMessage = authResponse.message();
                String message = (authMessage != null && !authMessage.isBlank())
                        ? authMessage
                        : bundle.getString("error.invalid.credentials");
                showError(loginFormBox, errorLabel, message);
            }
        });
    }

    private void showMainWindow(Stage stage, UserDTO user) {
        MainWindow mainWindow = new MainWindow(user);
        mainWindow.show(stage, () -> {
            // По логауту возвращаем окно авторизации
            try {
                start(stage, bundle.getString("error.inactive.logout"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void showError(Pane node, Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setTextFill(Color.web("#e74c3c"));
        errorLabel.setVisible(true);

        javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(Duration.millis(60), node);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.playFromStart();
    }

    //
    // ############################## FORM CONTROLS CREATION ##############################
    private @NotNull Scene createMainScene(String errorMsg) {

        emailField = createEmailField();
        passwordField = createPasswordField();
        errorLabel = createErrorLabel(errorMsg);
        loginButton = createLoginButton();
        loginSpinner = createLoginSpinner();

        var title = createFormTitle();
        loginFormBox = createLoginFormBox(title, emailField, passwordField, errorLabel);

        StackPane pane = new StackPane(loginFormBox);
        pane.setPrefSize(400, 370);
        pane.setStyle("-fx-background-color: linear-gradient(to bottom, #e9f0ff, #e6e6e6);");

        Scene scene = new Scene(pane);
        String style = Objects.requireNonNull(getClass().getResource("login.css")).toExternalForm();
        scene.getStylesheets().add(style);
        return scene;
    }

    private @NotNull VBox createLoginFormBox(Text title, TextField email, PasswordField password, Label errorMessage) {
        var loginRow = new HBox(10, loginButton, loginSpinner);
        loginRow.setAlignment(Pos.CENTER);

        VBox formBox = new VBox(16, title, createLogoImage(), email, password, errorMessage, loginRow);
        formBox.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 18;");
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(40, 30, 40, 30));
        formBox.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 18;");
        formBox.setEffect(new DropShadow(10, Color.rgb(80, 80, 80, 0.15)));
        return formBox;
    }

    private static @NotNull ProgressIndicator createLoginSpinner() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(30, 30);
        spinner.setVisible(false);
        return spinner;
    }

    private @NotNull Button createLoginButton() {
        Button loginButton = new Button(bundle.getString("login.button"));
        loginButton.setStyle("-fx-background-color: #3578e5; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-size: 16px;");
        loginButton.setPrefHeight(40);
        loginButton.setDefaultButton(true);
        return loginButton;
    }

    private static @NotNull Label createErrorLabel(String errorMsg) {
        Label label = new Label();
        label.setTextFill(Color.web("#e74c3c"));
        label.setFont(Font.font(14));
        label.setVisible(false);
        label.setWrapText(true);
        label.setMaxWidth(320);
        label.setAlignment(Pos.CENTER);


        label.setTextFill(Color.web("#e74c3c"));
        if (errorMsg != null && !errorMsg.isBlank()) {
            label.setText(errorMsg);
            label.setTextFill(Color.web("#e74c3c"));
            label.setVisible(true);
        }
        return label;
    }

    private @NotNull PasswordField createPasswordField() {
        PasswordField passwordField = new PasswordField();
        passwordField.setStyle("-fx-background-color: #fff; -fx-text-fill: #212121; -fx-background-radius: 10; -fx-prompt-text-fill: #888;");
        passwordField.setPromptText(bundle.getString("password.placeholder"));
        passwordField.setPrefHeight(40);
        return passwordField;
    }

    private @NotNull TextField createEmailField() {
        TextField emailField = new TextField();
        emailField.setStyle("-fx-background-color: #fff; -fx-text-fill: #212121; -fx-background-radius: 10; -fx-prompt-text-fill: #888;");
        emailField.setPromptText(bundle.getString("email.placeholder"));
        emailField.setPrefHeight(40);
        return emailField;
    }

    private @NotNull Text createFormTitle() {
        String formTitle = bundle.getString("form.title");
        Text title = new Text(formTitle);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setFill(Color.web("#3A3A3A"));
        title.setTextAlignment(TextAlignment.CENTER);
        title.setFill(Color.web("#3A3A3A"));
        return title;
    }

    private @NotNull ImageView createLogoImage() {
        var logoUrl = getClass().getResource("logo.png");
        Image image = new Image(Objects.requireNonNull(logoUrl).toExternalForm(), 500, 500, true, true);
        ImageView logoView = new ImageView(image);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);
        logoView.setFitWidth(240);
        logoView.setFitHeight(240);
        logoView.setOpacity(1);
//        logoView.setEffect(new DropShadow(16, Color.rgb(30,30,60,0.20)));
        return logoView;
    }
}
