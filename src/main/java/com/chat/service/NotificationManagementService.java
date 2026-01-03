package com.chat.service;

import javafx.scene.control.Alert;
import javafx.stage.Window;

/**
 * 通知管理服务
 */
public class NotificationManagementService {

    /**
     * 更新通知按钮显示
     */
    public static String getNotificationButtonText(int notificationCount) {
        if (notificationCount > 0) {
            return "通知(" + notificationCount + ")";
        } else {
            return "通知";
        }
    }

    /**
     * 获取通知按钮样式
     */
    public static String getNotificationButtonStyle(int notificationCount) {
        if (notificationCount > 0) {
            return "-fx-background-color: #ff4444; -fx-text-fill: white;";
        } else {
            return "-fx-background-color: #4CAF50; -fx-text-fill: white;";
        }
    }

    /**
     * 显示桌面通知
     */
    public static void showDesktopNotification(Window owner, String title, String message) {
        try {
            Alert notification = new Alert(Alert.AlertType.INFORMATION);
            notification.setTitle(title);
            notification.setHeaderText(null);
            notification.setContentText(message);
            notification.initOwner(owner);

            notification.show();

            // 自动关闭通知
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                javafx.application.Platform.runLater(() -> {
                    if (notification.isShowing()) {
                        notification.close();
                    }
                });
            }).start();

        } catch (Exception e) {
            System.err.println("显示桌面通知失败: " + e.getMessage());
        }
    }

    /**
     * 获取通知中心窗口标题
     */
    public static String getNotificationWindowTitle(int notificationCount) {
        return "通知中心" + (notificationCount > 0 ? " (" + notificationCount + ")" : "");
    }
}