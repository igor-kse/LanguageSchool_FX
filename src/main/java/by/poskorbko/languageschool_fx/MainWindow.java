package by.poskorbko.languageschool_fx;

import by.poskorbko.languageschool_fx.dto.LevelScaleDTO;
import by.poskorbko.languageschool_fx.tabs.LanguageLevelTab;
import by.poskorbko.languageschool_fx.tabs.LanguagesTab;
import by.poskorbko.languageschool_fx.tabs.ScheduleTab;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class MainWindow {
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    private Timer inactivityTimer;
    private final int timeoutMinutes = AppConfig.getInt("session.timeout.seconds");

    public void show(Stage stage, Runnable onLogout) {
        // FIXME получить с бэка
        String userName = "Иван Иванов";
        String userRole = "Администратор";
        String avatarPath = "avatar.png"; // Положи png-аватар в ресурсы или используй дефолт

        ImageView logo = createLogo();
        VBox userInfo = createUserInfo(userName, userRole);
        ImageView avatarView = createAvatar(avatarPath);

        // ====== Кнопка выхода ======
        Button logoutBtn = new Button(bundle.getString("logout.button"));
        logoutBtn.setStyle("-fx-background-radius: 12; -fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutBtn.setOnAction(e -> {
            Platform.runLater(onLogout);
        });

        // FIXME сделать меню
        // ====== Меню (пример) ======
        MenuButton menuBtn = new MenuButton("Меню");
        menuBtn.getItems().addAll(
                new MenuItem("О программе"),
                new MenuItem("Помощь"),
                new MenuItem("Настройки")
        );

        // ====== Шапка ======
        HBox header = new HBox(14, logo, userInfo, new Region(), avatarView, menuBtn, logoutBtn);
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS); // Spacer
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 24, 12, 24));
        header.setStyle("-fx-background-color: #f8f9fa;");

        // ====== Вкладки ======
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // FIXME
        VBox scheduleBox = ScheduleTab.createScheduleTable(TestData.getTestSchedule());
        VBox levelsBox = LanguageLevelTab.createLevelsTable(TestData.getTestLevelScale());
        VBox languagesBox = LanguagesTab.createLanguagesTable(TestData.getTestLanguageEntries(), TestData.getTestLevelScale().stream().map(LevelScaleDTO::name).toList());

        // TODO проверить
        // Можно добавить заголовок:
        // Label scheduleLabel = new Label("Расписание групп");
        // scheduleLabel.setFont(Font.font(18));
        // scheduleBox.getChildren().add(0, scheduleLabel);

        Tab scheduleTab = new Tab("Расписание", scheduleBox);
        Tab studentsTab = new Tab("Студенты", createPlaceholderContent("Студенты"));
        Tab groupsTab = new Tab("Группы", createPlaceholderContent("Группы"));

        Tab languagesTab = new Tab("Языки", languagesBox);
        Tab levelsTab = new Tab("Уровни языка", levelsBox);
        Tab spacer = createInvisibleTabSpacer(500);
        tabs.getTabs().addAll(scheduleTab, studentsTab, groupsTab, spacer, languagesTab, levelsTab);

        // ====== Layout ======
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabs);

        Scene scene = new Scene(root, 900, 650);

        // ====== Авто-логаут ======
        Runnable resetTimer = () -> restartTimer(stage, onLogout);
        scene.addEventFilter(MouseEvent.ANY, e -> resetTimer.run());
        scene.addEventFilter(KeyEvent.ANY, e -> resetTimer.run());

        String style = Objects.requireNonNull(getClass().getResource("main.css")).toExternalForm();
        scene.getStylesheets().add(style);

        stage.setScene(scene);
        stage.setTitle(bundle.getString("main.title"));
        stage.show();
        stage.centerOnScreen();
        restartTimer(stage, onLogout);
    }

    private @NotNull ImageView createAvatar(String avatarPath) {
        ImageView avatarView;
        try {
            avatarView = new ImageView(new Image(Objects.requireNonNull(getClass().getResource(avatarPath)).toExternalForm(), 36, 36, true, true));
        } catch (Exception e) {
            avatarView = new ImageView(); // fallback если нет файла
        }
        avatarView.setClip(new Circle(18, 18, 18));
        return avatarView;
    }

    private static @NotNull VBox createUserInfo(String userName, String userRole) {
        VBox userInfo = new VBox(
                new Label(userName),
                new Label(userRole)
        );
        userInfo.setAlignment(Pos.CENTER_LEFT);
        userInfo.setSpacing(2);
        userInfo.getChildren().get(0).setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        userInfo.getChildren().get(1).setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        return userInfo;
    }

    private @NotNull ImageView createLogo() {
        URL url = getClass().getResource("logo.png");
        Objects.requireNonNull(url, "Error getting logo");
        Image image = new Image(url.toExternalForm(), 38, 38, true, true);
        return new ImageView(image);
    }

    private void restartTimer(Stage stage, Runnable onLogout) {
        if (inactivityTimer != null) inactivityTimer.cancel();
        inactivityTimer = new Timer(true);
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(onLogout);
            }
        }, timeoutMinutes * 1000L);
    }

    // === Заглушка для вкладок ===
    private VBox createPlaceholderContent(String title) {
        Label label = new Label(title + " — здесь будет содержимое");
        label.setFont(Font.font("Arial", 18));
        label.setTextFill(Color.web("#888"));
        VBox box = new VBox(label);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(32));
        return box;
    }

    public static Tab createInvisibleTabSpacer(int widthPx) {
        Tab spacer = new Tab();
        spacer.setClosable(false);
        spacer.setDisable(true);

        // Прозрачная "заглушка" для ширины
        Label spacerLabel = new Label();
        spacerLabel.setMinWidth(widthPx);
        spacerLabel.setPrefWidth(widthPx);
        spacerLabel.setMaxWidth(widthPx);
        spacerLabel.setStyle("-fx-background-color: transparent;");

        spacer.setGraphic(spacerLabel);

        // Для абсолютной невидимости — кастомный стиль
        spacer.getStyleClass().add("tab-spacer-invisible");
        return spacer;
    }
}
