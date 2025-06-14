package by.poskorbko.languageschool_fx;

import by.poskorbko.languageschool_fx.dto.Role;
import by.poskorbko.languageschool_fx.dto.UserDTO;
import by.poskorbko.languageschool_fx.tabs.*;
import by.poskorbko.languageschool_fx.util.ActivityMonitor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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
    private final GroupTab groupTab = new GroupTab();
    private final PaymentTab paymentTab = new PaymentTab();

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
        var about = new MenuItem("VIP Lang");
        about.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("О программе");
            alert.setHeaderText("О программе");
            alert.setContentText(
                    """
                            Программное средство для автоматизации работы
                            школы иностранных языков "VIP Lang"
                            
                            Разработано студенткой второго курса группы 314371
                            Поскробко Виктории Андреевной
                            """
            );
            alert.showAndWait();
        });
        menuBtn.getItems().addAll(about);

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

        Set<Role> roles = user.roles().stream().map(Role::valueOf).collect(Collectors.toSet());
        VBox scheduleBox = scheduleTab.createScheduleTable(roles);
        VBox levelsBox = languageScaleTab.createLevelsTable();
        VBox languagesBox = languagesTab.createLanguagesTable();
        VBox usersBox = usersTab.createUsersTable();
        VBox teachersBox = teacherTab.createTeachersTable();
        VBox studentsBox = studentTab.createStudentsTable();
        VBox groupBox = groupTab.createGroupsTable(roles);
        VBox paymentBox = paymentTab.createPaymentsTable(roles);

        Tab scheduleTabFx = new Tab("Расписание", scheduleBox);
        Tab studentsTabFx = new Tab("Студенты", studentsBox);
        Tab groupsTabFx = new Tab("Группы", groupBox);
        Tab teachersTabFx = new Tab("Учителя", teachersBox);
        Tab paymentTabFx = new Tab("Платежи", paymentBox);

        Tab languagesTabFx = new Tab("Языки", languagesBox);
        Tab levelsTabFx = new Tab("Уровни языка", levelsBox);
        Tab spacer = createInvisibleTabSpacer(320);
        Tab usersTabFx = new Tab("Пользователи", usersBox);

        scheduleTabFx.setOnSelectionChanged(e -> {
            if (scheduleTabFx.isSelected()) scheduleTab.getRefreshButton().fire();
        });
        studentsTabFx.setOnSelectionChanged(e -> {
            if (studentsTabFx.isSelected()) studentTab.getRefreshButton().fire();
        });
        groupsTabFx.setOnSelectionChanged(e -> {
            if (groupsTabFx.isSelected()) groupTab.getRefreshButton().fire();
        });
        teachersTabFx.setOnSelectionChanged(e -> {
            if (teachersTabFx.isSelected()) teacherTab.getRefreshButton().fire();
        });
        paymentTabFx.setOnSelectionChanged(e -> {
            if (paymentTabFx.isSelected()) paymentTab.getRefreshButton().fire();
        });
        languagesTabFx.setOnSelectionChanged(e -> {
            if (languagesTabFx.isSelected()) languagesTab.getRefreshButton().fire();
        });
        levelsTabFx.setOnSelectionChanged(e -> {
            if (levelsTabFx.isSelected()) languageScaleTab.getRefreshButton().fire();
        });
        usersTabFx.setOnSelectionChanged(e -> {
            if (usersTabFx.isSelected()) usersTab.getRefreshButton().fire();
        });


        if (isAdmin()) {
            tabs.getTabs().addAll(scheduleTabFx, groupsTabFx, teachersTabFx, studentsTabFx, paymentTabFx,
                    spacer, languagesTabFx, levelsTabFx, usersTabFx);
        } else {
            tabs.getTabs().addAll(scheduleTabFx, groupsTabFx, paymentTabFx);
        }

        // ====== Layout ======
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabs);

        Scene scene = new Scene(root, 900, 650);

        // ====== Авто-логаут ======
        Runnable resetTimer = () -> restartTimer(stage, onLogout);
        ActivityMonitor.setOnActivity(resetTimer);
        ActivityMonitor.attach(scene);

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
        for (String role : roles) {
            roleSet.add(Role.valueOf(role.trim()));
        }
        return Role.getStrongest(roleSet).getName();
    }

    private boolean isAdmin() {
        return user.roles() != null && user.roles().contains(Role.ADMIN.name());
    }

    private boolean isTeacher() {
        return user.roles() != null && user.roles().contains(Role.TEACHER.name());
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
