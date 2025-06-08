package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.LanguageEntryDTO;
import by.poskorbko.languageschool_fx.dto.TeacherDTO;
import by.poskorbko.languageschool_fx.http.CrudRestClient;
import by.poskorbko.languageschool_fx.util.JsonObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;

import java.util.List;
import java.util.function.Consumer;

public class TeacherTab extends BaseTab<TeacherDTO> {

    private static final String BASE_PATH = "/teachers";

    public TeacherTab() {
        super(BASE_PATH);
    }

    public VBox createTeachersTable() {
        TableView<TeacherDTO> table = getTable();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TeacherDTO, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().lastName() + " " + data.getValue().firstName()
        ));

        TableColumn<TeacherDTO, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email()));

        TableColumn<TeacherDTO, String> educationCol = new TableColumn<>("Образование");
        educationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().education()));

        TableColumn<TeacherDTO, String> langsCol = new TableColumn<>("Языки");
        langsCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.join(", ", data.getValue().languages()))
        );

        if (table.getColumns().isEmpty()) {
            table.getColumns().addAll(List.of(nameCol, emailCol, educationCol, langsCol));
        }

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        CrudRestClient.getCall(BASE_PATH,
                response -> Platform.runLater(() -> {
                    try {
                        List<TeacherDTO> teachers = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {
                        });
                        table.getItems().setAll(teachers);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }),
                response -> Platform.runLater(() -> System.err.println("Ошибка: " + response.statusCode()))
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
                                List<TeacherDTO> entities = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {
                                });
                                getTable().getItems().setAll(entities);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }),
                        response -> Platform.runLater(() -> {
                            System.out.println(response.statusCode());
                        })));
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        return getTable().getItems().get(index).id();
    }

    protected void showEditDialog(TeacherDTO teacher, Consumer<TeacherDTO> onSave) {
        Dialog<TeacherDTO> dialog = new Dialog<>();
        dialog.setTitle(teacher == null ? "Добавить учителя" : "Редактировать учителя");

        // остальные поля...

        TextField educationField = new TextField(teacher == null ? "" : teacher.education());

        // 1. Загружаем список языков (асинхронно!)
        CrudRestClient.getCall("/languages",
                response -> Platform.runLater(() -> {
                    try {
                        List<LanguageEntryDTO> languageNames = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {});
                        buildWithLanguages(teacher, onSave, dialog, educationField, languageNames.stream().map(LanguageEntryDTO::name).toList());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }),
                failedResponse -> Platform.runLater(() -> System.err.println("Языки: " + failedResponse.statusCode()))
        );
    }

    private static void buildWithLanguages(
            TeacherDTO teacher,
            Consumer<TeacherDTO> onSave,
            Dialog<TeacherDTO> dialog,
            TextField educationField,
            List<String> availableLanguages
    ) {
        TextField firstNameField = new TextField(teacher == null ? "" : teacher.firstName());
        TextField lastNameField = new TextField(teacher == null ? "" : teacher.lastName());
        TextField emailField = new TextField(teacher == null ? "" : teacher.email());

        CheckComboBox<String> langsCombo = new CheckComboBox<>();
        langsCombo.getItems().addAll(availableLanguages);

        if (teacher != null && teacher.languages() != null) {
            for (String lang : teacher.languages()) {
                langsCombo.getCheckModel().check(lang);
            }
        }

        boolean readOnly = (teacher != null);
        firstNameField.setEditable(!readOnly);
        lastNameField.setEditable(!readOnly);
        emailField.setEditable(!readOnly);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8); grid.setPadding(new Insets(24));
        grid.add(new Label("Имя:"), 0, 0); grid.add(firstNameField, 1, 0);
        grid.add(new Label("Фамилия:"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailField, 1, 2);
        grid.add(new Label("Образование:"), 0, 3); grid.add(educationField, 1, 3);
        grid.add(new Label("Языки:"), 0, 4); grid.add(langsCombo, 1, 4);
        grid.setPrefWidth(300);

        dialog.getDialogPane().setPrefWidth(320);
        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // --- Валидация на пустое образование ---
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (educationField.getText().trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Пожалуйста, заполните поле \"Образование\".", ButtonType.OK);
                alert.setHeaderText("Обязательное поле");
                alert.showAndWait();
                event.consume(); // Не закрывать диалог!
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == okButtonType) {
                String id = teacher == null ? null : teacher.id();
                String firstname = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();
                String education = educationField.getText().trim();
                List<String> langs = langsCombo.getCheckModel().getCheckedItems();

                if (!firstname.isEmpty() && !lastName.isEmpty() && !education.isEmpty() && !langs.isEmpty() && !email.isEmpty()) {
                    return new TeacherDTO(id, firstname, lastName, education, langs, email);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }


    @Override
    protected Button getEditButton() {
        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #ffd43b; -fx-text-fill: #333; -fx-background-radius: 8;");
        editBtn.disableProperty().bind(getTable().getSelectionModel().selectedItemProperty().isNull());

        EventHandler<ActionEvent> handler = event -> {
            TeacherDTO selected = getTable().getSelectionModel().getSelectedItem();
            showEditDialog(selected,
                    entityToUpdate -> CrudRestClient.patchCall(
                            BASE_PATH, entityToUpdate,
                            successResponse -> Platform.runLater(() -> getRefreshButton().fire()),
                            failResponse -> System.err.println("Failed to edit: " + failResponse.statusCode() + " " + failResponse.body())
                    ));
        };
        editBtn.setOnAction(handler);

        return editBtn;
    }
}
