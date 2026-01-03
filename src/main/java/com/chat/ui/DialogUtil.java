package com.chat.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;

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
            // 尝试加载自定义对话框
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
            System.err.println("加载自定义对话框失败，使用系统默认对话框: " + e.getMessage());
            showFallbackAlert(Alert.AlertType.INFORMATION, "提示", message);
        } catch (Exception e) {
            System.err.println("对话框显示异常: " + e.getMessage());
            showFallbackAlert(Alert.AlertType.INFORMATION, "提示", message);
        }
    }

    /**
     * 显示错误提示对话框
     * @param owner 父窗口
     * @param message 要显示的错误消息
     */
    public static void showError(Window owner, String message) {
        try {
            // 尝试加载自定义错误对话框
            FXMLLoader loader = new FXMLLoader(DialogUtil.class.getResource("/com/chat/fxml/components/ErrorDialog.fxml"));
            Parent dialogRoot = loader.load();

            ErrorDialog controller = loader.getController();
            controller.setMessage(message);

            Stage dialogStage = new Stage();
            controller.setDialogStage(dialogStage);
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("错误");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            // 如果自定义对话框加载失败，使用系统默认的Alert
            showFallbackAlert(Alert.AlertType.ERROR, "错误", message);
        }
    }

    /**
     * 显示警告提示对话框
     * @param owner 父窗口
     * @param message 要显示的警告消息
     */
    public static void showWarning(Window owner, String message) {
        showFallbackAlert(Alert.AlertType.WARNING, "警告", message);
    }

    /**
     * 显示确认对话框
     * @param owner 父窗口
     * @param message 要显示的确认消息
     * @return true表示用户确认，false表示用户取消
     */
    public static boolean showConfirmation(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("确认");
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * 显示带自定义按钮的确认对话框
     * @param owner 父窗口
     * @param title 对话框标题
     * @param message 消息内容
     * @param confirmButtonText 确认按钮文本
     * @return true表示用户确认，false表示用户取消
     */
    public static boolean showCustomConfirmation(Window owner, String title, String message, String confirmButtonText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 自定义按钮文本
        ButtonType confirmButton = new ButtonType(confirmButtonText);
        ButtonType cancelButton = new ButtonType("取消", ButtonType.CANCEL.getButtonData());
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }

    /**
     * 显示输入对话框
     * @param owner 父窗口
     * @param title 对话框标题
     * @param header 对话框头部文本
     * @param content 对话框内容文本
     * @return 用户输入的内容，如果取消则返回null
     */
    public static String showInputDialog(Window owner, String title, String header, String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * 显示简单的输入对话框
     * @param owner 父窗口
     * @param message 提示消息
     * @return 用户输入的内容，如果取消则返回null
     */
    public static String showInputDialog(Window owner, String message) {
        return showInputDialog(owner, "输入", null, message);
    }

    /**
     * 显示等待对话框（需要手动关闭）
     * @param owner 父窗口
     * @param message 等待消息
     * @return 对话框Stage，用于后续关闭
     */
    public static Stage showWaitingDialog(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("请稍候");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
        return (Stage) alert.getDialogPane().getScene().getWindow();
    }

    /**
     * 显示成功提示对话框
     * @param owner 父窗口
     * @param message 成功消息
     */
    public static void showSuccess(Window owner, String message) {
        showInfo(owner, message); // 暂时使用InfoDialog显示成功消息
    }

    /**
     * 备用方案：使用系统Alert
     */
    private static void showFallbackAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== 无父窗口版本的方法 ==========

    /**
     * 显示信息提示对话框（无父窗口版本）
     * @param message 要显示的消息
     */
    public static void showInfo(String message) {
        showFallbackAlert(Alert.AlertType.INFORMATION, "提示", message);
    }

    /**
     * 显示错误提示对话框（无父窗口版本）
     * @param message 要显示的错误消息
     */
    public static void showError(String message) {
        showFallbackAlert(Alert.AlertType.ERROR, "错误", message);
    }

    /**
     * 显示成功提示对话框（无父窗口版本）
     * @param message 成功消息
     */
    public static void showSuccess(String message) {
        showFallbackAlert(Alert.AlertType.INFORMATION, "成功", message);
    }

    /**
     * 显示警告提示对话框（无父窗口版本）
     * @param message 警告消息
     */
    public static void showWarning(String message) {
        showFallbackAlert(Alert.AlertType.WARNING, "警告", message);
    }

    /**
     * 显示确认对话框（无父窗口版本）
     * @param message 确认消息
     * @return true表示用户确认，false表示用户取消
     */
    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}