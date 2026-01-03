package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.LoginResponse;
import com.chat.service.RegistrationService;
import com.chat.ui.CustomButton;
import com.chat.ui.DialogUtil;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 登录界面的控制器 - 仅处理UI交互
 */
public class LoginControl {

    @FXML private TextField uidField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink forgotPasswordLink;

    private SocketClient socketClient;
    private RegistrationService registrationService;

    @FXML
    public void initialize() {
        uidField.setOnAction(this::handleLogin);
        passwordField.setOnAction(this::handleLogin);
        uidField.setPromptText("请输入用户ID（数字）");
    }

    @FXML
    void login(ActionEvent event) {
        handleLogin(event);
    }

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

    private void handleLogin(ActionEvent event) {
        String uidText = uidField.getText().trim();
        String password = passwordField.getText();

        // 使用Service验证输入
        String validationError = RegistrationService.validateLoginInput(uidText, password);
        if (validationError != null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), validationError);
            return;
        }

        Long uid = Long.parseLong(uidText);
        loginButton.setDisable(true);

        // 使用新的方法返回连接
        Task<RegistrationService.LoginResult> task = new Task<RegistrationService.LoginResult>() {
            @Override
            protected RegistrationService.LoginResult call() {
                try {
                    RegistrationService service = new RegistrationService();
                    return service.loginWithConnection(uid, password);
                } catch (Exception e) {
                    System.err.println("登录请求异常: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            handleLoginResult(task.getValue(), uid);
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "服务器连接失败，请稍后重试");
            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        new Thread(task).start();
    }

    private void handleLoginResult(RegistrationService.LoginResult loginResult, Long inputUid) {
        if (loginResult == null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "登录失败，无法连接到服务器");
            return;
        }

        LoginResponse resp = loginResult.getResponse();
        SocketClient connectedClient = loginResult.getSocketClient();

        if (resp == null) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "登录失败，服务器响应异常");
            if (connectedClient != null) {
                connectedClient.disconnect();
            }
            return;
        }

        // 校验响应类型
        if (!com.chat.protocol.MessageType.LOGIN_RESPONSE.equals(resp.getType())) {
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "响应类型不匹配，稍后重试");
            if (connectedClient != null) {
                connectedClient.disconnect();
            }
            return;
        }

        if (!resp.isSuccess()) {
            String message = resp.getMessage();
            DialogUtil.showInfo(loginButton.getScene().getWindow(),
                    (message == null || message.isBlank()) ? "登录失败，用户ID或密码错误，请重试" : message);
            if (connectedClient != null) {
                connectedClient.disconnect();
            }
            return;
        }

        // 登录成功，加载主界面
        try {
            String username = resp.getUsername();
            if (username == null || username.isEmpty()) {
                username = "用户" + inputUid;
            }

            String actualUid = (resp.getUid() != null && !resp.getUid().isEmpty()) ? resp.getUid() : inputUid.toString();

            // 保存连接供主界面使用
            this.socketClient = connectedClient;

            System.out.println("[LoginControl] 登录成功，用户: " + username + ", UID: " + actualUid);
            System.out.println("[LoginControl] Socket连接状态: " +
                    (socketClient != null ? socketClient.isConnected() : "null"));

            showMainWindow(username, actualUid);
            closeLoginWindow();
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showInfo(loginButton.getScene().getWindow(), "加载主界面失败: " + e.getMessage());
            if (connectedClient != null) {
                connectedClient.disconnect();
            }
        }
    }

    private void showMainWindow(String username, String uid) throws IOException {
        try {
            var fxmlUrl = getClass().getResource("/com/chat/fxml/main.fxml");
            if (fxmlUrl == null) {
                System.err.println("[FXML] 资源未找到: /com/chat/fxml/main.fxml");
                throw new IOException("找不到 main.fxml 资源");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            MainControl controller = loader.getController();
            controller.setUsername(username);
            controller.setUserId(uid);

            // 关键：传递已经成功连接的 socketClient
            controller.setSocketClient(socketClient);

            Stage stage = new Stage();
            stage.setTitle("中杯聊天软件 - " + username);
            stage.setScene(new Scene(root, 700, 500));
            stage.show();

        } catch (Exception e) {
            System.err.println("[FXML] 加载 main.fxml 失败: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new IOException("加载主界面失败: " + e.getMessage(), e);
        }
    }

    private void closeLoginWindow() {
        ((Stage) loginButton.getScene().getWindow()).close();
    }
}