package by.poskorbko.languageschool_fx;

import by.poskorbko.languageschool_fx.dto.Role;
import by.poskorbko.languageschool_fx.dto.UserDTO;
import by.poskorbko.languageschool_fx.tabs.*;
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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;

public class MainWindow {
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    private Timer inactivityTimer;
    private final UserDTO user;
    private final int timeoutMinutes = AppConfig.getInt("session.timeout.seconds");
    private final LanguageScaleTab languageScaleTab = new LanguageScaleTab();
    private final LanguagesTab languagesTab = new LanguagesTab();
    private final ScheduleTab scheduleTab = new ScheduleTab();
    private final UsersTab usersTab = new UsersTab();
    private final TeacherTab teacherTab = new TeacherTab();
    private final StudentsTab studentTab = new StudentsTab();

    public MainWindow(UserDTO user) {
        this.user = user;
    }

    public void show(Stage stage, Runnable onLogout) {
        String roleName = resolveUserRoleName(user.roles().toString());
        VBox userInfo = createUserInfo(user.firstName() + " " + user.lastName(), roleName);
        ImageView avatarView = createAvatar(user.avatarBase64());

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
        ImageView logo = createLogo();
        HBox header = new HBox(14, logo, userInfo, new Region(), avatarView, menuBtn, logoutBtn);
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS); // Spacer
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 24, 12, 24));
        header.setStyle("-fx-background-color: #f8f9fa;");

        // ====== Вкладки ======
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox scheduleBox = scheduleTab.createScheduleTable();
        VBox levelsBox = languageScaleTab.createLevelsTable();
        VBox languagesBox = languagesTab.createLanguagesTable();
        VBox usersBox = usersTab.createUsersTable();
        VBox teachersBox = teacherTab.createTeachersTable();
        VBox studentsBox = studentTab.createStudentsTable();

        Tab scheduleTab = new Tab("Расписание", scheduleBox);
        Tab studentsTab = new Tab("Студенты", studentsBox);
        Tab groupsTab = new Tab("Группы", BaseTab.createPlaceholderContent("Группы"));
        Tab teachersTab = new Tab("Учителя", teachersBox);

        Tab languagesTab = new Tab("Языки", languagesBox);
        Tab levelsTab = new Tab("Уровни языка", levelsBox);
        Tab spacer = createInvisibleTabSpacer(380);
        Tab usersTab = new Tab("Пользователи", usersBox);
        tabs.getTabs().addAll(scheduleTab, groupsTab, teachersTab, studentsTab, spacer, languagesTab, levelsTab, usersTab);

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

    private String resolveUserRoleName(String stringRoles) {
        String[] roles = stringRoles.substring(1, stringRoles.length() - 1).split(",");
        Set<Role> roleSet = new HashSet<>();
        for(String role : roles) {
            roleSet.add(Role.valueOf(role.trim()));
        }
        return Role.getStrongest(roleSet).getName();
    }

    private @NotNull ImageView createAvatar(String base64) {
        Image image;
        if (base64 == null || base64.isEmpty()) {
            image = new Image(Objects.requireNonNull(getClass().getResource("no_avatar.png"))
                    .toExternalForm(), 36, 36, true, true);
        } else {
            byte[] bytes = Base64.getDecoder().decode(base64);
            image = new Image(new ByteArrayInputStream(bytes), 36, 36, true, true);
        }
        ImageView avatar = new ImageView(image);
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setClip(new Circle(18, 18, 18));
        return avatar;
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
