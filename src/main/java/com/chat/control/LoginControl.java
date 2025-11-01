package com.chat.control;

import com.chat.model.LoginRequest;
import com.chat.model.Request;
import com.chat.network.SocketClient;
import com.chat.ui.CustomButton;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 登录界面的控制器
 */
public class LoginControl {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CustomButton loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink forgotPasswordLink;

    private SocketClient socketClient;

    @FXML
    public void initialize() {
        usernameField.setOnAction(this::handleLogin);
        passwordField.setOnAction(this::handleLogin);
    }

    @FXML
    void login(ActionEvent event) {
        handleLogin(event);
    }

    @FXML
    void register(ActionEvent event) {
        DialogUtil.showInfo(loginButton.getScene().getWindow(), "注册功能开发中...");
    }

    @FXML
    void forgotPassword(ActionEvent event) {
        DialogUtil.showInfo(loginButton.getScene().getWindow(), "忘记密码功能开发中...");
    }

    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "请输入用户名和密码");
            return;
        }

        loginButton.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                socketClient = new SocketClient();
                return sendLoginRequest(username, password);
            }
        };

        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            handleLoginResponse(task.getValue(), username);
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败");
            if (socketClient != null) socketClient.disconnect();
        });

        new Thread(task).start();
    }

    private String sendLoginRequest(String username, String password) {
        Gson gson = new Gson();

        // 尝试不同的请求格式
        String[] types = {"LOGIN", "AUTH", "USER_LOGIN"};

        for (String type : types) {
            // 格式1: 使用Request包装
            Request request = new Request(type, new LoginRequest(username, password));
            String response = socketClient.sendLoginRequest(gson.toJson(request));
            if (isValidResponse(response)) return response;

            // 格式2: 直接JSON
            JsonObject directJson = new JsonObject();
            directJson.addProperty("type", type);
            directJson.addProperty("username", username);
            directJson.addProperty("password", password);
            response = socketClient.sendLoginRequest(gson.toJson(directJson));
            if (isValidResponse(response)) return response;
        }

        // 格式3: 简单JSON
        JsonObject simpleJson = new JsonObject();
        simpleJson.addProperty("username", username);
        simpleJson.addProperty("password", password);
        return socketClient.sendLoginRequest(gson.toJson(simpleJson));
    }

    private boolean isValidResponse(String response) {
        return response != null && !response.contains("UNKNOWN") && !response.contains("Unsupported");
    }

    private void handleLoginResponse(String response, String username) {
        if (response == null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器无响应");
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String status = json.has("status") ? json.get("status").getAsString() : "";
            String message = json.has("message") ? json.get("message").getAsString() : "";

            if ("SUCCESS".equals(status) || "OK".equals(status)) {
                showMainWindow(username);
                closeLoginWindow();
            } else {
                DialogUtil.showInfo(loginButton.getScene().getWindow(), "登录失败: " + message);
                socketClient.disconnect();
            }
        } catch (Exception e) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "响应解析错误");
            socketClient.disconnect();
        }
    }

    private void showMainWindow(String username) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setUsername(username);
        controller.setSocketClient(socketClient);

        Stage stage = new Stage();
        stage.setTitle("中杯聊天软件 - " + username);
        stage.setScene(new Scene(root, 700, 500));
        stage.show();
    }

    private void closeLoginWindow() {
        ((Stage) loginButton.getScene().getWindow()).close();
    }
}