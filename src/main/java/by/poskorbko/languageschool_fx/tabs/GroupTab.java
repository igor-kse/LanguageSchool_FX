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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class GroupTab extends BaseTab<GroupDTO> {

    private static final String BASE_PATH = "/groups";

    // Для выпадающих списков
    private final ObservableList<TeacherDTO> teachers = FXCollections.observableArrayList();
    private final ObservableList<LanguageDTO> languages = FXCollections.observableArrayList();
    private final ObservableList<LanguageScaleLevelDTO> levels = FXCollections.observableArrayList();

    public GroupTab() {
        super(BASE_PATH);
    }

    public VBox createGroupsTable() {
        TableView<GroupDTO> table = getTable();

        TableColumn<GroupDTO, String> nameCol = new TableColumn<>("Группа");
        TableColumn<GroupDTO, String> teacherCol = new TableColumn<>("Учитель");
        TableColumn<GroupDTO, String> languageCol = new TableColumn<>("Язык");
        TableColumn<GroupDTO, String> scaleCol = new TableColumn<>("Шкала");
        TableColumn<GroupDTO, String> levelCol = new TableColumn<>("Уровень");

        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        teacherCol.setCellValueFactory(data -> new SimpleStringProperty(teacherDisplay(data.getValue().teacher())));
        languageCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().language() != null ? data.getValue().language().name() : "")
        );
        scaleCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().language() != null && data.getValue().language().scale() != null
                        ? data.getValue().language().scale().name() : "")
        );
        levelCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().levelDTO() != null ? data.getValue().levelDTO().name() : "")
        );

        table.getColumns().addAll(nameCol, teacherCol, languageCol, scaleCol, levelCol);

        VBox vbox = new VBox(8, getButtons(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        // Начальная загрузка данных
        getRefreshButton().fire();

        return vbox;
    }

    protected HBox getButtons() {
        HBox buttons = new HBox(10, getAddButton(), getEditButton(), getDeleteButton(), getStudentsButton(), getSpacer(), getRefreshButton());
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(0, 0, 10, 0));
        return buttons;
    }

    @Override
    public Button getRefreshButton() {
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(event -> {
            // Сначала подгружаем справочники (teachers, languages)
            loadReferenceData(() -> {
                CrudRestClient.getCall(BASE_PATH, response -> Platform.runLater(() -> {
                    try {
                        List<GroupDTO> entities = JsonObjectMapper.getInstance().readValue(
                                response.body(), new TypeReference<>() {});
                        getTable().getItems().setAll(entities);
                    } catch (Exception e) {
                        showAlert("Ошибка", "Ошибка разбора списка групп");
                    }
                }), failedResponse -> Platform.runLater(() -> {
                    showAlert("Ошибка", "Ошибка загрузки групп: " + failedResponse.statusCode());
                }));
            });
        });
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        return getTable().getItems().get(index).id();
    }

    @Override
    protected void showEditDialog(GroupDTO group, Consumer<GroupDTO> onSave) {
        boolean isNew = (group == null);

        Dialog<GroupDTO> dialog = createDialog();
        dialog.setTitle(isNew ? "Добавить группу" : "Редактировать группу");

        // Поля формы
        TextField nameField = new TextField(isNew ? "" : group.name());

        ComboBox<TeacherDTO> teacherBox = new ComboBox<>(teachers);
        teacherBox.setPromptText("Учитель");
        teacherBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TeacherDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : teacherDisplay(item));
            }
        });
        teacherBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TeacherDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : teacherDisplay(item));
            }
        });

        ComboBox<LanguageDTO> languageBox = new ComboBox<>(languages);
        languageBox.setPromptText("Язык");
        languageBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LanguageDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        languageBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LanguageDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });

        ComboBox<LanguageScaleLevelDTO> levelBox = new ComboBox<>(levels);
        levelBox.setPromptText("Уровень");

        // --- ОТРАЖЕНИЕ ТОЛЬКО ИМЕНИ УРОВНЯ ---
        levelBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LanguageScaleLevelDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        levelBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LanguageScaleLevelDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });

        // --- Логика: при выборе языка — подтянуть уровни этой шкалы ---
        languageBox.valueProperty().addListener((obs, oldLang, newLang) -> {
            levels.clear();
            if (newLang != null && newLang.scale() != null && newLang.scale().levels() != null) {
                levels.addAll(newLang.scale().levels());
                if (!isNew && group.levelDTO() != null) {
                    // установить значение level
                    for (LanguageScaleLevelDTO l : levels) {
                        if (l.id().equals(group.levelDTO().id())) {
                            levelBox.getSelectionModel().select(l);
                            break;
                        }
                    }
                }
            } else {
                levelBox.getSelectionModel().clearSelection();
            }
        });

        // При редактировании выставляем значения по умолчанию
        if (!isNew) {
            if (group.teacher() != null)
                teachers.stream().filter(t -> t.id().equals(group.teacher().id())).findFirst().ifPresent(teacherBox.getSelectionModel()::select);
            if (group.language() != null)
                languages.stream().filter(l -> l.name().equals(group.language().name())).findFirst().ifPresent(languageBox.getSelectionModel()::select);
            // уровень подтянется listener'ом языка
        }

        VBox vbox = new VBox(12,
                new Label("Название группы:"), nameField,
                new Label("Учитель:"), teacherBox,
                new Label("Язык:"), languageBox,
                new Label("Уровень:"), levelBox
        );
        vbox.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().setPrefWidth(360);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name = nameField.getText().trim();
                TeacherDTO teacher = teacherBox.getValue();
                LanguageDTO language = languageBox.getValue();
                LanguageScaleLevelDTO level = levelBox.getValue();
                if (name.isEmpty() || teacher == null || language == null || level == null) {
                    showAlert("Ошибка", "Заполните все поля");
                    return null;
                }
                return new GroupDTO(
                        isNew ? null : group.id(),
                        name,
                        teacher,
                        language,
                        level
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }


    private String teacherDisplay(TeacherDTO teacher) {
        if (teacher == null) return "";
        String name = (teacher.firstName() != null ? teacher.firstName() : "") +
                " " + (teacher.lastName() != null ? teacher.lastName() : "");
        String email = (teacher.email() != null && !teacher.email().isEmpty()) ? " (" + teacher.email() + ")" : "";
        return name.trim() + email;
    }

    // --- Подгрузка справочников (teachers, languages)
    private void loadReferenceData(Runnable onReady) {
        CrudRestClient.getCall("/teachers", resp -> {
            try {
                List<TeacherDTO> tList = JsonObjectMapper.getInstance().readValue(
                        resp.body(), new TypeReference<>() {});
                Platform.runLater(() -> {
                    teachers.setAll(tList);
                    CrudRestClient.getCall("/languages-scales", resp2 -> {
                        try {
                            List<LanguageDTO> lList = JsonObjectMapper.getInstance().readValue(
                                    resp2.body(), new TypeReference<>() {});
                            Platform.runLater(() -> {
                                languages.setAll(lList);
                                if (onReady != null) onReady.run();
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showAlert("Ошибка", "Ошибка разбора языков");
                        }
                    }, resp2 -> showAlert("Ошибка", "Ошибка загрузки языков: " + (resp2 == null ? "" : resp2.statusCode())));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Ошибка", "Ошибка разбора учителей");
            }
        }, resp -> showAlert("Ошибка", "Ошибка загрузки учителей: " + (resp == null ? "" : resp.statusCode())));
    }

    private Button getStudentsButton() {
        Button btn = new Button("Студенты");
        btn.setStyle("-fx-background-color: #ffb347; -fx-text-fill: #333; -fx-background-radius: 8;");
        btn.disableProperty().bind(getTable().getSelectionModel().selectedItemProperty().isNull());
        btn.setOnAction(event -> {
            GroupDTO group = getTable().getSelectionModel().getSelectedItem();
            if (group != null) showStudentsDualListDialog(group);
        });
        return btn;
    }

    private void showStudentsDualListDialog(GroupDTO group) {
        Dialog<Void> dialog = createDialog();
        dialog.setTitle("Управление студентами группы " + group.name());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // --- Списки студентов (локальные копии)
        ObservableList<StudentDTO> allStudents = FXCollections.observableArrayList();
        ObservableList<StudentDTO> groupStudents = FXCollections.observableArrayList();

        ListView<StudentDTO> allList = new ListView<>(allStudents);
        ListView<StudentDTO> inGroupList = new ListView<>(groupStudents);

        allList.setPrefWidth(220);
        inGroupList.setPrefWidth(220);
        allList.setPrefHeight(260);
        inGroupList.setPrefHeight(260);

        allList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(StudentDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.firstName() + " " + item.lastName() + " (" + item.email() + ")");
            }
        });
        inGroupList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(StudentDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.firstName() + " " + item.lastName() + " (" + item.email() + ")");
            }
        });

        // --- Кнопки добавить/убрать
        Button addBtn = new Button("→");
        Button removeBtn = new Button("←");
        addBtn.setPrefWidth(40);
        removeBtn.setPrefWidth(40);

        addBtn.setOnAction(e -> {
            StudentDTO selected = allList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                groupStudents.add(selected);
                allStudents.remove(selected);
            }
        });

        removeBtn.setOnAction(e -> {
            StudentDTO selected = inGroupList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                allStudents.add(selected);
                groupStudents.remove(selected);
            }
        });

        addBtn.disableProperty().bind(allList.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.disableProperty().bind(inGroupList.getSelectionModel().selectedItemProperty().isNull());

        VBox btns = new VBox(8, addBtn, removeBtn);
        btns.setAlignment(Pos.CENTER);

        HBox lists = new HBox(16, allList, btns, inGroupList);
        lists.setAlignment(Pos.CENTER);
        lists.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(12, new Label("Доступные и текущие студенты группы:"), lists);
        root.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(root);

        // --- Подгружаем студентов (сперва группу, потом всех)
        CrudRestClient.getCall("/students?group=" + group.id(), resp -> Platform.runLater(() -> {
            try {
                List<StudentDTO> inGroup = JsonObjectMapper.getInstance().readValue(
                        resp.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                groupStudents.setAll(inGroup);

                CrudRestClient.getCall("/students", resp2 -> Platform.runLater(() -> {
                    try {
                        List<StudentDTO> all = JsonObjectMapper.getInstance().readValue(
                                resp2.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                        // Исключаем уже добавленных
                        all.removeIf(st -> groupStudents.stream().anyMatch(gs -> gs.id().equals(st.id())));
                        allStudents.setAll(all);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Ошибка", "Ошибка разбора студентов");
                    }
                }), fail -> showAlert("Ошибка", "Ошибка загрузки всех студентов"));
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Ошибка", "Ошибка разбора студентов группы");
            }
        }), fail -> showAlert("Ошибка", "Ошибка загрузки студентов группы"));

        // --- Действие на OK ---
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                // Сохраняем финальный список студентов группы
                List<String> studentIds = groupStudents.stream().map(StudentDTO::id).toList();
                CrudRestClient.putCall("/groups/" + group.id() + "/students", studentIds,
                        resp -> Platform.runLater(() -> {}),
                        fail -> Platform.runLater(() -> showAlert("Ошибка", "Не удалось обновить состав группы")));
            }
            return null;
        });

        dialog.showAndWait();
    }

}
