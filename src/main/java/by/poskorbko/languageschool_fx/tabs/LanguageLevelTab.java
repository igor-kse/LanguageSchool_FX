package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.LevelScaleDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class LanguageLevelTab extends BaseTab<LevelScaleDTO> {

    public LanguageLevelTab() {
        super("/languages/levels");
    }

    public VBox createLevelsTable(List<LevelScaleDTO> scales) {
        TableView<LevelScaleDTO> table = getTable();

        TableColumn<LevelScaleDTO, String> scaleCol = new TableColumn<>("Шкала");
        scaleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<LevelScaleDTO, String> levelsCol = new TableColumn<>("Уровни");
        levelsCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.join(", ", data.getValue().levels())
        ));
        // Альтернатива — уровни тегами (HBox), если хочешь красивее:
        // levelsCol.setCellFactory(col -> new TableCell<>() {
        //     @Override
        //     protected void updateItem(String item, boolean empty) {
        //         super.updateItem(item, empty);
        //         if (empty || item == null) {
        //             setGraphic(null);
        //         } else {
        //             HBox box = new HBox(6);
        //             for (String lvl : item.split(",\\s*")) {
        //                 Label tag = new Label(lvl);
        //                 tag.setStyle("-fx-background-color:#eef; -fx-background-radius:7; -fx-padding:2 8; -fx-font-size:13;");
        //                 box.getChildren().add(tag);
        //             }
        //             setGraphic(box);
        //         }
        //     }
        // });

        table.getColumns().addAll(scaleCol, levelsCol);
        table.getItems().addAll(scales);

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    @Override
    protected void showEditDialog(LevelScaleDTO scale, Consumer<LevelScaleDTO> onSave) {
        boolean isNew = (scale == null);
        Dialog<LevelScaleDTO> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Добавить шкалу" : "Редактировать шкалу");

        TextField nameField = new TextField(isNew ? "" : scale.name());

        // Таблица для уровней
        TableView<String> levelsTable = new TableView<>();
        levelsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        levelsTable.setPrefHeight(180);
        TableColumn<String, String> levelCol = new TableColumn<>("Уровень");
        levelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
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
                if (!val.trim().isEmpty() && !levelsTable.getItems().contains(val.trim())) {
                    levelsTable.getItems().add(val.trim());
                }
            });
        });

        editBtn.disableProperty().bind(levelsTable.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> {
            String selected = levelsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                TextInputDialog editDialog = new TextInputDialog(selected);
                editDialog.setTitle("Редактировать уровень");
                editDialog.setHeaderText(null);
                editDialog.setContentText("Изменить уровень:");
                editDialog.showAndWait().ifPresent(val -> {
                    if (!val.trim().isEmpty() && !levelsTable.getItems().contains(val.trim())) {
                        int idx = levelsTable.getSelectionModel().getSelectedIndex();
                        levelsTable.getItems().set(idx, val.trim());
                    }
                });
            }
        });

        deleteBtn.disableProperty().bind(levelsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> {
            String selected = levelsTable.getSelectionModel().getSelectedItem();
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
        grid.add(new Label("Уровни:"), 0, 1);
        grid.add(levelsBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name = nameField.getText().trim();
                List<String> levels = levelsTable.getItems().stream()
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (!name.isEmpty() && !levels.isEmpty()) {
                    return new LevelScaleDTO(name, levels);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }
}
