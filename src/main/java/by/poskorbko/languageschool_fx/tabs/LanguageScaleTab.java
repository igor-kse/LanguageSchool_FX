package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.LanguageScaleDTO;
import by.poskorbko.languageschool_fx.dto.LanguageScaleLevelDTO;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class LanguageScaleTab extends BaseTab<LanguageScaleDTO> {

    private static final String BASE_PATH = "/scales";

    public LanguageScaleTab() {
        super(BASE_PATH);
    }

    public VBox createLevelsTable() {
        TableView<LanguageScaleDTO> table = getTable();

        TableColumn<LanguageScaleDTO, String> scaleCol = new TableColumn<>("Шкала");
        scaleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<LanguageScaleDTO, Set<LanguageScaleLevelDTO>> levelsCol = new TableColumn<>("Уровни");
        levelsCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().levels()));
        levelsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Set<LanguageScaleLevelDTO> levels, boolean empty) {
                super.updateItem(levels, empty);
                if (empty || levels == null || levels.isEmpty()) {
                    setText("");
                } else {
                    String names = levels.stream()
                            .map(LanguageScaleLevelDTO::name)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
                    setText(names);
                }
            }
        });

        table.getColumns().addAll(List.of(scaleCol, levelsCol));

        // Получаем с бэка и обновляем таблицу
        reloadTable();

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    private void reloadTable() {
        CrudRestClient.getCall(BASE_PATH,
                response -> Platform.runLater(() -> {
                    try {
                        List<LanguageScaleDTO> scales = JsonObjectMapper.getInstance().readValue(
                                response.body(), new TypeReference<>() {});
                        getTable().getItems().setAll(scales);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }),
                response -> Platform.runLater(() -> {
                    System.out.println(response.statusCode());
                }));
    }

    @Override
    public Button getRefreshButton() {
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(event -> reloadTable());
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        // Для идентификации используем name
        return getTable().getItems().get(index).name();
    }

    @Override
    protected void showEditDialog(LanguageScaleDTO scale, Consumer<LanguageScaleDTO> onSave) {
        boolean isNew = (scale == null);
        String oldName = isNew ? null : scale.name();

        Dialog<LanguageScaleDTO> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Добавить шкалу" : "Редактировать шкалу");

        TextField nameField = new TextField(isNew ? "" : scale.name());
        TextArea descriptionField = new TextArea(isNew ? "" : scale.description());
        descriptionField.setPromptText("Описание");
        descriptionField.setPrefRowCount(2);

        // Таблица для уровней
        TableView<LanguageScaleLevelDTO> levelsTable = new TableView<>();
        levelsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        levelsTable.setPrefHeight(180);
        TableColumn<LanguageScaleLevelDTO, String> levelCol = new TableColumn<>("Уровень");
        levelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        levelsTable.getColumns().add(levelCol);

        // Заполняем уровнями
        if (scale != null && scale.levels() != null) {
            levelsTable.getItems().addAll(scale.levels());
        }

        // Кнопки для управления уровнями
        Button addBtn = new Button("Добавить");
        Button editBtn = new Button("Редактировать");
        Button deleteBtn = new Button("Удалить");

        addBtn.setOnAction(e -> {
            TextInputDialog addDialog = new TextInputDialog();
            addDialog.setTitle("Добавить уровень");
            addDialog.setHeaderText(null);
            addDialog.setContentText("Введите новый уровень:");
            addDialog.showAndWait().ifPresent(val -> {
                String name = val.trim();
                if (!name.isEmpty() && levelsTable.getItems().stream().noneMatch(l -> l.name().equalsIgnoreCase(name))) {
                    levelsTable.getItems().add(new LanguageScaleLevelDTO(java.util.UUID.randomUUID().toString(), name));
                }
            });
        });

        editBtn.disableProperty().bind(levelsTable.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> {
            LanguageScaleLevelDTO selected = levelsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                TextInputDialog editDialog = new TextInputDialog(selected.name());
                editDialog.setTitle("Редактировать уровень");
                editDialog.setHeaderText(null);
                editDialog.setContentText("Изменить уровень:");
                editDialog.showAndWait().ifPresent(val -> {
                    String newName = val.trim();
                    if (!newName.isEmpty() && levelsTable.getItems().stream().noneMatch(l -> l.name().equalsIgnoreCase(newName))) {
                        int idx = levelsTable.getSelectionModel().getSelectedIndex();
                        levelsTable.getItems().set(idx, new LanguageScaleLevelDTO(selected.id(), newName));
                    }
                });
            }
        });

        deleteBtn.disableProperty().bind(levelsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> {
            LanguageScaleLevelDTO selected = levelsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                levelsTable.getItems().remove(selected);
            }
        });

        HBox levelBtns = new HBox(10, addBtn, editBtn, deleteBtn);
        levelBtns.setAlignment(Pos.CENTER_LEFT);
        VBox levelsBox = new VBox(6, levelsTable, levelBtns);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Название шкалы:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Уровни:"), 0, 2);
        grid.add(levelsBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name = nameField.getText().trim();
                String description = descriptionField.getText().trim();
                List<LanguageScaleLevelDTO> levels = levelsTable.getItems();
                if (!name.isEmpty() && !levels.isEmpty()) {
                    return new LanguageScaleDTO(name, description, new java.util.LinkedHashSet<>(levels));
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedScale -> {
            if (updatedScale != null) {
                if (isNew) {
                    CrudRestClient.addPostCall(BASE_PATH, updatedScale,
                            resp -> Platform.runLater(this::reloadTable),
                            err -> Platform.runLater(() -> showAlert("Ошибка", "Не удалось добавить шкалу")));
                } else {
                    // Важно: ЭКРАНИРОВАТЬ oldName для URI!
                    String encodedOldName = URLEncoder.encode(oldName, StandardCharsets.UTF_8);
                    CrudRestClient.putCall(BASE_PATH + "/" + encodedOldName, updatedScale,
                            resp -> Platform.runLater(this::reloadTable),
                            err -> Platform.runLater(() -> showAlert("Ошибка", "Не удалось обновить шкалу")));
                }
            }
        });
    }
}
