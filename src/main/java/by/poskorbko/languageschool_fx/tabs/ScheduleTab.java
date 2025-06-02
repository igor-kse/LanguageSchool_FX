package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.TestData;
import by.poskorbko.languageschool_fx.dto.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

public class ScheduleTab extends BaseTab<ScheduleDTO> {
    public ScheduleTab() {
        super("/schedule");
    }

    @Override
    protected Button getRefreshButton() {
        return new Button();
    }

    @Override
    protected String getSelectedUuid(int index) {
        return "";
    }

    public VBox createScheduleTable(List<ScheduleDTO> schedules) {
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

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    @Override
    protected void showEditDialog(ScheduleDTO schedule, Consumer<ScheduleDTO> onSave) {
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
}
