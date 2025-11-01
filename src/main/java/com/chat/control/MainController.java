package com.chat.control;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * 主界面的控制器：负责在页面顶部显示当前用户名。
 */
public class MainController {

    @FXML
    private Label usernameLabel;

    /**
     * 设置并显示当前登录用户名。
     * @param username 用户名
     */
    public void setUsername(String username) {
        usernameLabel.setText("Welcome, " + username);
    }
}
