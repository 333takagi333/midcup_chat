package com.chat.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * 简单的信息提示对话框控制器，用于展示一段消息并允许用户关闭。
 */
public class InfoDialog {

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
     * 设置要展示的提示消息。
     * @param message 消息文本
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * 关闭对话框。
     */
    @FXML
    void closeDialog(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
