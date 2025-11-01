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

/**
 * 登录界面的控制器，负责处理用户登录和跳转到主界面，以及错误提示。
 */
public class LoginControl {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CustomButton loginButton;

    @FXML
    private CustomButton registerButton;

    /**
     * 处理登录按钮点击事件：
     * - 校验输入
     * - 发送登录请求
     * - 根据服务端返回的结果进行界面跳转或错误提示
     */
    @FXML
    void login(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showInfo("请输入用户名和密码");
            return;
        }

        String encryptedPassword = PasswordEncryptor.encrypt(password);
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
                        System.err.println("无法加载主界面: " + ioException.getMessage());
                    }
                } else {
                    showInfo("登录失败，用户或密码错误，请重试");
                }
            } else {
                // response == null，视为连接失败
                showInfo("服务器连接失败，请稍后重试");
            }
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            showInfo("服务器连接失败，请稍后重试");
        });

        new Thread(task, "login-request-thread").start();
    }

    /**
     * 处理注册按钮点击事件（占位）。
     */
    @FXML
    void register(ActionEvent event) {
        // 预留：注册逻辑
    }

    /**
     * 以模态对话框的方式展示提示信息。
     *
     * @param message 提示消息文本
     */
    private void showInfo(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/components/InfoDialog.fxml"));
            Parent dialogRoot = loader.load();
            InfoDialog controller = loader.getController();
            controller.setMessage(message);
            Stage owner = (Stage) loginButton.getScene().getWindow();
            Stage dialogStage = new Stage();
            controller.setDialogStage(dialogStage);
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("提示");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.showAndWait();
        } catch (IOException ioException) {
            System.err.println("无法显示提示对话框: " + ioException.getMessage());
        }
    }
}
