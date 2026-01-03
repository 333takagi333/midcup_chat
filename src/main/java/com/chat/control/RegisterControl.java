package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.RegisterResponse;
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
 * 注册页面控制器 - 仅处理UI交互
 */
public class RegisterControl {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        passwordField.setOnAction(event -> register());
        confirmPasswordField.setOnAction(event -> register());
    }

    @FXML
    private void register() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 使用Service验证输入
        String validationError = RegistrationService.validateRegistrationInput(username, password, confirmPassword);
        if (validationError != null) {
            DialogUtil.showError(usernameField.getScene().getWindow(), validationError);
            return;
        }

        registerButton.setDisable(true);

        Task<RegisterResponse> task = new Task<RegisterResponse>() {
            @Override
            protected RegisterResponse call() {
                try {
                    SocketClient client = new SocketClient();
                    RegistrationService service = new RegistrationService();
                    return service.register(client, username, password);
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
            DialogUtil.showError(usernameField.getScene().getWindow(), "注册失败，网络错误");
        });

        new Thread(task).start();
    }

    private void handleRegisterResponse(RegisterResponse response) {
        if (response == null) {
            DialogUtil.showError(usernameField.getScene().getWindow(), "注册失败，服务器无响应");
            return;
        }

        if (response.isSuccess()) {
            showRegistrationSuccessDialog(response.getUid(), response.getSecretKey());
        } else {
            String errorMsg = "注册失败";
            if (response.getMessage() != null && !response.getMessage().isBlank()) {
                errorMsg += ": " + response.getMessage();
            }
            DialogUtil.showError(usernameField.getScene().getWindow(), errorMsg);
        }
    }

    private void showRegistrationSuccessDialog(Long uid, String secretKey) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/registration-success.fxml"));
            Parent root = loader.load();

            RegistrationSuccessControl controller = loader.getController();
            controller.setRegistrationInfo(uid != null ? uid.toString() : "", secretKey);
            controller.setOnConfirmCallback(this::backToLogin);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("注册成功");
            dialogStage.setScene(new Scene(root, 400, 320));
            dialogStage.setResizable(false);
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(registerButton.getScene().getWindow());
            dialogStage.show();

        } catch (Exception e) {
            System.err.println("加载注册成功窗口失败: " + e.getMessage());
            DialogUtil.showInfo(registerButton.getScene().getWindow(),
                    "注册成功！\n用户ID: " + uid + "\n登录密钥: " + secretKey);
            backToLogin();
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
}