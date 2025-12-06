package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.RegistrationService;
import com.chat.ui.DialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SettingControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private Label usernameLabel;
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button updatePasswordButton;
    @FXML private Label passwordStatusLabel;
    @FXML private Button logoutButton;

    private SocketClient socketClient;
    private String userId;
    private String username;
    private RegistrationService registrationService;
    private Runnable logoutCallback;
    private Consumer<Boolean> passwordResetCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        if (oldPasswordField != null) {
            oldPasswordField.setPromptText("请输入当前密码");
        }
        if (newPasswordField != null) {
            newPasswordField.setPromptText("请输入新密码");
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.setPromptText("请再次输入新密码");
        }
        if (passwordStatusLabel != null) {
            passwordStatusLabel.setText("");
            passwordStatusLabel.setStyle("-fx-font-size: 13px;");
        }
    }

    private void setupEventHandlers() {
        if (updatePasswordButton != null) {
            updatePasswordButton.setOnAction(event -> updatePassword());
        }
        if (logoutButton != null) {
            logoutButton.setOnAction(event -> logout());
        }
    }

    @FXML
    private void updatePassword() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 验证输入
        if (oldPassword.isEmpty()) {
            showPasswordStatus("请输入当前密码", true);
            oldPasswordField.requestFocus();
            return;
        }
        if (newPassword.isEmpty()) {
            showPasswordStatus("请输入新密码", true);
            newPasswordField.requestFocus();
            return;
        }
        if (confirmPassword.isEmpty()) {
            showPasswordStatus("请确认新密码", true);
            confirmPasswordField.requestFocus();
            return;
        }
        if (newPassword.equals(oldPassword)) {
            showPasswordStatus("新密码不能与当前密码相同", true);
            newPasswordField.requestFocus();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showPasswordStatus("两次输入的新密码不一致", true);
            confirmPasswordField.requestFocus();
            return;
        }

        // 调用Service修改密码
        updatePasswordOnServer(oldPassword, newPassword);
    }

    private void updatePasswordOnServer(String oldPassword, String newPassword) {
        if (!checkConnection()) {
            showPasswordStatus("网络连接失败，请检查连接", true);
            return;
        }

        updatePasswordButton.setDisable(true);

        new Thread(() -> {
            try {
                registrationService = new RegistrationService();
                boolean success = registrationService.changePassword(
                        socketClient, userId, oldPassword, newPassword);

                Platform.runLater(() -> {
                    updatePasswordButton.setDisable(false);

                    if (success) {
                        showPasswordStatus("密码修改成功", false);
                        clearPasswordFields();

                        DialogHelper.showInfo(mainContainer.getScene().getWindow(),
                                "密码修改成功！\n系统将自动退出到登录界面。");

                        // 延迟后执行退出
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Platform.runLater(() -> performLogoutAfterPasswordReset());
                        }).start();
                    } else {
                        showPasswordStatus("密码修改失败", true);
                        oldPasswordField.clear();
                        oldPasswordField.requestFocus();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updatePasswordButton.setDisable(false);
                    showPasswordStatus("密码修改失败: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void performLogoutAfterPasswordReset() {
        try {
            if (socketClient != null) {
                socketClient.disconnect();
            }

            if (passwordResetCallback != null) {
                passwordResetCallback.accept(true);
            }

            Platform.runLater(() -> {
                Stage stage = (Stage) mainContainer.getScene().getWindow();
                if (stage != null && stage.isShowing()) {
                    stage.close();
                }

                if (logoutCallback != null) {
                    logoutCallback.run();
                }
            });

        } catch (Exception e) {
            System.err.println("执行密码修改后退出时发生错误: " + e.getMessage());
        }
    }

    private void showPasswordStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            if (passwordStatusLabel != null) {
                passwordStatusLabel.setText(message);
                passwordStatusLabel.setStyle(isError ?
                        "-fx-text-fill: #f44336; -fx-font-size: 13px;" :
                        "-fx-text-fill: #4CAF50; -fx-font-size: 13px;");
            }
        });
    }

    private void clearPasswordFields() {
        Platform.runLater(() -> {
            oldPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        });
    }

    private boolean checkConnection() {
        return socketClient != null && socketClient.isConnected();
    }

    @FXML
    private void logout() {
        try {
            boolean confirm = DialogHelper.showConfirmation(mainContainer.getScene().getWindow(),
                    "确定要退出登录吗？\n退出后需要重新登录。");

            if (confirm) {
                performNormalLogout();
            }
        } catch (Exception e) {
            System.err.println("Error in logout: " + e.getMessage());
        }
    }

    private void performNormalLogout() {
        if (socketClient != null) {
            socketClient.disconnect();
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            if (stage != null && stage.isShowing()) {
                stage.close();
            }

            if (logoutCallback != null) {
                logoutCallback.run();
            }
        });
    }

    public void setUserInfo(String username, String userId) {
        this.username = username;
        this.userId = userId;

        Platform.runLater(() -> {
            if (mainContainer != null && mainContainer.getScene() != null) {
                Stage stage = (Stage) mainContainer.getScene().getWindow();
                if (stage != null) {
                    stage.setTitle("设置 - " + username);
                }
            }

            if (usernameLabel != null) {
                usernameLabel.setText("用户: " + username);
            }
        });
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    public void setLogoutCallback(Runnable logoutCallback) {
        this.logoutCallback = logoutCallback;
    }

    public void setPasswordResetCallback(Consumer<Boolean> passwordResetCallback) {
        this.passwordResetCallback = passwordResetCallback;
    }
}