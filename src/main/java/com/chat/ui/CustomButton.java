package com.chat.ui;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class CustomButton extends AnchorPane {
    @FXML
    private Button button;

    public CustomButton() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/chat/fxml/components/CustomButton.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public String getText() {
        return textProperty().get();
    }

    public void setText(String value) {
        textProperty().set(value);
    }

    public StringProperty textProperty() {
        return button.textProperty();
    }

    public void setOnAction(EventHandler<ActionEvent> value) {
        button.setOnAction(value);
    }

    public EventHandler<ActionEvent> getOnAction() {
        return button.getOnAction();
    }
}
