package com.chat.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class InfoDialog {

    @FXML
    private Label messageLabel;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    @FXML
    void closeDialog(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}

