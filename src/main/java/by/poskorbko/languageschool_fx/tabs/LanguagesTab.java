package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.LanguageEntryDTO;
import by.poskorbko.languageschool_fx.dto.LanguageScaleDTO;
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

public class LanguagesTab extends BaseTab<LanguageEntryDTO> {

    private static final String BASE_PATH = "/languages";

    public LanguagesTab() {
        super(BASE_PATH);
    }

    public VBox createLanguagesTable() {
        TableView<LanguageEntryDTO> table = getTable();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LanguageEntryDTO, String> langCol = new TableColumn<>("Язык");
        langCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<LanguageEntryDTO, String> scaleCol = new TableColumn<>("Шкала");
        scaleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().scale()));

        TableColumn<LanguageEntryDTO, String> noteCol = new TableColumn<>("Заметка");
        noteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().note()));

        if (table.getColumns().isEmpty()) {
            table.getColumns().addAll(List.of(langCol, scaleCol, noteCol));
        }

        String message = "Удалить язык?";
        VBox vbox = new VBox(8, getButtons(message), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        CrudRestClient.getCall(BASE_PATH,
                response -> Platform.runLater(() -> {
                    try {
                        List<LanguageEntryDTO> languages = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {
                        });
                        table.getItems().setAll(languages);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }),
                response -> Platform.runLater(() -> {
                    System.out.println(response.statusCode());
                }));

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
                                List<LanguageEntryDTO> entities = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {
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
        return getTable().getItems().get(index).name();
    }

    protected void showEditDialog(LanguageEntryDTO entry, Consumer<LanguageEntryDTO> onSave) {
        boolean isNew = (entry == null);
        Dialog<LanguageEntryDTO> dialog = createDialog();
        dialog.setTitle(isNew ? "Добавить язык" : "Редактировать язык");

        CrudRestClient.getCall("/scales",
                response -> Platform.runLater(() -> {
                    try {
                        List<LanguageScaleDTO> scales = JsonObjectMapper.getInstance().readValue(response.body(), new TypeReference<>() {
                        });
                        buildWithScales(entry, onSave, isNew, scales, dialog);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        showAlert("Ошибка", "Ошибка разбора шкал");
                    }
                }),
                failedResponse -> Platform.runLater(() ->
                        showAlert("Ошибка", "Ошибка загрузки шкал: " + (failedResponse == null ? "" : failedResponse.statusCode()))));
    }

    private static void buildWithScales(LanguageEntryDTO entry, Consumer<LanguageEntryDTO> onSave, boolean isNew, List<LanguageScaleDTO> scales, Dialog<LanguageEntryDTO> dialog) {
        TextField langField = new TextField(isNew ? "" : entry.name());
        ComboBox<String> scaleBox = new ComboBox<>();
        List<String> names = scales.stream().map(LanguageScaleDTO::name).toList();
        scaleBox.getItems().addAll(names);
        scaleBox.setValue(isNew ? (names.isEmpty() ? "" : names.get(0)) : entry.scale());

        TextField noteField = new TextField(isNew ? "" : entry.note());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Язык:"), 0, 0);
        grid.add(langField, 1, 0);
        grid.add(new Label("Шкала:"), 0, 1);
        grid.add(scaleBox, 1, 1);
        grid.add(new Label("Заметка:"), 0, 2);
        grid.add(noteField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String requiredFields = "";
            if (langField.getText().trim().isEmpty()) {
                requiredFields += "        Язык\n";
            }
            if (scaleBox.getSelectionModel().isEmpty()) {
                requiredFields += "        Шкала\n";
            }
            if (noteField.getText().isEmpty()) {
                requiredFields += "        Заметка\n";
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
                String language = langField.getText().trim();
                String scale = scaleBox.getValue();
                String note = noteField.getText().trim();
                return new LanguageEntryDTO(language, scale, note);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }
}
