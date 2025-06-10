package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.StudentDTO;
import by.poskorbko.languageschool_fx.dto.UserDTO;
import by.poskorbko.languageschool_fx.http.CrudRestClient;
import by.poskorbko.languageschool_fx.util.JsonObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class StudentsTab extends BaseTab<StudentDTO> {

    private static final String BASE_PATH = "/students";

    public StudentsTab() {
        super(BASE_PATH);
    }

    public VBox createStudentsTable() {
        TableView<StudentDTO> table = getTable();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<StudentDTO, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().lastName() + " " + data.getValue().firstName()
        ));

        TableColumn<StudentDTO, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email()));

        TableColumn<StudentDTO, String> ageCol = new TableColumn<>("Возраст");
        ageCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().age())));

        TableColumn<StudentDTO, String> channelCol = new TableColumn<>("Канал");
        channelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().channel()));

        TableColumn<StudentDTO, String> hobbiesCol = new TableColumn<>("Увлечения");
        hobbiesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().hobbies()));

        TableColumn<StudentDTO, String> noteCol = new TableColumn<>("Заметка");
        noteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().note()));

        if (table.getColumns().isEmpty()) {
            table.getColumns().addAll(nameCol, emailCol, ageCol, channelCol, hobbiesCol, noteCol);
        }

        String message = "Удалить студента?";
        VBox vbox = new VBox(8, getButtons(message), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        CrudRestClient.getCall(BASE_PATH,
                response -> Platform.runLater(() -> {
                    try {
                        List<StudentDTO> students = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {});
                        table.getItems().setAll(students);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }),
                failed -> Platform.runLater(() -> System.err.println("Ошибка: " + failed.statusCode()))
        );

        return vbox;
    }

    @Override
    public Button getRefreshButton() {
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(event ->
                CrudRestClient.getCall(BASE_PATH,
                        response -> Platform.runLater(() -> {
                            try {
                                List<StudentDTO> entities = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {});
                                getTable().getItems().setAll(entities);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }),
                        failed -> Platform.runLater(() -> System.err.println("Ошибка: " + failed.statusCode()))
                )
        );
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        return getTable().getItems().get(index).id();
    }

    @Override
    protected void showEditDialog(StudentDTO student, Consumer<StudentDTO> onSave) {
        Dialog<StudentDTO> dialog = createDialog();
        boolean isNew = (student == null);
        dialog.setTitle(isNew ? "Добавить студента" : "Редактировать студента");

        ComboBox<UserDTO> userCombo = new ComboBox<>();
        userCombo.setMinWidth(280);
        userCombo.setPrefWidth(340);

        TextField firstNameField = new TextField(isNew ? "" : student.firstName());
        TextField lastNameField = new TextField(isNew ? "" : student.lastName());
        TextField emailField = new TextField(isNew ? "" : student.email());
        TextField ageField = new TextField(isNew ? "" : String.valueOf(isNew ? 0 : student.age()));
        TextField channelField = new TextField(isNew ? "" : student.channel());
        TextField hobbiesField = new TextField(isNew ? "" : student.hobbies());
        TextField noteField = new TextField(isNew ? "" : student.note());

        if (isNew) {
            // Запрашиваем всех студентов (для фильтрации)
            CrudRestClient.getCall("/students",
                    studentResponse -> Platform.runLater(() -> {
                        try {
                            List<StudentDTO> students = JsonObjectMapper.getInstance().readValue(studentResponse.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            List<String> existingStudentIds = students.stream().map(StudentDTO::id).toList();

                            // Теперь запрашиваем всех пользователей
                            CrudRestClient.getCall("/users",
                                    userResponse -> Platform.runLater(() -> {
                                        try {
                                            List<UserDTO> users = JsonObjectMapper.getInstance().readValue(userResponse.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});

                                            // Оставляем только тех, кто НЕ студент
                                            List<UserDTO> filtered = users.stream()
                                                    .filter(u -> !existingStudentIds.contains(u.id()))
                                                    .toList();

                                            userCombo.getItems().add(null); // <Новый пользователь>
                                            userCombo.getItems().addAll(filtered);

                                            userCombo.setCellFactory(listView -> new ListCell<>() {
                                                @Override
                                                protected void updateItem(UserDTO user, boolean empty) {
                                                    super.updateItem(user, empty);
                                                    if (empty) {
                                                        setText("");
                                                    } else if (user == null) {
                                                        setText("<Новый пользователь>");
                                                    } else {
                                                        setText(user.lastName() + " " + user.firstName() + " (" + user.email() + ")");
                                                    }
                                                }
                                            });
                                            userCombo.setButtonCell(new ListCell<>() {
                                                @Override
                                                protected void updateItem(UserDTO user, boolean empty) {
                                                    super.updateItem(user, empty);
                                                    if (empty) {
                                                        setText("");
                                                    } else if (user == null) {
                                                        setText("<Новый пользователь>");
                                                    } else {
                                                        setText(user.lastName() + " " + user.firstName() + " (" + user.email() + ")");
                                                    }
                                                }
                                            });

                                            // биндинг editability
                                            userCombo.valueProperty().addListener((obs, oldUser, newUser) -> {
                                                boolean editable = (newUser == null);
                                                firstNameField.setEditable(editable);
                                                lastNameField.setEditable(editable);
                                                emailField.setEditable(editable);
                                                if (newUser != null) {
                                                    firstNameField.setText(newUser.firstName());
                                                    lastNameField.setText(newUser.lastName());
                                                    emailField.setText(newUser.email());
                                                } else {
                                                    firstNameField.clear();
                                                    lastNameField.clear();
                                                    emailField.clear();
                                                }
                                            });

                                            userCombo.getSelectionModel().selectFirst();

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }),
                                    failed -> Platform.runLater(() -> System.err.println("Ошибка загрузки пользователей: " + (failed != null ? failed.statusCode() : "нет ответа")))
                            );

                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }),
                    failed -> Platform.runLater(() -> System.err.println("Ошибка загрузки студентов: " + (failed != null ? failed.statusCode() : "нет ответа")))
            );
        }

        firstNameField.setEditable(isNew);
        lastNameField.setEditable(isNew);
        emailField.setEditable(isNew);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8); grid.setPadding(new Insets(24));
        grid.setPrefWidth(420);

        int row = 0;
        if (isNew) {
            grid.add(new Label("Пользователь:"), 0, row);
            grid.add(userCombo, 1, row++);
        }
        grid.add(new Label("Имя:"), 0, row); grid.add(firstNameField, 1, row++);
        grid.add(new Label("Фамилия:"), 0, row); grid.add(lastNameField, 1, row++);
        grid.add(new Label("Email:"), 0, row); grid.add(emailField, 1, row++);
        grid.add(new Label("Возраст:"), 0, row); grid.add(ageField, 1, row++);
        grid.add(new Label("Канал:"), 0, row); grid.add(channelField, 1, row++);
        grid.add(new Label("Увлечения:"), 0, row); grid.add(hobbiesField, 1, row++);
        grid.add(new Label("Заметка:"), 0, row); grid.add(noteField, 1, row++);

        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // === Динамический контроль активности кнопки OK ===
        dialog.setOnShown(event -> {
            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (isNew) {
                okButton.disableProperty().bind(
                        // Кнопка активна, если ИЛИ выбран пользователь, ИЛИ заполнены основные поля
                        userCombo.valueProperty().isNull()
                                .and(firstNameField.textProperty().isEmpty()
                                        .or(lastNameField.textProperty().isEmpty())
                                        .or(emailField.textProperty().isEmpty()))
                );
            } else {
                okButton.disableProperty().bind(
                        firstNameField.textProperty().isEmpty()
                                .or(lastNameField.textProperty().isEmpty())
                                .or(emailField.textProperty().isEmpty())
                );
            }
        });

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String requiredFields = "";
            if (userCombo.getSelectionModel().getSelectedIndex() == -1) {
                requiredFields += "        Пользователь\n";
            }
            if (firstNameField.getText().isEmpty()) {
                requiredFields += "        Имя\n";
            }
            if (lastNameField.getText().isEmpty()) {
                requiredFields += "        Фамилия\n";
            }
            if (emailField.getText().isEmpty()) {
                requiredFields += "        email\n";
            }
            if (ageField.getText().isEmpty()) {
                requiredFields += "        Возраст\n";
            }
            if (channelField.getText().isEmpty()) {
                requiredFields += "        Канал\n";
            }
            if (!requiredFields.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Пожалуйста, заполните поля:\n\n" + requiredFields, ButtonType.OK);
                alert.setHeaderText("Обязательное поле");
                alert.showAndWait();
                event.consume(); // Не закрывать диалог!
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String id;
                String firstName, lastName, email;
                if (isNew && userCombo.getValue() != null) {
                    UserDTO u = userCombo.getValue();
                    id = u.id();
                    firstName = u.firstName();
                    lastName = u.lastName();
                    email = u.email();
                } else {
                    id = isNew ? null : student.id();
                    firstName = firstNameField.getText().trim();
                    lastName = lastNameField.getText().trim();
                    email = emailField.getText().trim();
                }
                int age = 0;
                try { age = Integer.parseInt(ageField.getText().trim()); } catch (Exception ignored) {}
                String channel = channelField.getText().trim();
                String hobbies = hobbiesField.getText().trim();
                String note = noteField.getText().trim();
                if (id != null || (!firstName.isEmpty() && !lastName.isEmpty() && !email.isEmpty())) {
                    return new StudentDTO(
                            id,
                            age,
                            firstName,
                            lastName,
                            email,
                            channel,
                            hobbies,
                            note
                    );
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }


}
