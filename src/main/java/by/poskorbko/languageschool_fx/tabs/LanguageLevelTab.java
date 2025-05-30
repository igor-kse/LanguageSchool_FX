package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.TestData;
import by.poskorbko.languageschool_fx.dto.LevelScaleDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

public class LanguageLevelTab extends BaseTab {

    public static VBox createLevelsTable(List<LevelScaleDTO> scales)  {
        TableView<LevelScaleDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        // ==== КНОПКИ ====
        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #39d353; -fx-text-fill: #333; -fx-background-radius: 8;");
        addBtn.setOnAction(e -> {
            showLevelEditDialog(null, newScale -> {
                table.getItems().add(newScale);
                restAddLevelScale(newScale,
                        () -> { /* showSnackbar("Добавлено!"); */ },
                        () -> {
                            table.getItems().remove(newScale);
                            showAlert("Ошибка", "Не удалось добавить шкалу. Данные не изменены.");
                        }
                );
            });
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #ffd43b; -fx-text-fill: #333; -fx-background-radius: 8;");
        editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> {
            LevelScaleDTO selected = table.getSelectionModel().getSelectedItem();
            int selectedIdx = table.getSelectionModel().getSelectedIndex();
            if (selected != null) {
                showLevelEditDialog(selected, updated -> {
                    LevelScaleDTO old = table.getItems().get(selectedIdx);
                    table.getItems().set(selectedIdx, updated);
                    restUpdateLevelScale(updated,
                            () -> { /* showSnackbar("Изменено!"); */ },
                            () -> {
                                table.getItems().set(selectedIdx, old);
                                showAlert("Ошибка", "Не удалось сохранить изменения. Данные не изменены.");
                            }
                    );
                });
            }
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: #333; -fx-background-radius: 8;");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> {
            LevelScaleDTO selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить выбранную шкалу?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Подтверждение удаления");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        int oldIndex = table.getItems().indexOf(selected);
                        table.getItems().remove(selected);
                        restDeleteLevelScale(selected,
                                () -> { /* showSnackbar("Удалено!"); */ },
                                () -> {
                                    table.getItems().add(oldIndex, selected);
                                    showAlert("Ошибка", "Не удалось удалить. Данные не изменены.");
                                }
                        );
                    }
                });
            }
        });

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(e -> {
            // FIXME
            // Здесь — обновление с бэка (пока просто имитация)
            List<LevelScaleDTO> updatedScales = TestData.getTestLevelScale();
            table.getItems().setAll(updatedScales);
            // showSnackbar("Обновлено!");
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

    private static void showLevelEditDialog(LevelScaleDTO scale, Consumer<LevelScaleDTO> onSave) {
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
        grid.add(new Label("Название шкалы:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Уровни:"), 0, 1); grid.add(levelsBox, 1, 1);

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

    private static void restAddLevelScale(LevelScaleDTO scale, Runnable onSuccess, Runnable onFail) {
        new Thread(() -> {
            try { Thread.sleep(300); Platform.runLater(onSuccess); }
            catch (Exception e) { Platform.runLater(onFail); }
        }).start();
    }

    private static void restUpdateLevelScale(LevelScaleDTO scale, Runnable onSuccess, Runnable onFail) {
        new Thread(() -> {
            try { Thread.sleep(300); Platform.runLater(onSuccess); }
            catch (Exception e) { Platform.runLater(onFail); }
        }).start();
    }

    private static void restDeleteLevelScale(LevelScaleDTO scale, Runnable onSuccess, Runnable onFail) {
        new Thread(() -> {
            try { Thread.sleep(300); Platform.runLater(onSuccess); }
            catch (Exception e) { Platform.runLater(onFail); }
        }).start();
    }
}
