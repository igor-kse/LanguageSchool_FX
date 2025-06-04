package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.Role;
import by.poskorbko.languageschool_fx.dto.UserDTO;
import by.poskorbko.languageschool_fx.http.CrudRestClient;
import by.poskorbko.languageschool_fx.util.JsonObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.controlsfx.control.CheckComboBox;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class UsersTab extends BaseTab<UserDTO> {

    private static final String BASE_PATH = "/users";

    public UsersTab() {
        super(BASE_PATH);
    }

    public VBox createUsersTable() {
        TableView<UserDTO> table = getTable();

        TableColumn<UserDTO, UserDTO> avatarCol = createAvatarColumn();
        TableColumn<UserDTO, String> nameCol = new TableColumn<>("Имя");
        TableColumn<UserDTO, String> emailCol = new TableColumn<>("Email");
        TableColumn<UserDTO, String> rolesCol = new TableColumn<>("Роли");

        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().firstName() + " " + data.getValue().lastName()));
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email()));
        rolesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().roles().toString()));

        table.getColumns().addAll(List.of(avatarCol, nameCol, emailCol, rolesCol));

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        CrudRestClient.getCall(BASE_PATH,
                response -> Platform.runLater(() -> {
                    try {
                        List<UserDTO> users = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {});
                        table.getItems().setAll(users);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }),
                response -> Platform.runLater(() -> {
                    System.out.println(response.statusCode());
                }));

        return vbox;
    }

    protected Button getRefreshButton() {
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(event ->
                CrudRestClient.getCall(BASE_PATH,
                        response -> Platform.runLater(() -> {
                            try {
                                List<UserDTO> entities = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {});
                                getTable().getItems().setAll(entities);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }),
                        failedResponse -> Platform.runLater(() -> System.err.println(failedResponse.statusCode()))));
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        return getTable().getItems().get(index).id();
    }

    private @NotNull TableColumn<UserDTO, UserDTO> createAvatarColumn() {
        TableColumn<UserDTO, UserDTO> avatarCol = new TableColumn<>("Аватар");
        avatarCol.setPrefWidth(56);
        avatarCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        avatarCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(UserDTO user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createAvatarView(user.avatarBase64(), 32));
                    setAlignment(Pos.CENTER);
                }
            }
        });
        return avatarCol;
    }

    @Override
    protected void showEditDialog(UserDTO user, Consumer<UserDTO> onSave) {
        boolean isNew = (user == null);

        Dialog<UserDTO> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Добавить пользователя" : "Редактировать пользователя");

        TextField firstNameField = new TextField(isNew ? "" : user.firstName());
        TextField lastNameField = new TextField(isNew ? "" : user.lastName());
        TextField emailField = new TextField(isNew ? "" : user.email());
        PasswordField passwordField = new PasswordField();

        // ==== Drag&Drop + Click аватарка ====
        String base64Avatar = user == null ? "" : (user.avatarBase64() == null ? "" : user.avatarBase64());
        ImageView avatarView = createAvatarView(base64Avatar, 64);
        final String[] avatarBase64 = {base64Avatar};

        Runnable avatarChooser = () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Выберите аватарку (PNG, JPG)");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg")
            );
            File f = fc.showOpenDialog(null);
            if (f != null) {
                String newBase64 = encodeImageToBase64(f, 64, 64);
                if (newBase64 != null) {
                    avatarBase64[0] = newBase64;
                    avatarView.setImage(decodeBase64ToImage(newBase64, 64));
                }
            }
        };

        avatarView.setOnDragOver(event -> {
            if (event.getGestureSource() != avatarView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        avatarView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                String newBase64 = encodeImageToBase64(file, 64, 64);
                if (newBase64 != null) {
                    avatarBase64[0] = newBase64;
                    avatarView.setImage(decodeBase64ToImage(newBase64, 64));
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        avatarView.setOnMouseClicked(e -> avatarChooser.run());

        Button uploadButton = new Button("Загрузить...");
        uploadButton.setOnAction(e -> avatarChooser.run());

        Button removeButton = new Button("Удалить");
        removeButton.setOnAction(e -> {
            avatarBase64[0] = "";
            avatarView.setImage(fallbackAvatar(64));
        });

        HBox avatarButtons = new HBox(8, uploadButton, removeButton);
        avatarButtons.setAlignment(Pos.CENTER);

        CheckComboBox<Role> rolesCombo = new CheckComboBox<>();
        rolesCombo.getItems().addAll(Role.values());
        rolesCombo.setPrefWidth(320);

        if (!isNew && user.roles() != null) {
            for (String role : user.roles()) {
                rolesCombo.getCheckModel().check(Role.valueOf(role));
            }
        }

        // ==== Grid ====
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Имя:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Фамилия:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Пароль:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Роли:"), 0, 4);
        grid.add(rolesCombo, 1, 4);
        grid.add(new Label("Аватар:"), 0, 5);
        grid.add(avatarView, 1, 5);
        grid.add(avatarButtons, 1, 6, 2, 1);
        grid.setPrefWidth(390);

        dialog.getDialogPane().setPrefWidth(440);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();
                String password = passwordField.getText().trim();
                List<String> roles = rolesCombo.getCheckModel().getCheckedItems()
                        .stream().map(Role::name).toList();
                if (!firstName.isEmpty() && !lastName.isEmpty() && !email.isEmpty()) {
                    String id = isNew ? null : user.id();
                    return new UserDTO(id, firstName, lastName, email, password, roles, avatarBase64[0]);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }

    private ImageView createAvatarView(String base64, int size) {
        Image image;
        if (base64 == null || base64.isEmpty()) {
            image = fallbackAvatar(size);
        } else {
            image = decodeBase64ToImage(base64, size);
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        return view;
    }

    private Image fallbackAvatar(int size) {
        return new Image(getClass().getResource("/by/poskorbko/languageschool_fx/no_avatar.png").toExternalForm(), size, size, true, true);
    }

    private Image decodeBase64ToImage(String base64, int size) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new Image(new ByteArrayInputStream(bytes), size, size, true, true);
        } catch (Exception e) {
            return fallbackAvatar(size);
        }
    }

    private String encodeImageToBase64(File file, int w, int h) {
        try (InputStream is = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Image img = new Image(is, w, h, true, true);
            javafx.embed.swing.SwingFXUtils.fromFXImage(img, null); // preload
            BufferedImage bimg = javafx.embed.swing.SwingFXUtils.fromFXImage(img, null);
            javax.imageio.ImageIO.write(bimg, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}
