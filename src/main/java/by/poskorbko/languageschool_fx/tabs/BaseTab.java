package by.poskorbko.languageschool_fx.tabs;

import by.poskorbko.languageschool_fx.dto.Role;
import by.poskorbko.languageschool_fx.dto.ScheduleDTO;
import by.poskorbko.languageschool_fx.http.CrudRestClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import by.poskorbko.languageschool_fx.util.ActivityMonitor;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.function.Consumer;

public abstract class BaseTab<T> {

    private final TableView<T> table = new TableView<>();
    private String basePath;

    public BaseTab(String basePath) {
        this.basePath = basePath;
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    protected TableView<T> getTable() {
        return table;
    }

    protected <E> Dialog<E> createDialog() {
        Dialog<E> dialog = new Dialog<>();
        ActivityMonitor.attach(dialog.getDialogPane());
        return dialog;
    }

    protected Button getAddButton() {
        Button addButton = new Button("Добавить");
        addButton.setStyle("-fx-background-color: #39d353; -fx-text-fill: #333; -fx-background-radius: 8;");

        Consumer<T> onSave = entityToSave -> CrudRestClient.addPostCall(
                basePath, entityToSave,
                successResponse -> Platform.runLater(() -> getRefreshButton().fire()),
                failResponse -> System.err.println("Failed to create: " + failResponse.statusCode() + " " + failResponse.body())
        );
        EventHandler<ActionEvent> handler = event -> showEditDialog(null, onSave);
        addButton.setOnAction(handler);
        return addButton;
    }

    protected Button getEditButton() {
        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #ffd43b; -fx-text-fill: #333; -fx-background-radius: 8;");
        editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        EventHandler<ActionEvent> handler = event -> {
            T selected = getTable().getSelectionModel().getSelectedItem();
            System.out.println("Выбран для редактирования: " + selected);
            if (selected == null) {
                System.out.println("Нет выбранного преподавателя");
                return;
            }
            showEditDialog(selected,
                    entityToUpdate -> CrudRestClient.putCall(
                            basePath, entityToUpdate,
                            successResponse -> Platform.runLater(() -> getRefreshButton().fire()),
                            failResponse -> System.err.println("Failed to edit: " + failResponse.statusCode() + " " + failResponse.body())
                    )
            );
            getTable().getSelectionModel().clearSelection();
        };
        editBtn.setOnAction(handler);

        return editBtn;
    }

    protected Button getDeleteButton(String message) {
        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: #333; -fx-background-radius: 8;");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> {
            T selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Подтверждение удаления");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        int oldIndex = table.getItems().indexOf(selected);
                        String id = getSelectedUuid(oldIndex);
                        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
                        table.getItems().remove(selected);
                        CrudRestClient.deleteCall(basePath + "/" + encodedId,
                                successResponse -> table.getItems().remove(oldIndex),
                                failResponse -> System.err.println("Failed to delete: " + failResponse.statusCode() + " " + failResponse.body())
                        );
                    }
                });
            }
        });
        return deleteBtn;
    }

    protected abstract Button getRefreshButton();

    protected abstract String getSelectedUuid(int index);

    protected Region getSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    protected HBox getButtons(String message) {
        HBox buttons = new HBox(10, getAddButton(), getEditButton(), getDeleteButton(message), getSpacer(), getRefreshButton());
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(0, 0, 10, 0));
        return buttons;
    }

    protected HBox getRefreshAsButtons() {
        HBox buttons = new HBox(10, getRefreshButton());
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(0, 0, 10, 0));
        return buttons;
    }

    protected abstract void showEditDialog(T item, Consumer<T> onSave);

    protected static void showAlert(String title, String msg) {
        Runnable show = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.setHeaderText(title);
            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    // === Заглушка для вкладок ===
    public static VBox createPlaceholderContent(String title) {
        Label label = new Label(title + " — здесь будет содержимое");
        label.setFont(Font.font("Arial", 18));
        label.setTextFill(Color.web("#888"));
        VBox box = new VBox(label);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(32));
        return box;
    }


}
