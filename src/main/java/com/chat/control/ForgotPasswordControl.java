package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ResetPasswordRequest;
import com.chat.protocol.ResetPasswordResponse;
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
 * 忘记密码页面控制器
 */
public class ForgotPasswordControl {

    @FXML private TextField keyField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private SocketClient socketClient;
    private Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 设置回车键重置密码
        newPasswordField.setOnAction(event -> resetPassword());
        confirmPasswordField.setOnAction(event -> resetPassword());
    }

    /**
     * 重置密码按钮点击事件
     */
    @FXML
    private void resetPassword() {
        String key = keyField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 输入验证
        if (!validateInput(key, newPassword, confirmPassword)) {
            return;
        }

        resetButton.setDisable(true);
        showStatus("正在重置密码...", false);

        Task<ResetPasswordResponse> task = new Task<ResetPasswordResponse>() {
            @Override
            protected ResetPasswordResponse call() {
                try {
                    socketClient = new SocketClient();
                    String encryptedPassword = PasswordEncryptor.encrypt(newPassword);
                    ResetPasswordRequest request = new ResetPasswordRequest(key, encryptedPassword);
                    String response = socketClient.sendResetPasswordRequest(request);

                    if (response != null) {
                        return gson.fromJson(response, ResetPasswordResponse.class);
                    }
                    return null;
                } catch (Exception e) {
                    System.err.println("重置密码请求异常: " + e.getMessage());
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            resetButton.setDisable(false);
            handleResetPasswordResponse(task.getValue());
        });

        task.setOnFailed(e -> {
            resetButton.setDisable(false);
            showStatus("重置密码失败，网络错误", true);
        });

        new Thread(task).start();
    }

    /**
     * 处理重置密码响应
     */
    private void handleResetPasswordResponse(ResetPasswordResponse response) {
        if (response == null) {
            showStatus("重置密码失败，服务器无响应", true);
            return;
        }

        // 校验响应类型
        if (!MessageType.RESET_PASSWORD_RESPONSE.equals(response.getType())) {
            showStatus("重置密码失败，响应类型不匹配", true);
            return;
        }

        if (response.isSuccess()) {
            String successMsg = response.getMessage() != null ? response.getMessage() : "密码重置成功！";
            DialogUtil.showInfo(resetButton.getScene().getWindow(), successMsg);
            backToLogin();
        } else {
            String errorMsg = "密码重置失败";
            if (response.getMessage() != null && !response.getMessage().isBlank()) {
                errorMsg += ": " + response.getMessage();
            }
            showStatus(errorMsg, true);
        }
    }

    /**
     * 输入验证
     */
    private boolean validateInput(String key, String newPassword, String confirmPassword) {
        if (key.isEmpty()) {
            showStatus("请输入安全密钥", true);
            return false;
        }
        if (newPassword.isEmpty()) {
            showStatus("请输入新密码", true);
            return false;
        }
        if (confirmPassword.isEmpty()) {
            showStatus("请确认新密码", true);
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
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