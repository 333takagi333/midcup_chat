package com.chat.control;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 注册成功信息显示控制器
 */
public class RegistrationSuccessControl implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label instructionLabel;
    @FXML private TextField uidField;
    @FXML private TextField secretKeyField;
    @FXML private Button copyUidButton;
    @FXML private Button copyKeyButton;
    @FXML private Button copyAllButton;
    @FXML private Button confirmButton;

    private String uid;
    private String secretKey;
    private Runnable onConfirmCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();

        // 如果数据已经设置，更新界面
        if (uid != null) {
            uidField.setText(uid);
        }
        if (secretKey != null) {
            secretKeyField.setText(secretKey);
        }
    }

    /**
     * 设置注册成功信息
     */
    public void setRegistrationInfo(String uid, String secretKey) {
        this.uid = uid;
        this.secretKey = secretKey;

        // 如果UI已初始化，直接更新
        if (uidField != null) {
            uidField.setText(uid != null ? uid.toString() : "未知");
        }
        if (secretKeyField != null) {
            secretKeyField.setText(secretKey != null ? secretKey : "未知");
        }
    }

    /**
     * 设置确认回调
     */
    public void setOnConfirmCallback(Runnable callback) {
        this.onConfirmCallback = callback;
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 复制UID按钮
        copyUidButton.setOnAction(e -> {
            if (uid != null && !uid.isEmpty()) {
                copyToClipboard("用户ID", uid);
                showCopiedFeedback(copyUidButton);
            }
        });

        // 复制密钥按钮
        copyKeyButton.setOnAction(e -> {
            if (secretKey != null && !secretKey.isEmpty()) {
                copyToClipboard("登录密钥", secretKey);
                showCopiedFeedback(copyKeyButton);
            }
        });

        // 复制全部按钮
        copyAllButton.setOnAction(e -> {
            StringBuilder allText = new StringBuilder();

            if (uid != null && !uid.isEmpty()) {
                allText.append("用户ID: ").append(uid).append("\n");
            }

            if (secretKey != null && !secretKey.isEmpty()) {
                allText.append("登录密钥: ").append(secretKey);
            }

            if (allText.length() > 0) {
                copyToClipboard("全部信息", allText.toString());
                showCopiedFeedback(copyAllButton);
            }
        });

        // 确认按钮
        confirmButton.setOnAction(e -> {
            // 关闭窗口
            Stage stage = (Stage) confirmButton.getScene().getWindow();
            stage.close();

            // 执行回调
            if (onConfirmCallback != null) {
                onConfirmCallback.run();
            }
        });
    }

    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String label, String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);

        System.out.println("[Registration] 已复制 " + label + " 到剪贴板");
    }

    /**
     * 显示复制成功反馈
     */
    private void showCopiedFeedback(Button button) {
        String originalText = button.getText();
        String originalStyle = button.getStyle();

        button.setText("已复制 ✓");
        button.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");

        // 2秒后恢复原状
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    button.setText(originalText);
                    button.setStyle(originalStyle);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}