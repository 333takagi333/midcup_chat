package com.chat.control;

import com.chat.model.LoginRequest;
import com.chat.model.Request;
import com.chat.network.SocketClient;
import com.chat.ui.CustomButton;
import com.chat.ui.InfoDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.IOException;

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
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                if ("SUCCESS".equals(status)) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/main.fxml"));
                        Parent mainRoot = loader.load();

                        com.chat.control.MainController mainController = loader.getController();
                        mainController.setUsername(username);

                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        stage.setTitle("Chat");
                        stage.setScene(new Scene(mainRoot));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/components/InfoDialog.fxml"));
                        Parent dialogRoot = loader.load();
                        InfoDialog controller = loader.getController();
                        controller.setMessage("登录失败，用户或密码错误，请重试");
                        Stage owner = (Stage) loginButton.getScene().getWindow();
                        Stage dialogStage = new Stage();
                        controller.setDialogStage(dialogStage);
                        dialogStage.initOwner(owner);
                        dialogStage.initModality(Modality.APPLICATION_MODAL);
                        dialogStage.setTitle("提示");
                        dialogStage.setScene(new Scene(dialogRoot));
                        dialogStage.showAndWait();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } else {
                // response == null，视为连接失败
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/components/InfoDialog.fxml"));
                    Parent dialogRoot = loader.load();
                    InfoDialog controller = loader.getController();
                    controller.setMessage("服务器连接失败，请稍后重试");
                    Stage owner = (Stage) loginButton.getScene().getWindow();
                    Stage dialogStage = new Stage();
                    controller.setDialogStage(dialogStage);
                    dialogStage.initOwner(owner);
                    dialogStage.initModality(Modality.APPLICATION_MODAL);
                    dialogStage.setTitle("提示");
                    dialogStage.setScene(new Scene(dialogRoot));
                    dialogStage.showAndWait();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/components/InfoDialog.fxml"));
                Parent dialogRoot = loader.load();
                InfoDialog controller = loader.getController();
                controller.setMessage("服务器连接失败，请稍后重试");
                Stage owner = (Stage) loginButton.getScene().getWindow();
                Stage dialogStage = new Stage();
                controller.setDialogStage(dialogStage);
                dialogStage.initOwner(owner);
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.setTitle("提示");
                dialogStage.setScene(new Scene(dialogRoot));
                dialogStage.showAndWait();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        new Thread(task, "login-request-thread").start();
    }

    @FXML
    void register(ActionEvent event) {
        System.out.println("Register button clicked");
        // 在这里添加注册逻辑
    }
}
