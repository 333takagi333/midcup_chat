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
    @FXML private Label statusLabel; // 这个也可以删除了

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
        // 可以保留statusLabel的简单状态显示
        if (statusLabel != null) {
            statusLabel.setText("正在注册...");
            statusLabel.setStyle("-fx-text-fill: #007bff; -fx-font-size: 12px;");
        }

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
            // 网络错误时显示简单提示
            if (statusLabel != null) {
                statusLabel.setText("注册失败，网络错误");
                statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
            }
        });

        new Thread(task).start();
    }

    /**
     * 处理注册响应
     */
    private void handleRegisterResponse(RegisterResponse response) {
        if (response == null) {
            // 服务器无响应
            if (statusLabel != null) {
                statusLabel.setText("注册失败，服务器无响应");
                statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
            }
            return;
        }

        // 校验响应类型
        if (!MessageType.REGISTER_RESPONSE.equals(response.getType())) {
            if (statusLabel != null) {
                statusLabel.setText("注册失败，响应类型不匹配");
                statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
            }
            return;
        }

        if (response.isSuccess()) {
            // 清除状态标签（如果有）
            if (statusLabel != null) {
                statusLabel.setText("");
            }

            // 显示注册成功信息窗口
            showRegistrationSuccessDialog(response.getUid(), response.getSecretKey());
        } else {
            String errorMsg = "注册失败";
            if (response.getMessage() != null && !response.getMessage().isBlank()) {
                errorMsg += ": " + response.getMessage();
            }
            if (statusLabel != null) {
                statusLabel.setText(errorMsg);
                statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
            }
        }
    }

    /**
     * 输入验证
     */
    private boolean validateInput(String username, String password, String confirmPassword) {
        if (username.isEmpty()) {
            // 可以直接使用DialogUtil显示错误
            DialogUtil.showError(usernameField.getScene().getWindow(), "请输入用户名");
            usernameField.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            DialogUtil.showError(passwordField.getScene().getWindow(), "请输入密码");
            passwordField.requestFocus();
            return false;
        }
        if (confirmPassword.isEmpty()) {
            DialogUtil.showError(confirmPasswordField.getScene().getWindow(), "请确认密码");
            confirmPasswordField.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            DialogUtil.showError(confirmPasswordField.getScene().getWindow(), "两次输入的密码不一致");
            confirmPasswordField.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * 显示注册成功信息对话框
     */
    private void showRegistrationSuccessDialog(Long uid, String secretKey) {
        try {
            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/registration-success.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            RegistrationSuccessControl controller = loader.getController();
            controller.setRegistrationInfo(uid != null ? uid.toString() : "", secretKey);
            controller.setOnConfirmCallback(this::backToLogin);

            // 创建窗口
            Stage dialogStage = new Stage();
            dialogStage.setTitle("注册成功");
            dialogStage.setScene(new Scene(root, 400, 320));
            dialogStage.setResizable(false);

            // 显示模态对话框
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(registerButton.getScene().getWindow());
            dialogStage.show();

        } catch (Exception e) {
            System.err.println("加载注册成功窗口失败: " + e.getMessage());
            e.printStackTrace();

            // 失败时显示简单提示并返回登录
            DialogUtil.showInfo(registerButton.getScene().getWindow(),
                    "注册成功！\n用户ID: " + uid + "\n登录密钥: " + secretKey);
            backToLogin();
        }
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
}