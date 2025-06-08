package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.*;
import by.poskorbko.languageschool_fx.http.CrudRestClient;
import by.poskorbko.languageschool_fx.util.JsonObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private static final String BASE_PATH = "/schedule";
    private final ObservableList<GroupDTO> groups = FXCollections.observableArrayList();

    public ScheduleTab() {
        super(BASE_PATH);
    }

    public VBox createScheduleTable() {
        TableView<ScheduleDTO> table = getTable();

        TableColumn<ScheduleDTO, String> groupCol = new TableColumn<>("Группа");
        groupCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().groupName()));

        TableColumn<ScheduleDTO, String> languageCol = new TableColumn<>("Язык");
        languageCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().languageName()));

        TableColumn<ScheduleDTO, String> levelCol = new TableColumn<>("Уровень");
        levelCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().levelName()));

        TableColumn<ScheduleDTO, String> teacherCol = new TableColumn<>("Преподаватель");
        teacherCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().teacherName()));

        TableColumn<ScheduleDTO, String> dayCol = new TableColumn<>("День");
        dayCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dayOfWeek().toString()));

        TableColumn<ScheduleDTO, String> timeCol = new TableColumn<>("Время");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().startTime() + " — " + data.getValue().endTime()
        ));

        table.getColumns().addAll(groupCol, languageCol, levelCol, teacherCol, dayCol, timeCol);

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        getRefreshButton().fire();
        return vbox;
    }

    @Override
    protected Button getRefreshButton() {
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(event -> {
            // Подгружаем группы
            CrudRestClient.getCall("/groups", resp -> {
                try {
                    List<GroupDTO> groupList = JsonObjectMapper.getInstance().readValue(
                            resp.body(), new TypeReference<List<GroupDTO>>() {});
                    Platform.runLater(() -> groups.setAll(groupList));
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Ошибка", "Ошибка разбора списка групп");
                }
            }, resp -> showAlert("Ошибка", "Ошибка загрузки групп: " + (resp == null ? "" : resp.statusCode())));

            // Подгружаем расписание
            CrudRestClient.getCall(BASE_PATH, response -> Platform.runLater(() -> {
                try {
                    List<ScheduleDTO> entities = JsonObjectMapper.getInstance().readValue(
                            response.body(), new TypeReference<List<ScheduleDTO>>() {});
                    getTable().getItems().setAll(entities);
                } catch (Exception e) {
                    showAlert("Ошибка", "Ошибка разбора расписания");
                }
            }), failedResponse -> Platform.runLater(() -> {
                showAlert("Ошибка", "Ошибка загрузки расписания: " + failedResponse.statusCode());
            }));
        });
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        return getTable().getItems().get(index).id();
    }

    @Override
    protected void showEditDialog(ScheduleDTO schedule, Consumer<ScheduleDTO> onSave) {
        boolean isNew = (schedule == null);

        Dialog<ScheduleToPost> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Добавить занятие" : "Редактировать занятие");

        ComboBox<GroupDTO> groupBox = new ComboBox<>(groups);
        groupBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(GroupDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : groupDisplay(item));
            }
        });
        groupBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GroupDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : groupDisplay(item));
            }
        });

        ComboBox<DayOfWeek> dayBox = new ComboBox<>();
        dayBox.getItems().addAll(DayOfWeek.values());

        Spinner<Integer> startHour = new Spinner<>(0, 23, 9);
        Spinner<Integer> startMinute = new Spinner<>(0, 59, 0);
        Spinner<Integer> endHour = new Spinner<>(0, 23, 10);
        Spinner<Integer> endMinute = new Spinner<>(0, 59, 30);

        // Если редактирование — выставляем значения
        if (!isNew) {
            groups.stream().filter(g -> g.name().equals(schedule.groupName()))
                    .findFirst().ifPresent(groupBox.getSelectionModel()::select);
            dayBox.setValue(schedule.dayOfWeek());
            startHour.getValueFactory().setValue(schedule.startTime().getHour());
            startMinute.getValueFactory().setValue(schedule.startTime().getMinute());
            endHour.getValueFactory().setValue(schedule.endTime().getHour());
            endMinute.getValueFactory().setValue(schedule.endTime().getMinute());
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Группа:"), 0, 0); grid.add(groupBox, 1, 0, 3, 1);
        grid.add(new Label("День недели:"), 0, 1); grid.add(dayBox, 1, 1, 3, 1);
        grid.add(new Label("Начало:"), 0, 2); grid.add(startHour, 1, 2); grid.add(new Label(":"), 2, 2); grid.add(startMinute, 3, 2);
        grid.add(new Label("Окончание:"), 0, 3); grid.add(endHour, 1, 3); grid.add(new Label(":"), 2, 3); grid.add(endMinute, 3, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(420);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                GroupDTO group = groupBox.getValue();
                DayOfWeek day = dayBox.getValue();
                int sh = startHour.getValue(), sm = startMinute.getValue();
                int eh = endHour.getValue(), em = endMinute.getValue();
                if (group == null || day == null) {
                    showAlert("Ошибка", "Заполните все поля");
                    return null;
                }
                LocalTime st = LocalTime.of(sh, sm);
                LocalTime et = LocalTime.of(eh, em);

                if (!st.isBefore(et)) {
                    showAlert("Ошибка", "Время начала должно быть раньше времени окончания");
                    return null;
                }

                return new ScheduleToPost(
                        isNew ? null : schedule.id(),
                        group.id(),
                        day.name(),
                        st.toString(),
                        et.toString()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(toPost -> {
            if (toPost != null) {
                Consumer<ScheduleDTO> postSaveRefresh = e -> getRefreshButton().fire();
                if (isNew) {
                    CrudRestClient.addPostCall(BASE_PATH, toPost, resp -> Platform.runLater(() -> getRefreshButton().fire()), fail -> showAlert("Ошибка", "Ошибка создания занятия"));
                } else {
                    CrudRestClient.putCall(BASE_PATH, toPost, resp -> Platform.runLater(() -> getRefreshButton().fire()), fail -> showAlert("Ошибка", "Ошибка редактирования занятия"));
                }
            }
        });
    }


    private String groupDisplay(GroupDTO group) {
        if (group == null) return "";
        String level = group.levelDTO() != null ? group.levelDTO().name() : "";
        String scale = (group.language() != null && group.language().scale() != null) ? group.language().scale().name() : "";
        String teacher = group.teacher() != null ? (group.teacher().firstName() + " " + group.teacher().lastName()) : "";
        return String.format("%s %s (%s) - %s", group.name(), level, scale, teacher);
    }
}
