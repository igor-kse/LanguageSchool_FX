module by.poskorbko.languageschool_fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.net.http;
    requires annotations;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires javafx.swing;
    requires jdk.httpserver;
    requires com.jfoenix;

    opens by.poskorbko.languageschool_fx to javafx.fxml;
    exports by.poskorbko.languageschool_fx;
    exports by.poskorbko.languageschool_fx.tabs;
    exports by.poskorbko.languageschool_fx.dto;
    opens by.poskorbko.languageschool_fx.tabs to javafx.fxml;
    opens by.poskorbko.languageschool_fx.dto to com.fasterxml.jackson.databind;
    exports by.poskorbko.languageschool_fx.util;
}