package com.chat.control;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private Label usernameLabel;

    public void setUsername(String username) {
        usernameLabel.setText("Welcome, " + username);
    }
}
