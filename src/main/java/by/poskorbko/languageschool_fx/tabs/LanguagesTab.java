package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.TestData;
import by.poskorbko.languageschool_fx.dto.LanguageEntryDTO;
import by.poskorbko.languageschool_fx.dto.LevelScaleDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

public class LanguagesTab extends BaseTab<LanguageEntryDTO> {

    public LanguagesTab() {
        super("/languages");
    }

    @Override
    protected Button getRefreshButton() {
        return new Button();
    }

    @Override
    protected String getSelectedUuid(int index) {
        return "";
    }

    public VBox createLanguagesTable(List<LanguageEntryDTO> entries) {
        TableView<LanguageEntryDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LanguageEntryDTO, String> langCol = new TableColumn<>("Язык");
        langCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().language()));

        TableColumn<LanguageEntryDTO, String> scaleCol = new TableColumn<>("Шкала");
        scaleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().scale()));

        TableColumn<LanguageEntryDTO, String> noteCol = new TableColumn<>("Заметка");
        noteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().note()));

        table.getColumns().addAll(langCol, scaleCol, noteCol);
        table.getItems().addAll(entries);

        // ==== КНОПКИ ====
        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #39d353; -fx-text-fill: #333; -fx-background-radius: 8;");
        addBtn.setOnAction(e -> {
            showEditDialog(null, newEntry -> {
                table.getItems().add(newEntry);
//                restAddCall(newEntry,
//                        () -> { /* showSnackbar("Добавлено!"); */ },
//                        () -> {
//                            table.getItems().remove(newEntry);
//                            showAlert("Ошибка", "Не удалось добавить язык. Данные не изменены.");
//                        }
//                );
            });
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #ffd43b; -fx-text-fill: #333; -fx-background-radius: 8;");
        editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> {
            LanguageEntryDTO selected = table.getSelectionModel().getSelectedItem();
            int selectedIdx = table.getSelectionModel().getSelectedIndex();
            if (selected != null) {
                showEditDialog(selected, updated -> {
                    LanguageEntryDTO old = table.getItems().get(selectedIdx);
                    table.getItems().set(selectedIdx, updated);
//                    restUpdateCall(updated,
//                            () -> { /* showSnackbar("Изменено!"); */ },
//                            () -> {
//                                table.getItems().set(selectedIdx, old);
//                                showAlert("Ошибка", "Не удалось сохранить изменения. Данные не изменены.");
//                            }
//                    );
                });
            }
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: #333; -fx-background-radius: 8;");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> {
            LanguageEntryDTO selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить выбранный язык?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Подтверждение удаления");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        int oldIndex = table.getItems().indexOf(selected);
                        table.getItems().remove(selected);
//                        restDeleteCall(selected,
//                                () -> { /* showSnackbar("Удалено!"); */ },
//                                () -> {
//                                    table.getItems().add(oldIndex, selected);
//                                    showAlert("Ошибка", "Не удалось удалить. Данные не изменены.");
//                                }
//                        );
                    }
                });
            }
        });

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(e -> {
            // FIXME: здесь будет обновление с бэка
            List<LanguageEntryDTO> updatedEntries = TestData.getTestLanguageEntries();
            table.getItems().setAll(updatedEntries);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(10, addBtn, editBtn, deleteBtn, spacer, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(0, 0, 10, 0));

        VBox vbox = new VBox(8, buttons, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    protected void showEditDialog(LanguageEntryDTO entry,  Consumer<LanguageEntryDTO> onSave) {
        boolean isNew = (entry == null);
        Dialog<LanguageEntryDTO> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Добавить язык" : "Редактировать язык");

        //FIXME
        var availableScales = TestData.getTestLevelScale().stream().map(LevelScaleDTO::name).toList();

        TextField langField = new TextField(isNew ? "" : entry.language());
        ComboBox<String> scaleBox = new ComboBox<>();
        scaleBox.getItems().addAll(availableScales);
        scaleBox.setValue(isNew ? (availableScales.isEmpty() ? "" : availableScales.get(0)) : entry.scale());

        TextField noteField = new TextField(isNew ? "" : entry.note());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Язык:"), 0, 0); grid.add(langField, 1, 0);
        grid.add(new Label("Шкала:"), 0, 1); grid.add(scaleBox, 1, 1);
        grid.add(new Label("Заметка:"), 0, 2); grid.add(noteField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String language = langField.getText().trim();
                String scale = scaleBox.getValue();
                String note = noteField.getText().trim();
                if (!language.isEmpty() && scale != null && !scale.isEmpty()) {
                    return new LanguageEntryDTO(language, scale, note);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }
}
