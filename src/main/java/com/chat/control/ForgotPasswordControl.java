package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ResetPasswordResponse;
import com.chat.service.RegistrationService;
import com.chat.ui.DialogUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * 忘记密码页面控制器 - 仅处理UI交互
 */
public class ForgotPasswordControl {

    @FXML private TextField keyField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        newPasswordField.setOnAction(event -> resetPassword());
        confirmPasswordField.setOnAction(event -> resetPassword());
    }

    @FXML
    private void resetPassword() {
        String key = keyField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 使用Service验证输入
        String validationError = RegistrationService.validateResetPasswordInput(key, newPassword, confirmPassword);
        if (validationError != null) {
            showStatus(validationError, true);
            return;
        }

        resetButton.setDisable(true);
        showStatus("正在重置密码...", false);

        Task<ResetPasswordResponse> task = new Task<ResetPasswordResponse>() {
            @Override
            protected ResetPasswordResponse call() {
                try {
                    SocketClient client = new SocketClient();
                    RegistrationService service = new RegistrationService();
                    return service.resetPassword(client, key, newPassword);
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

    private void handleResetPasswordResponse(ResetPasswordResponse response) {
        if (response == null) {
            showStatus("重置密码失败，服务器无响应", true);
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

    private void showStatus(String message, boolean isError) {
        // 这里可以显示状态标签，但原代码中statusLabel可能已被删除
        System.out.println("状态: " + message);
    }
}