package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.RegisterRequest;
import com.chat.protocol.RegisterResponse;
import com.chat.protocol.MessageType;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * 注册页面控制器
 */
public class RegisterControl {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private SocketClient socketClient;
    private Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 设置回车键注册
        passwordField.setOnAction(event -> register());
        confirmPasswordField.setOnAction(event -> register());
    }

    /**
     * 注册按钮点击事件
     */
    @FXML
    private void register() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 输入验证
        if (!validateInput(username, password, confirmPassword)) {
            return;
        }

        registerButton.setDisable(true);
        showStatus("正在注册...", false);

        Task<RegisterResponse> task = new Task<RegisterResponse>() {
            @Override
            protected RegisterResponse call() {
                try {
                    socketClient = new SocketClient();
                    String encryptedPassword = PasswordEncryptor.encrypt(password);
                    RegisterRequest request = new RegisterRequest(username, encryptedPassword);
                    String response = socketClient.sendRegisterRequest(request);

                    if (response != null) {
                        return gson.fromJson(response, RegisterResponse.class);
                    }
                    return null;
                } catch (Exception e) {
                    System.err.println("注册请求异常: " + e.getMessage());
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            registerButton.setDisable(false);
            handleRegisterResponse(task.getValue());
        });

        task.setOnFailed(e -> {
            registerButton.setDisable(false);
            showStatus("注册失败，网络错误", true);
        });

        new Thread(task).start();
    }

    /**
     * 处理注册响应
     */
    private void handleRegisterResponse(RegisterResponse response) {
        if (response == null) {
            showStatus("注册失败，服务器无响应", true);
            return;
        }

        // 校验响应类型
        if (!MessageType.REGISTER_RESPONSE.equals(response.getType())) {
            showStatus("注册失败，响应类型不匹配", true);
            return;
        }

        if (response.isSuccess()) {
            String successMsg = response.getMessage() != null ? response.getMessage() : "注册成功！";
            DialogUtil.showInfo(registerButton.getScene().getWindow(), successMsg);
            backToLogin();
        } else {
            String errorMsg = "注册失败";
            if (response.getMessage() != null && !response.getMessage().isBlank()) {
                errorMsg += ": " + response.getMessage();
            }
            showStatus(errorMsg, true);
        }
    }

    /**
     * 输入验证
     */
    private boolean validateInput(String username, String password, String confirmPassword) {
        if (username.isEmpty()) {
            showStatus("请输入用户名", true);
            return false;
        }
        if (password.isEmpty()) {
            showStatus("请输入密码", true);
            return false;
        }
        if (confirmPassword.isEmpty()) {
            showStatus("请确认密码", true);
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showStatus("两次输入的密码不一致", true);
            return false;
        }

        return true;
    }

    /**
     * 返回登录页面
     */
    @FXML
    private void backToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 400));
            stage.setTitle("中杯聊天软件 - 登录");
        } catch (Exception e) {
            System.err.println("返回登录页面失败: " + e.getMessage());
            DialogUtil.showInfo(backButton.getScene().getWindow(), "返回登录页面失败");
        }
    }

    /**
     * 显示状态信息
     */
    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            if (isError) {
                statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
            }
        }
    }
}