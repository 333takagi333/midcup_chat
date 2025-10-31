package com.chat.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmDialog {

    @FXML
    private Label messageLabel;

    private Stage dialogStage;
    private boolean okClicked = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    void handleOk(ActionEvent event) {
        okClicked = true;
        dialogStage.close();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        dialogStage.close();
    }
}

