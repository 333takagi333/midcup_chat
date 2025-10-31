package com.chat.control;

import com.chat.model.LoginRequest;
import com.chat.model.Request;
import com.chat.network.SocketClient;
import com.chat.ui.CustomButton;
import com.google.gson.Gson;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginControl {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CustomButton loginButton;

    @FXML
    private CustomButton registerButton;

    @FXML
    void login(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username or password cannot be empty.");
            // 可以在这里弹出一个InfoDialog提示用户
            return;
        }

        String encryptedPassword = PasswordEncryptor.encrypt(password);
        System.out.println("Login attempt with username: " + username + " and encrypted password: " + encryptedPassword);

        // 封装登录请求数据
        LoginRequest loginRequestData = new LoginRequest(username, encryptedPassword);
        // 封装通用请求
        Request request = new Request("LOGIN", loginRequestData);

        // 转换为JSON
        Gson gson = new Gson();
        String jsonRequest = gson.toJson(request);

        // 禁用按钮，防止重复点击
        loginButton.setDisable(true);

        // 在后台线程中发送请求并等待响应，避免阻塞UI线程
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                SocketClient client = new SocketClient();
                return client.sendLoginRequest(jsonRequest);
            }
        };

        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            String response = task.getValue();
            if (response != null) {
                System.out.println("Server response: " + response);
            } else {
                System.out.println("No response or request failed.");
            }
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            Throwable ex = task.getException();
            System.err.println("Request failed: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        new Thread(task, "login-request-thread").start();
    }

    @FXML
    void register(ActionEvent event) {
        System.out.println("Register button clicked");
        // 在这里添加注册逻辑
    }
}
