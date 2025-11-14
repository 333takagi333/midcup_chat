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
        String encrypted = PasswordEncryptor.encrypt(password);
        LoginRequest request = new LoginRequest(username, encrypted);
        return socketClient.sendLoginRequest(request);
    }

    /**
     * 按照当前标准处理登录响应：顶层字段必须包含 type、success，可选 uid、message。
     */
    private void handleLoginResponse(String response, String username) {
        if (response == null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败，请稍后重试");
            return;
        }

        // 先解析 JSON
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
        String uid = resp.getUid();
        String message = resp.getMessage();

        if (!loginOk) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(),
                    (message == null || message.isBlank()) ? "登录失败，用户或密码错误，请重试" : message);
            if (socketClient != null) socketClient.disconnect();
            return;
        }

        // 登录成功，加载主界面（与响应解析分开处理）
        try {
            showMainWindow(username, uid);
            closeLoginWindow();
        } catch (IOException e) {
            // 将加载失败的详细堆栈直接打印到控制台，便于定位 FXML 事件/ID 不匹配问题
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
                System.err.println("[FXML] 当前类路径: " + System.getProperty("java.class.path"));
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
            if (uid != null && !uid.isBlank()) {
                controller.setUserId(uid);
            }

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
