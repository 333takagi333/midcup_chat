package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ChangePasswordRequest;
import com.chat.ui.DialogHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
    private final Gson gson = new Gson();
    private Runnable logoutCallback;
    private Consumer<Boolean> passwordResetCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        try {
            // 设置密码相关字段提示文本
            if (oldPasswordField != null) {
                oldPasswordField.setPromptText("请输入当前密码");
            }
            if (newPasswordField != null) {
                newPasswordField.setPromptText("请输入新密码");
            }
            if (confirmPasswordField != null) {
                confirmPasswordField.setPromptText("请再次输入新密码");
            }

            // 设置状态标签
            if (passwordStatusLabel != null) {
                passwordStatusLabel.setText("");
                passwordStatusLabel.setStyle("-fx-font-size: 13px;");
            }

        } catch (Exception e) {
            System.err.println("Error in setupUI: " + e.getMessage());
        }
    }

    private void setupEventHandlers() {
        try {
            if (updatePasswordButton != null) {
                updatePasswordButton.setOnAction(event -> updatePassword());
            }
            if (logoutButton != null) {
                logoutButton.setOnAction(event -> logout());
            }
        } catch (Exception e) {
            System.err.println("Error in setupEventHandlers: " + e.getMessage());
        }
    }

    // ========== 修改密码功能 ==========

    @FXML
    private void updatePassword() {
        try {
            String oldPassword = oldPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // 基本验证
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

            // 验证通过，执行密码修改
            changePasswordOnServer(oldPassword, newPassword);
        } catch (Exception e) {
            showPasswordStatus("操作失败: " + e.getMessage(), true);
        }
    }

    private void changePasswordOnServer(String oldPassword, String newPassword) {
        if (!checkConnection()) {
            showPasswordStatus("网络连接失败，请检查连接", true);
            return;
        }

        try {
            // 创建修改密码请求对象
            ChangePasswordRequest request = new ChangePasswordRequest(userId, oldPassword, newPassword);

            // 使用专门的发送方法
            String response = socketClient.sendChangePasswordRequest(request);

            if (response != null) {
                JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                if (responseObj.has("success") && responseObj.get("success").getAsBoolean()) {
                    showPasswordStatus("密码修改成功", false);
                    clearPasswordFields();

                    // 密码修改成功后，显示提示并执行退出
                    Platform.runLater(() -> {
                        DialogHelper.showInfo(mainContainer.getScene().getWindow(),
                                "密码修改成功！\n系统将自动退出到登录界面。");

                        // 延迟1秒后执行退出
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Platform.runLater(this::performLogoutAfterPasswordReset);
                        }).start();
                    });
                } else {
                    String errorMsg = responseObj.has("message") ?
                            responseObj.get("message").getAsString() : "密码修改失败";
                    showPasswordStatus(errorMsg, true);
                    oldPasswordField.clear();
                    oldPasswordField.requestFocus();
                }
            } else {
                showPasswordStatus("服务器无响应，请稍后重试", true);
            }
        } catch (Exception e) {
            showPasswordStatus("密码修改失败: " + e.getMessage(), true);
        }
    }

    private void performLogoutAfterPasswordReset() {
        try {
            // 断开网络连接
            if (socketClient != null) {
                socketClient.disconnect();
            }

            // 通知主窗口关闭
            if (passwordResetCallback != null) {
                passwordResetCallback.accept(true);
            }

            // 关闭当前设置窗口
            Platform.runLater(() -> {
                Stage stage = (Stage) mainContainer.getScene().getWindow();
                if (stage != null && stage.isShowing()) {
                    stage.close();
                }

                // 调用退出回调返回到登录界面
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

    // ========== 退出登录功能 ==========

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
        // 断开连接
        if (socketClient != null) {
            socketClient.disconnect();
        }

        // 关闭窗口并执行回调
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

    // ========== Setter 方法 ==========

    public void setUserInfo(String username, String userId) {
        this.username = username;
        this.userId = userId;

        Platform.runLater(() -> {
            // 设置窗口标题
            if (mainContainer != null && mainContainer.getScene() != null) {
                Stage stage = (Stage) mainContainer.getScene().getWindow();
                if (stage != null) {
                    stage.setTitle("设置 - " + username);
                }
            }

            // 设置用户名标签
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