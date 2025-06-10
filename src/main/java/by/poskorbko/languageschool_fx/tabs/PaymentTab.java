package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.PaymentDTO;
import by.poskorbko.languageschool_fx.dto.Role;
import by.poskorbko.languageschool_fx.dto.UserDTO;
import by.poskorbko.languageschool_fx.http.CrudRestClient;
import by.poskorbko.languageschool_fx.util.JsonObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class PaymentTab extends BaseTab<PaymentDTO> {

    private static final String BASE_PATH = "/payments";
    private final ObservableList<UserDTO> users = FXCollections.observableArrayList();

    public PaymentTab() {
        super(BASE_PATH);
    }

    public VBox createPaymentsTable(Set<Role> roles) {
        TableView<PaymentDTO> table = getTable();

        TableColumn<PaymentDTO, String> userCol = new TableColumn<>("Плательщик");
        userCol.setCellValueFactory(data -> new SimpleStringProperty(
                getUserNameById(data.getValue().user()))
        );

        TableColumn<PaymentDTO, String> amountCol = new TableColumn<>("Сумма");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                formatAmount(data.getValue().amount()))
        );

        TableColumn<PaymentDTO, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().date().toString())
        );

        TableColumn<PaymentDTO, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().description())
        );

        table.getColumns().addAll(userCol, amountCol, dateCol, descCol);

        String deleteMessage = "Удалить платёж?";
        Node[] buttons;
        if (roles.contains(Role.ADMIN)) {
            buttons = new Node[]{getButtons(deleteMessage), table};
        } else {
            buttons = new Node[]{getRefreshAsButtons(), table};
        }
        VBox vbox = new VBox(8, buttons);
        VBox.setVgrow(table, Priority.ALWAYS);
        vbox.setPadding(new Insets(10));

        getRefreshButton().fire();
        return vbox;
    }

    @Override
    public Button getRefreshButton() {
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #36a3f7; -fx-text-fill: white; -fx-background-radius: 8;");
        refreshBtn.setOnAction(event -> {
            CrudRestClient.getCall("/users", userResp -> {
                try {
                    List<UserDTO> userList = JsonObjectMapper.getInstance().readValue(
                            userResp.body(), new TypeReference<List<UserDTO>>() {});
                    Platform.runLater(() -> {
                        users.setAll(userList);

                        CrudRestClient.getCall(BASE_PATH, payResp -> Platform.runLater(() -> {
                            try {
                                List<PaymentDTO> payments = JsonObjectMapper.getInstance().readValue(
                                        payResp.body(), new TypeReference<List<PaymentDTO>>() {});
                                getTable().getItems().setAll(payments);
                            } catch (Exception e) {
                                showAlert("Ошибка", "Ошибка загрузки платежей");
                            }
                        }), err -> Platform.runLater(() -> showAlert("Ошибка", "Ошибка загрузки платежей")));
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Ошибка", "Ошибка загрузки пользователей");
                }
            }, err -> Platform.runLater(() -> showAlert("Ошибка", "Ошибка загрузки пользователей")));
        });
        return refreshBtn;
    }

    @Override
    protected String getSelectedUuid(int index) {
        return getTable().getItems().get(index).id();
    }

    @Override
    protected void showEditDialog(PaymentDTO payment, Consumer<PaymentDTO> onSave) {
        boolean isNew = (payment == null);

        Dialog<PaymentDTO> dialog = createDialog();
        dialog.setTitle(isNew ? "Добавить платёж" : "Редактировать платёж");

        ComboBox<UserDTO> userBox = new ComboBox<>(users);
        userBox.setPromptText("Выберите пользователя");
        userBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : userDisplay(item));
            }
        });
        userBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(UserDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : userDisplay(item));
            }
        });

        // ============ Сумма ==============
        // Рубли
        TextField rubField = new TextField();
        rubField.setPromptText("Рубли");
        rubField.setPrefWidth(60);
        // Только числа!
        rubField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                rubField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // Копейки (расширенный Spinner)
        Spinner<Integer> kopSpinner = new Spinner<>(0, 99, 0);
        kopSpinner.setEditable(true);
        kopSpinner.getEditor().setPrefWidth(60);

        // Если редактируем — заполнить поля
        if (!isNew && payment.amount() >= 0) {
            rubField.setText(Long.toString(payment.amount() / 100));
            kopSpinner.getValueFactory().setValue((int)Math.abs(payment.amount() % 100));
        }

        DatePicker datePicker = new DatePicker(isNew ? LocalDate.now() : payment.date());

        TextField descField = new TextField(isNew ? "" : payment.description());
        descField.setPromptText("Описание");

        if (!isNew && payment.user() != null) {
            users.stream().filter(u -> u.id().equals(payment.user())).findFirst()
                    .ifPresent(userBox.getSelectionModel()::select);
        }

        // ========== UI =============
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(24));
        grid.add(new Label("Плательщик:"), 0, 0); grid.add(userBox, 1, 0);
        grid.add(new Label("Сумма:"), 0, 1);
        HBox amountBox = new HBox(5, rubField, new Label("руб."), kopSpinner, new Label("коп."));
        grid.add(amountBox, 1, 1);
        grid.add(new Label("Дата:"), 0, 2); grid.add(datePicker, 1, 2);
        grid.add(new Label("Описание:"), 0, 3); grid.add(descField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(400);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String requiredFields = "";
            if (userBox.getSelectionModel().getSelectedIndex() == -1) {
                requiredFields += "        Плательщик\n";
            }
            if (rubField.getText().isEmpty()) {
                requiredFields += "        Сумма\n";
            }
            if (descField.getText().isEmpty()) {
                requiredFields += "        Описание\n";
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
                UserDTO user = userBox.getValue();
                String rubStr = rubField.getText().trim();
                Integer kop = kopSpinner.getValue();
                LocalDate date = datePicker.getValue();
                String desc = descField.getText().trim();

                if (user == null || rubStr.isEmpty() || kop == null || date == null || desc.isEmpty()) {
                    showAlert("Ошибка", "Заполните все поля");
                    return null;
                }
                int rub;
                try {
                    rub = Integer.parseInt(rubStr);
                    if (rub < 0 || kop < 0 || kop > 99)
                        throw new IllegalArgumentException();
                } catch (Exception e) {
                    showAlert("Ошибка", "Некорректное значение суммы");
                    return null;
                }
                long amountLong = rub * 100L + kop;
                if (amountLong <= 0) {
                    showAlert("Ошибка", "Сумма должна быть больше нуля");
                    return null;
                }
                return new PaymentDTO(
                        isNew ? null : payment.id(),
                        user.id(),
                        amountLong,
                        date,
                        desc
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(onSave);
    }

    // ======= Вспомогательные методы =======

    private String getUserNameById(String userId) {
        if (userId == null) return "";
        return users.stream()
                .filter(u -> u.id().equals(userId))
                .findFirst()
                .map(this::userDisplay)
                .orElse(userId);
    }

    private String userDisplay(UserDTO user) {
        return user.lastName() + " " + user.firstName();
    }

    private String formatAmount(long amount) {
        long rub = amount / 100;
        long kop = Math.abs(amount % 100);
        return String.format("%d руб. %02d коп.", rub, kop);
    }
}
