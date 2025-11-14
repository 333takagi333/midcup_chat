package com.chat.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * 错误提示对话框控制器
 */
public class ErrorDialog {

    @FXML
    private Label messageLabel;

    private Stage dialogStage;

    /**
     * 注入对话框窗口引用，便于在控制器中关闭窗口。
     * @param dialogStage 对话框窗口
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置要展示的错误消息。
     * @param message 错误消息文本
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * 关闭对话框。
     */
    @FXML
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}