package com.chat.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * 确认对话框控制器，提供“确定 / 取消”的确认交互。
 */
public class ConfirmDialog {

    @FXML
    private Label messageLabel;

    private Stage dialogStage;
    private boolean okClicked = false;

    /**
     * 注入对话框窗口引用。
     * @param dialogStage 对话框窗口
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置要显示的消息文本。
     * @param message 消息
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * 是否点击了“确定”。
     * @return true 表示用户点击了确定
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 处理“确定”按钮。
     */
    @FXML
    void handleOk(ActionEvent event) {
        okClicked = true;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * 处理“取消”按钮。
     */
    @FXML
    void handleCancel(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
