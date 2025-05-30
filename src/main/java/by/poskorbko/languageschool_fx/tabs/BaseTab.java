package by.poskorbko.languageschool_fx.tabs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;

public class BaseTab<T> {

    private final TableView<T> table = new TableView<>();



    protected static void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }


}
