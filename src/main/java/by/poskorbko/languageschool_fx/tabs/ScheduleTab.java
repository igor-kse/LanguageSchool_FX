package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.TestData;
import by.poskorbko.languageschool_fx.dto.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

public class ScheduleTab extends BaseTab {
    public static VBox createScheduleTable(List<ScheduleDTO> schedules) {
        TableView<ScheduleDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ScheduleDTO, String> groupCol = new TableColumn<>("Группа");
        groupCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().group().name()));

        TableColumn<ScheduleDTO, String> languageCol = new TableColumn<>("Язык");
        languageCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().group().language().name()));

        TableColumn<ScheduleDTO, String> levelCol = new TableColumn<>("Уровень");
        levelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().group().grade().level().toString()));

        TableColumn<ScheduleDTO, String> dayCol = new TableColumn<>("День");
        dayCol.setCellValueFactory(data -> new SimpleStringProperty(TestData.dayOfWeekRus(data.getValue().dayOfWeek())));

        TableColumn<ScheduleDTO, String> timeCol = new TableColumn<>("Время");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().startTime() + " — " + data.getValue().endTime()
        ));

        table.getColumns().addAll(groupCol, languageCol, levelCol, dayCol, timeCol);
        table.getItems().addAll(schedules);

        // ==== КНОПКИ ====
        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #39d353; -fx-text-fill: #333; -fx-background-radius: 8;");
        addBtn.setOnAction(e -> {
            showEditDialog(null, newSchedule -> {
                table.getItems().add(newSchedule);
                restAddSchedule(newSchedule,
                        () -> { /* showSnackbar("Занятие добавлено!"); */ },
                        () -> {
                            table.getItems().remove(newSchedule);
                            showAlert("Ошибка", "Не удалось добавить запись. Данные не изменены.");
                        }
                );
            });
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #ffd43b; -fx-text-fill: #333; -fx-background-radius: 8;");
        editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> {
            ScheduleDTO selected = table.getSelectionModel().getSelectedItem();
            int selectedIdx = table.getSelectionModel().getSelectedIndex();
            if (selected != null) {
                showEditDialog(selected, updatedDto -> {
                    ScheduleDTO oldDto = table.getItems().get(selectedIdx);
                    table.getItems().set(selectedIdx, updatedDto);
                    restUpdateSchedule(updatedDto,
                            () -> { /* showSnackbar("Изменения сохранены!"); */ },
                            () -> {
                                table.getItems().set(selectedIdx, oldDto);
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
            ScheduleDTO selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить выбранное занятие?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Подтверждение удаления");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        int oldIndex = table.getItems().indexOf(selected);
                        table.getItems().remove(selected);
                        restDeleteSchedule(selected,
                                () -> { /* showSnackbar("Удалено!"); */ },
                                () -> {
                                    table.getItems().add(oldIndex, selected);
                                    showAlert("Ошибка", "Не удалось удалить запись. Данные не изменены.");
                                }
                        );
                    }
                });
            }
        });

        // ==== КНОПКА ОБНОВИТЬ ====
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(e -> {
            // Имитация обновления (в будущем — запрос на сервер)
            // Например: перезапросить REST, обновить table.getItems()
            List<ScheduleDTO> updatedSchedules = TestData.getTestSchedule(); // заменить на свой REST-метод
            table.getItems().setAll(updatedSchedules);
            // showSnackbar("Расписание обновлено!"); // если используешь снэкбар
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

    private static void showEditDialog(ScheduleDTO schedule, Consumer<ScheduleDTO> onSave) {
        boolean isNew = (schedule == null);

        Dialog<ScheduleDTO> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Новое занятие" : "Редактирование занятия");

        TextField groupField = new TextField(isNew ? "" : schedule.group().name());
        ComboBox<DayOfWeek> dayBox = new ComboBox<>();
        dayBox.getItems().addAll(DayOfWeek.values());
        dayBox.setValue(isNew ? DayOfWeek.MONDAY : schedule.dayOfWeek());
        TextField startField = new TextField(isNew ? "09:00" : schedule.startTime().toString());
        TextField endField = new TextField(isNew ? "10:30" : schedule.endTime().toString());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Группа:"), 0, 0); grid.add(groupField, 1, 0);
        grid.add(new Label("День недели:"), 0, 1); grid.add(dayBox, 1, 1);
        grid.add(new Label("Начало:"), 0, 2); grid.add(startField, 1, 2);
        grid.add(new Label("Окончание:"), 0, 3); grid.add(endField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    LocalTime start = LocalTime.parse(startField.getText());
                    LocalTime end = LocalTime.parse(endField.getText());
                    // В реальности группа должна выбираться из списка (или создаётся новая)
                    GroupDTO group = isNew
                            ? new GroupDTO("new", groupField.getText(), new GradeDTO("gr1", new LanguageDTO("Английский"), CEFRLevel.A1), new LanguageDTO("Английский"))
                            : new GroupDTO(schedule.group().id(), groupField.getText(),
                            schedule.group().grade(), schedule.group().language());

                    ScheduleDTO updated = new ScheduleDTO(
                            isNew ? "id" + System.currentTimeMillis() : schedule.id(),
                            group,
                            dayBox.getValue(), start, end
                    );
                    return updated;
                } catch (Exception e) {
                    // showSnackbar("Ошибка формата времени!");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null) {
                onSave.accept(result);
            }
        });
    }

    private static void restUpdateSchedule(ScheduleDTO dto, Runnable onSuccess, Runnable onFail) {
        // Имитация REST-запроса на обновление (PATCH/PUT)
        new Thread(() -> {
            try {
                Thread.sleep(600); // Имитация задержки
                // if (Math.random() < 0.5) throw new Exception(); // Для теста ошибок
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(onFail);
            }
        }).start();
    }

    private static void restAddSchedule(ScheduleDTO dto, Runnable onSuccess, Runnable onFail) {
        // Имитация REST-запроса на добавление (POST)
        new Thread(() -> {
            try {
                Thread.sleep(600);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(onFail);
            }
        }).start();
    }

    private static void restDeleteSchedule(ScheduleDTO dto, Runnable onSuccess, Runnable onFail) {
        // Имитация REST-запроса на удаление (DELETE)
        new Thread(() -> {
            try {
                Thread.sleep(600);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(onFail);
            }
        }).start();
    }
}
