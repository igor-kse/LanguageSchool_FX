package by.poskorbko.languageschool_fx.util;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class ActivityMonitor {
    private static Runnable onActivity;

    public static void setOnActivity(Runnable callback) {
        onActivity = callback;
    }

    public static void notifyActivity() {
        if (onActivity != null) {
            onActivity.run();
        }
    }

    public static void attach(Scene scene) {
        if (scene != null) {
            scene.addEventFilter(MouseEvent.ANY, e -> notifyActivity());
            scene.addEventFilter(KeyEvent.ANY, e -> notifyActivity());
        }
    }

    public static void attach(Node node) {
        if (node != null) {
            node.addEventFilter(MouseEvent.ANY, e -> notifyActivity());
            node.addEventFilter(KeyEvent.ANY, e -> notifyActivity());
        }
    }
}
