package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.LoginRequest;
import com.chat.protocol.LoginResponse;
import com.chat.ui.CustomButton;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
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
 * 登录界面的控制器 - UID登录
 */
public class LoginControl {

    @FXML private TextField uidField;
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
        uidField.setOnAction(this::handleLogin);
        passwordField.setOnAction(this::handleLogin);

        // 设置提示文本
        uidField.setPromptText("请输入用户ID（数字）");
    }

    /**
     * 登录按钮事件入口。
     */
    @FXML
    void login(ActionEvent event) {
        handleLogin(event);
    }

    /**
     * 注册入口。
     */
    @FXML
    void register(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerLink.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 400));
            stage.setTitle("中杯聊天软件 - 注册");
        } catch (Exception e) {
            System.err.println("加载注册页面失败: " + e.getMessage());
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "加载注册页面失败");
        }
    }

    /**
     * 忘记密码入口。
     */
    @FXML
    void forgotPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/forgot_password.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) forgotPasswordLink.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 400));
            stage.setTitle("中杯聊天软件 - 忘记密码");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "加载忘记密码页面失败");
        }
    }

    /**
     * 执行登录流程：校验输入 -> 后台线程向服务器发送登录请求 -> 处理响应。
     */
    private void handleLogin(ActionEvent event) {
        String uidText = uidField.getText().trim();
        String password = passwordField.getText();

        // 输入验证
        Long uid;
        try {
            uid = validateInput(uidText, password);
        } catch (IllegalArgumentException e) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), e.getMessage());
            return;
        }

        loginButton.setDisable(true);
        final Long finalUid = uid;

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                socketClient = new SocketClient();
                return sendLoginRequest(finalUid, password);
            }
        };

        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            handleLoginResponse(task.getValue(), finalUid);
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败，请稍后重试");
            if (socketClient != null) socketClient.disconnect();
        });

        new Thread(task).start();
    }

    /**
     * 输入验证
     */
    private Long validateInput(String uidText, String password) throws IllegalArgumentException {
        if (uidText.isEmpty()) {
            uidField.requestFocus();
            throw new IllegalArgumentException("请输入用户ID");
        }

        // 验证UID是否为数字
        Long uid;
        try {
            uid = Long.parseLong(uidText);
        } catch (NumberFormatException e) {
            uidField.requestFocus();
            throw new IllegalArgumentException("用户ID必须是数字");
        }

        if (password.isEmpty()) {
            passwordField.requestFocus();
            throw new IllegalArgumentException("请输入密码");
        }

        return uid;
    }

    /**
     * 组装并发送登录请求
     */
    private String sendLoginRequest(Long uid, String password) {
        // 加密密码
        String encrypted = PasswordEncryptor.encrypt(password);

        // 创建登录请求，使用Long类型的uid
        LoginRequest request = new LoginRequest(uid, encrypted);
        return socketClient.sendLoginRequest(request);
    }

    /**
     * 处理登录响应
     */
    private void handleLoginResponse(String response, Long inputUid) {
        if (response == null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败，请稍后重试");
            return;
        }

        // 解析JSON响应
        LoginResponse resp;
        try {
            Gson gson = new Gson();
            resp = gson.fromJson(response, LoginResponse.class);
        } catch (Exception e) {
            System.out.println("Server response: " + response);
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "响应解析错误: " + e.getMessage() + "，请稍后重试");
            if (socketClient != null) socketClient.disconnect();
            return;
        }

        // 校验响应类型
        if (resp == null || resp.getType() == null || !com.chat.protocol.MessageType.LOGIN_RESPONSE.equals(resp.getType())) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "响应类型不匹配，稍后重试");
            if (socketClient != null) socketClient.disconnect();
            return;
        }

        boolean loginOk = resp.isSuccess();
        String message = resp.getMessage();
        String responseUid = resp.getUid(); // 服务器返回的UID（String类型）

        if (!loginOk) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(),
                    (message == null || message.isBlank()) ? "登录失败，用户ID或密码错误，请重试" : message);
            if (socketClient != null) socketClient.disconnect();
            return;
        }

        // 登录成功，加载主界面
        try {
            // 获取用户名
            String username = resp.getUsername();
            if (username == null || username.isEmpty()) {
                username = "用户" + inputUid;
            }

            // 使用服务器返回的uid（如果提供了的话），否则使用输入的uid
            String actualUid = (responseUid != null && !responseUid.isEmpty()) ? responseUid : inputUid.toString();

            showMainWindow(username, actualUid);
            closeLoginWindow();
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "加载主界面失败: " + e.getMessage());
            if (socketClient != null) socketClient.disconnect();
        }
    }

    /**
     * 打开主界面并传入用户名、用户ID与已建立的 SocketClient。
     */
    private void showMainWindow(String username, String uid) throws IOException {
        try {
            var fxmlUrl = getClass().getResource("/com/chat/fxml/main.fxml");
            if (fxmlUrl == null) {
                System.err.println("[FXML] 资源未找到: /com/chat/fxml/main.fxml");
                throw new IOException("找不到 main.fxml 资源");
            }
            System.out.println("[FXML] 成功找到资源: " + fxmlUrl.toExternalForm());

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("[FXML] FXML 加载成功");

            MainControl controller = loader.getController();
            System.out.println("[FXML] 控制器获取: " + (controller != null ? "成功" : "失败"));

            controller.setUsername(username);
            controller.setSocketClient(socketClient);
            controller.setUserId(uid);

            Stage stage = new Stage();
            stage.setTitle("中杯聊天软件 - " + username);
            stage.setScene(new Scene(root, 700, 500));
            stage.show();
            System.out.println("[FXML] 主窗口显示成功");

        } catch (Exception e) {
            System.err.println("[FXML] 加载 main.fxml 失败: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new IOException("加载主界面失败: " + e.getMessage(), e);
        }
    }

    /**
     * 关闭当前登录窗口。
     */
    private void closeLoginWindow() {
        ((Stage) loginButton.getScene().getWindow()).close();
    }
}