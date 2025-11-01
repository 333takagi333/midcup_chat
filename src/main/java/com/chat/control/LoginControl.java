package com.chat.control;

import com.chat.model.Request;
import com.chat.network.SocketClient;
import com.chat.protocol.LoginRequest;
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

    /**
     * 初始化输入框的回车行为。
     */
    @FXML
    public void initialize() {
        usernameField.setOnAction(this::handleLogin);
        passwordField.setOnAction(this::handleLogin);
    }

    /**
     * 登录按钮事件入口。
     */
    @FXML
    void login(ActionEvent event) {
        handleLogin(event);
    }

    /**
     * 注册入口（占位）。
     */
    @FXML
    void register(ActionEvent event) {
        DialogUtil.showInfo(loginButton.getScene().getWindow(), "注册功能开发中...");
    }

    /**
     * 忘记密码入口（占位）。
     */
    @FXML
    void forgotPassword(ActionEvent event) {
        DialogUtil.showInfo(loginButton.getScene().getWindow(), "忘记密码功能开发中...");
    }

    /**
     * 执行登录流程：校验输入 -> 后台线程向服务器发送登录请求 -> 处理响应。
     */
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
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败，请稍后重试");
            if (socketClient != null) socketClient.disconnect();
        });

        new Thread(task).start();
    }

    /**
     * 组装并发送登录请求，遵循协议 type=login_request。
     * @return 服务器返回的首行响应字符串，失败时返回 null
     */
    private String sendLoginRequest(String username, String password) {
        Gson gson = new Gson();
        String encrypted = PasswordEncryptor.encrypt(password);
        Request request = new Request(com.chat.protocol.MessageType.LOGIN_REQUEST, new LoginRequest(username, encrypted));
        return socketClient.sendLoginRequest(gson.toJson(request));
    }

    /**
     * 处理登录响应：支持规范化的 login_response，也兼容旧格式（status 字段）。
     */
    private void handleLoginResponse(String response, String username) {
        if (response == null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败，请稍后重试");
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "";
            String status = json.has("status") ? json.get("status").getAsString() : "";
            String message = json.has("message") ? json.get("message").getAsString() : "";

            boolean loginOk = false;
            if (com.chat.protocol.MessageType.LOGIN_RESPONSE.equals(type)) {
                // 标准协议：从 data 中读取 authenticated
                if (json.has("data") && json.get("data").isJsonObject()) {
                    JsonObject data = json.getAsJsonObject("data");
                    loginOk = data.has("authenticated") && data.get("authenticated").getAsBoolean();
                    if (!loginOk && data.has("error")) {
                        message = translateLoginError(data.get("error").getAsString(), message);
                    }
                }
            } else {
                // 兼容旧协议：依据 status 判断
                loginOk = "SUCCESS".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(status);
                if (!loginOk && (message == null || message.isBlank())) {
                    message = "登录失败，用户或密码错误，请重试";
                }
            }

            if (loginOk) {
                showMainWindow(username);
                closeLoginWindow();
            } else {
                DialogUtil.showInfo(loginButton.getScene().getWindow(),
                        message == null || message.isBlank() ? "登录失败，用户或密码错误，请重试" : message);
                socketClient.disconnect();
            }
        } catch (Exception e) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "响应解析错误，请稍后重试");
            socketClient.disconnect();
        }
    }

    /**
     * 将服务端错误码翻译为更友好的提示文案。
     */
    private String translateLoginError(String code, String fallback) {
        if (code == null) return fallback;
        return switch (code) {
            case "INVALID_CREDENTIALS" -> "登录失败，用户或密码错误，请重试";
            case "ACCOUNT_LOCKED" -> "账号已被锁定，请稍后再试或联系管理员";
            default -> (fallback == null || fallback.isBlank()) ? "登录失败，请重试" : fallback;
        };
    }

    /**
     * 打开主界面并传入用户名与已建立的 SocketClient。
     */
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

    /**
     * 关闭当前登录窗口。
     */
    private void closeLoginWindow() {
        ((Stage) loginButton.getScene().getWindow()).close();
    }
}
