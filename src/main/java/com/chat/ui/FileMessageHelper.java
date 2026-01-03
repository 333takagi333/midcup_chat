package com.chat.ui;

import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * 文件消息助手类
 */
public class FileMessageHelper {

    /**
     * 创建私聊文件消息面板（带下载按钮）
     */
    public static HBox createPrivateFileMessageBox(String fileName, long fileSize, String fileType,
                                                   Long senderId, Long userId, Long messageId,
                                                   String time, SocketClient socketClient, Window window) {
        return createFileMessageBox(fileName, fileSize, fileType, senderId, null, userId,
                messageId, time, socketClient, window, "private");
    }

    /**
     * 创建群聊文件消息面板（带下载按钮）
     */
    public static HBox createGroupFileMessageBox(String fileName, long fileSize, String fileType,
                                                 Long senderId, Long groupId, Long userId,
                                                 Long messageId, String time, SocketClient socketClient, Window window) {
        return createFileMessageBox(fileName, fileSize, fileType, senderId, groupId, userId,
                messageId, time, socketClient, window, "group");
    }

    /**
     * 创建通用的文件消息面板
     */
    private static HBox createFileMessageBox(String fileName, long fileSize, String fileType,
                                             Long senderId, Long groupId, Long userId,
                                             Long messageId, String time, SocketClient socketClient,
                                             Window window, String chatType) {
        HBox fileMessageBox = new HBox(10);
        fileMessageBox.setPadding(new Insets(10));
        fileMessageBox.setAlignment(Pos.CENTER_LEFT);

        // 设置样式
        boolean isMyMessage = senderId.equals(userId);
        if (isMyMessage) {
            fileMessageBox.setStyle("-fx-background-color: #dcf8c6;" +
                    "-fx-background-radius: 15 15 0 15;" +
                    "-fx-border-radius: 15 15 0 15;" +
                    "-fx-border-color: #a8e6a8;" +
                    "-fx-border-width: 1;");
        } else {
            fileMessageBox.setStyle("-fx-background-color: #ffffff;" +
                    "-fx-background-radius: 15 15 15 0;" +
                    "-fx-border-radius: 15 15 15 0;" +
                    "-fx-border-color: #e0e0e0;" +
                    "-fx-border-width: 1;");
        }

        // 文件图标
        Label fileIcon = new Label(getFileIcon(fileType));
        fileIcon.setStyle("-fx-font-size: 28px;");

        // 文件信息
        VBox infoBox = new VBox(5);
        infoBox.setPrefWidth(250);

        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        nameLabel.setWrapText(true);

        Label sizeLabel = new Label(formatFileSize(fileSize));
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // 时间标签
        HBox metaBox = new HBox(10);
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        String senderText = isMyMessage ? "我" : (chatType.equals("group") ? "用户" + senderId : "好友");
        Label senderLabel = new Label(senderText);
        senderLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        metaBox.getChildren().addAll(timeLabel, senderLabel);
        infoBox.getChildren().addAll(nameLabel, sizeLabel, metaBox);

        // 下载按钮
        Button downloadBtn = createDownloadButton(fileName, fileSize, chatType,
                groupId, senderId, userId, socketClient, window);

        fileMessageBox.getChildren().addAll(fileIcon, infoBox, downloadBtn);
        return fileMessageBox;
    }

    /**
     * 创建下载按钮
     */
    private static Button createDownloadButton(String fileName, long fileSize, String chatType,
                                               Long groupId, Long senderId, Long userId,
                                               SocketClient socketClient, Window window) {
        Button downloadBtn = new Button("📥 下载");

        // 设置按钮样式
        downloadBtn.setStyle("-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 8 16; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;");
        downloadBtn.setTooltip(new Tooltip("下载文件到本地"));

        // 悬停效果
        downloadBtn.setOnMouseEntered(e -> {
            downloadBtn.setStyle("-fx-background-color: #45a049; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-padding: 8 16; " +
                    "-fx-background-radius: 5; " +
                    "-fx-cursor: hand;");
        });

        downloadBtn.setOnMouseExited(e -> {
            downloadBtn.setStyle("-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-padding: 8 16; " +
                    "-fx-background-radius: 5; " +
                    "-fx-cursor: hand;");
        });

        // 点击事件
        downloadBtn.setOnAction(event -> {
            // 显示确认对话框
            boolean confirm = DialogUtil.showConfirmation(
                    window,
                    String.format("确定要下载文件吗？\n\n文件名: %s\n文件大小: %s",
                            fileName, formatFileSize(fileSize))
            );

            if (confirm) {
                // 更新按钮状态
                downloadBtn.setText("⏳ 下载中...");
                downloadBtn.setDisable(true);

                // 创建聊天服务实例
                ChatService chatService = new ChatService();

                // 设置目标ID
                Long targetId = null;
                if (chatType.equals("private")) {
                    targetId = senderId;
                } else if (chatType.equals("group")) {
                    targetId = groupId;
                }

                // 生成文件ID（实际应该从消息中获取）
                String fileId = generateFileId(fileName, senderId, System.currentTimeMillis());

                // 开始下载
                chatService.downloadFile(
                        window,
                        socketClient,
                        userId,
                        fileId,
                        fileName,
                        chatType,
                        targetId,
                        () -> {
                            // 下载完成回调
                            downloadBtn.setText("✅ 已下载");
                            downloadBtn.setStyle("-fx-background-color: #888; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-padding: 8 16; " +
                                    "-fx-background-radius: 5;");
                            downloadBtn.setDisable(true);

                            DialogUtil.showInfo(window, "文件下载完成：" + fileName);
                        }
                );
            }
        });

        return downloadBtn;
    }

    /**
     * 获取文件图标
     */
    private static String getFileIcon(String fileType) {
        if (fileType == null) return "📎";
        switch (fileType.toLowerCase()) {
            case "image": return "🖼️";
            case "video": return "🎬";
            case "audio": return "🎵";
            case "document": return "📄";
            case "text": return "📝";
            case "archive": return "📦";
            default: return "📎";
        }
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 生成文件ID
     */
    private static String generateFileId(String fileName, Long senderId, long timestamp) {
        return "file_" + senderId + "_" + timestamp + "_" +
                fileName.hashCode() + "_" + (int)(Math.random() * 1000);
    }
}