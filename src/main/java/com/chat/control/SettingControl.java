package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ResetPasswordRequest;
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

    // 密钥显示相关
    @FXML private Label keyTitleLabel;
    @FXML private TextField keyValueField;
    @FXML private Button copyKeyButton;

    // 修改密码相关
    @FXML private Label passwordTitleLabel;
    @FXML private TextField recoveryCodeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button updatePasswordButton;
    @FXML private Label passwordStatusLabel;

    // 退出登录按钮
    @FXML private Button logoutButton;

    private SocketClient socketClient;
    private String userId;
    private String username;
    private final Gson gson = new Gson();

    // 回调函数：用于关闭所有窗口并返回登录界面
    private Runnable logoutCallback;
    // 新增：用于通知主窗口关闭的回调
    private Consumer<Boolean> passwordResetCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SettingControl initialized");
        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        try {
            System.out.println("Setting up UI components...");

            // 检查所有组件是否已注入
            checkComponents();

            // 设置密钥字段为只读
            if (keyValueField != null) {
                keyValueField.setEditable(false);
                keyValueField.setStyle("-fx-background-color: #f9f9f9;");
            }

            // 设置密码相关字段提示文本
            if (recoveryCodeField != null) {
                recoveryCodeField.setPromptText("请输入安全密钥");
            }
            if (newPasswordField != null) {
                newPasswordField.setPromptText("请输入新密码");
            }
            if (confirmPasswordField != null) {
                confirmPasswordField.setPromptText("请确认新密码");
            }

            // 设置按钮文本
            if (copyKeyButton != null) {
                copyKeyButton.setText("复制");
                copyKeyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            }
            if (updatePasswordButton != null) {
                updatePasswordButton.setText("修改密码");
                updatePasswordButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            }
            if (logoutButton != null) {
                logoutButton.setText("退出登录");
                logoutButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
            }

            // 设置状态标签
            if (passwordStatusLabel != null) {
                passwordStatusLabel.setText("");
                passwordStatusLabel.setStyle("-fx-font-size: 13px;");
            }

            System.out.println("UI setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error in setupUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkComponents() {
        System.out.println("Checking FXML components:");
        System.out.println("mainContainer: " + (mainContainer != null));
        System.out.println("usernameLabel: " + (usernameLabel != null));
        System.out.println("keyValueField: " + (keyValueField != null));
        System.out.println("copyKeyButton: " + (copyKeyButton != null));
        System.out.println("recoveryCodeField: " + (recoveryCodeField != null));
        System.out.println("newPasswordField: " + (newPasswordField != null));
        System.out.println("confirmPasswordField: " + (confirmPasswordField != null));
        System.out.println("updatePasswordButton: " + (updatePasswordButton != null));
        System.out.println("logoutButton: " + (logoutButton != null));
    }

    private void setupEventHandlers() {
        try {
            if (copyKeyButton != null) {
                copyKeyButton.setOnAction(event -> copyKeyToClipboard());
            }
            if (updatePasswordButton != null) {
                updatePasswordButton.setOnAction(event -> updatePassword());
            }
            if (logoutButton != null) {
                logoutButton.setOnAction(event -> logout());
            }
            System.out.println("Event handlers setup completed");
        } catch (Exception e) {
            System.err.println("Error in setupEventHandlers: " + e.getMessage());
        }
    }

    // ========== 密钥显示功能 ==========

    @FXML
    private void copyKeyToClipboard() {
        try {
            if (keyValueField != null) {
                String key = keyValueField.getText();
                if (key != null && !key.trim().isEmpty() && !key.trim().equals("加载中...")) {
                    Platform.runLater(() -> {
                        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString(key);
                        clipboard.setContent(content);

                        DialogHelper.showInfo(mainContainer.getScene().getWindow(), "密钥已复制到剪贴板");
                    });
                } else {
                    DialogHelper.showError(mainContainer.getScene().getWindow(), "没有可复制的密钥");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in copyKeyToClipboard: " + e.getMessage());
        }
    }

    // ========== 修改密码功能 ==========

    @FXML
    private void updatePassword() {
        try {
            String recoveryCode = recoveryCodeField.getText().trim();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // 验证输入
            if (recoveryCode.isEmpty()) {
                showPasswordStatus("请输入安全密钥", true);
                if (recoveryCodeField != null) recoveryCodeField.requestFocus();
                return;
            }

            if (newPassword.isEmpty()) {
                showPasswordStatus("请输入新密码", true);
                if (newPasswordField != null) newPasswordField.requestFocus();
                return;
            }

            if (confirmPassword.isEmpty()) {
                showPasswordStatus("请确认新密码", true);
                if (confirmPasswordField != null) confirmPasswordField.requestFocus();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showPasswordStatus("两次输入的新密码不一致", true);
                if (confirmPasswordField != null) confirmPasswordField.requestFocus();
                return;
            }

            // 验证通过，执行密码重置
            resetPasswordOnServer(recoveryCode, newPassword);
        } catch (Exception e) {
            System.err.println("Error in updatePassword: " + e.getMessage());
            showPasswordStatus("操作失败: " + e.getMessage(), true);
        }
    }

    private void resetPasswordOnServer(String recoveryCode, String newPassword) {
        if (!checkConnection()) {
            showPasswordStatus("网络连接失败，请检查连接", true);
            return;
        }

        try {
            // 创建重置密码请求
            ResetPasswordRequest request = new ResetPasswordRequest(recoveryCode, newPassword);

            // 发送请求到服务器
            String response = socketClient.sendRequest(request);

            if (response != null) {
                JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                if (responseObj.has("success") && responseObj.get("success").getAsBoolean()) {
                    showPasswordStatus("密码重置成功", false);
                    clearPasswordFields();

                    // 密码修改成功后，显示提示并执行退出
                    Platform.runLater(() -> {
                        DialogHelper.showInfo(mainContainer.getScene().getWindow(),
                                "密码重置成功！\n系统将自动退出到登录界面。");

                        // 延迟1秒后执行退出
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000); // 等待1秒
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Platform.runLater(() -> {
                                // 执行密码重置成功后的退出流程
                                performLogoutAfterPasswordReset();
                            });
                        }).start();
                    });
                } else {
                    String errorMsg = responseObj.has("message") ?
                            responseObj.get("message").getAsString() : "密码重置失败";
                    showPasswordStatus(errorMsg, true);
                    if (recoveryCodeField != null) {
                        recoveryCodeField.clear();
                        recoveryCodeField.requestFocus();
                    }
                }
            } else {
                showPasswordStatus("服务器无响应，请稍后重试", true);
            }
        } catch (Exception e) {
            showPasswordStatus("密码重置失败: " + e.getMessage(), true);
            System.err.println("密码重置异常: " + e.getMessage());
        }
    }

    /**
     * 密码重置成功后的退出操作
     */
    private void performLogoutAfterPasswordReset() {
        System.out.println("开始执行密码重置后的退出流程...");

        try {
            // 1. 断开网络连接（协议中没有 logout_request，直接断开即可）
            if (socketClient != null) {
                try {
                    socketClient.disconnect();
                    System.out.println("网络连接已断开");
                } catch (Exception e) {
                    System.err.println("断开连接时发生异常: " + e.getMessage());
                }
            }

            // 2. 通知主窗口关闭（通过回调函数）
            if (passwordResetCallback != null) {
                System.out.println("通知主窗口关闭...");
                passwordResetCallback.accept(true);
            } else {
                System.out.println("Warning: passwordResetCallback is null");
            }

            // 3. 关闭当前设置窗口
            Platform.runLater(() -> {
                Stage stage = (Stage) mainContainer.getScene().getWindow();
                if (stage != null && stage.isShowing()) {
                    System.out.println("关闭设置窗口...");
                    stage.close();
                }

                // 4. 调用退出回调返回到登录界面
                if (logoutCallback != null) {
                    System.out.println("调用退出回调返回到登录界面...");
                    logoutCallback.run();
                } else {
                    System.out.println("Warning: logoutCallback is null");
                }
            });

        } catch (Exception e) {
            System.err.println("执行密码重置后退出时发生错误: " + e.getMessage());
            e.printStackTrace();
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
            if (recoveryCodeField != null) recoveryCodeField.clear();
            if (newPasswordField != null) newPasswordField.clear();
            if (confirmPasswordField != null) confirmPasswordField.clear();
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

    /**
     * 正常的退出登录操作
     */
    private void performNormalLogout() {
        try {
            // 发送退出登录请求到服务器
            if (socketClient != null && socketClient.isConnected()) {
                try {

                } catch (Exception e) {
                    System.err.println("退出登录请求发送失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("退出登录请求失败: " + e.getMessage());
        }

        // 断开连接
        if (socketClient != null) {
            try {
                socketClient.disconnect();
            } catch (Exception e) {
                System.err.println("断开连接时发生异常: " + e.getMessage());
            }
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

    // ========== 密钥获取方法 ==========

    /**
     * 从服务器获取用户密钥
     */
    public void loadUserSecretKey() {
        if (!checkConnection() || userId == null) {
            Platform.runLater(() -> {
                if (keyValueField != null) {
                    keyValueField.setText("未连接服务器");
                }
            });
            return;
        }

        // 显示加载状态
        Platform.runLater(() -> {
            if (keyValueField != null) {
                keyValueField.setText("加载中...");
            }
        });

        // 从服务器获取密钥
        new Thread(() -> {
            try {
                // 构建密钥请求
                JsonObject request = new JsonObject();
                request.addProperty("type", "get_secret_key_request");
                request.addProperty("userId", userId);

                String response = socketClient.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null) {
                        try {
                            JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                            if (responseObj.has("success") && responseObj.get("success").getAsBoolean()) {
                                if (responseObj.has("secretKey")) {
                                    String secretKey = responseObj.get("secretKey").getAsString();
                                    if (keyValueField != null) {
                                        keyValueField.setText(secretKey);
                                    }
                                } else {
                                    if (keyValueField != null) {
                                        keyValueField.setText("密钥未设置");
                                    }
                                }
                            } else {
                                String errorMsg = responseObj.has("message") ?
                                        responseObj.get("message").getAsString() : "获取密钥失败";
                                if (keyValueField != null) {
                                    keyValueField.setText(errorMsg);
                                }
                            }
                        } catch (Exception e) {
                            if (keyValueField != null) {
                                keyValueField.setText("解析响应失败");
                            }
                        }
                    } else {
                        if (keyValueField != null) {
                            keyValueField.setText("服务器无响应");
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (keyValueField != null) {
                        keyValueField.setText("获取失败: " + e.getMessage());
                    }
                });
                System.err.println("获取用户密钥失败: " + e.getMessage());
            }
        }).start();
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

            loadUserSecretKey();
        });
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    public void setLogoutCallback(Runnable logoutCallback) {
        this.logoutCallback = logoutCallback;
    }

    // 新增：设置密码重置回调
    public void setPasswordResetCallback(Consumer<Boolean> passwordResetCallback) {
        this.passwordResetCallback = passwordResetCallback;
    }
}