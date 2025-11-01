package com.chat.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * 对话框工具类，提供通用的信息提示功能
 */
public class DialogUtil {

    /**
     * 显示信息提示对话框
     * @param owner 父窗口
     * @param message 要显示的消息
     */
    public static void showInfo(Window owner, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogUtil.class.getResource("/com/chat/fxml/components/InfoDialog.fxml"));
            Parent dialogRoot = loader.load();

            InfoDialog controller = loader.getController();
            controller.setMessage(message);

            Stage dialogStage = new Stage();
            controller.setDialogStage(dialogStage);
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("提示");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            // 如果自定义对话框加载失败，使用系统默认的Alert
            showFallbackAlert(message);
        }
    }

    /**
     * 备用方案：使用系统Alert
     */
    private static void showFallbackAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示信息提示对话框（无父窗口版本）
     * @param message 要显示的消息
     */
    public static void showInfo(String message) {
        showFallbackAlert(message);
    }
}