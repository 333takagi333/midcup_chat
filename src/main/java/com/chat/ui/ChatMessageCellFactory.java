package com.chat.ui;

import com.chat.model.ChatMessageModel;
import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * 聊天消息单元格工厂（支持文件下载）
 */
public class ChatMessageCellFactory extends ListCell<ChatMessageModel> {

    private final SocketClient socketClient;
    private final Long currentUserId;
    private final Window window;
    private final ChatService chatService;
    private final String chatType;
    private final Long targetId;

    public ChatMessageCellFactory(SocketClient socketClient, Long currentUserId,
                                  Window window, ChatService chatService,
                                  String chatType, Long targetId) {
        this.socketClient = socketClient;
        this.currentUserId = currentUserId;
        this.window = window;
        this.chatService = chatService;
        this.chatType = chatType;
        this.targetId = targetId;
    }

    @Override
    protected void updateItem(ChatMessageModel message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (message.getType() == ChatMessageModel.MessageType.TEXT) {
                setGraphic(createTextMessageCell(message));
            } else {
                setGraphic(createFileMessageCell(message));
            }
            setText(null);
        }
    }

    /**
     * 创建文本消息单元格
     */
    private HBox createTextMessageCell(ChatMessageModel message) {
        HBox container = new HBox();
        container.setPadding(new Insets(5, 10, 5, 10));

        // 消息对齐方式
        if (message.isMyMessage()) {
            container.setAlignment(Pos.CENTER_RIGHT);
        } else {
            container.setAlignment(Pos.CENTER_LEFT);
        }

        VBox messageBox = new VBox(3);
        messageBox.setPadding(new Insets(8, 12, 8, 12));
        messageBox.setMaxWidth(400);

        // 设置消息气泡样式
        if (message.isMyMessage()) {
            messageBox.setStyle("-fx-background-color: #dcf8c6;" +
                    "-fx-background-radius: 15 15 0 15;" +
                    "-fx-border-radius: 15 15 0 15;");
        } else {
            messageBox.setStyle("-fx-background-color: #ffffff;" +
                    "-fx-background-radius: 15 15 15 0;" +
                    "-fx-border-radius: 15 15 15 0;" +
                    "-fx-border-color: #e0e0e0;" +
                    "-fx-border-width: 1;");
        }

        // 发送者和时间
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label senderLabel = new Label(message.getSenderName());
        senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label timeLabel = new Label(message.getFormattedTime());
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        headerBox.getChildren().addAll(senderLabel, timeLabel);

        // 消息内容
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-padding: 3 0 0 0;");
        contentLabel.setMaxWidth(380);

        messageBox.getChildren().addAll(headerBox, contentLabel);
        container.getChildren().add(messageBox);

        return container;
    }

    /**
     * 创建文件消息单元格（带下载按钮）
     */
    private HBox createFileMessageCell(ChatMessageModel message) {
        HBox container = new HBox();
        container.setPadding(new Insets(5, 10, 5, 10));

        // 消息对齐方式
        if (message.isMyMessage()) {
            container.setAlignment(Pos.CENTER_RIGHT);
        } else {
            container.setAlignment(Pos.CENTER_LEFT);
        }

        HBox fileMessageBox = new HBox(10);
        fileMessageBox.setPadding(new Insets(12, 15, 12, 15));
        fileMessageBox.setMaxWidth(450);
        fileMessageBox.setAlignment(Pos.CENTER_LEFT);

        // 设置文件消息样式
        if (message.isMyMessage()) {
            fileMessageBox.setStyle("-fx-background-color: #dcf8c6;" +
                    "-fx-background-radius: 15 15 0 15;" +
                    "-fx-border-radius: 15 15 0 15;");
        } else {
            fileMessageBox.setStyle("-fx-background-color: #ffffff;" +
                    "-fx-background-radius: 15 15 15 0;" +
                    "-fx-border-radius: 15 15 15 0;" +
                    "-fx-border-color: #e0e0e0;" +
                    "-fx-border-width: 1;");
        }

        // 文件图标区域
        VBox iconBox = new VBox(2);
        iconBox.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(message.getFileIcon());
        iconLabel.setStyle("-fx-font-size: 28px;");

        Label typeLabel = new Label(message.getFileTypeDescription());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        iconBox.getChildren().addAll(iconLabel, typeLabel);

        // 文件信息区域
        VBox infoBox = new VBox(5);
        infoBox.setPrefWidth(250);

        Label fileNameLabel = new Label(message.getFileName());
        fileNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        fileNameLabel.setWrapText(true);
        fileNameLabel.setMaxWidth(250);

        Label sizeLabel = new Label(message.getFormattedFileSize());
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        HBox metaBox = new HBox(10);
        Label timeLabel = new Label(message.getFormattedTime());
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        Label senderLabel = new Label(message.getSenderName());
        senderLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        metaBox.getChildren().addAll(timeLabel, senderLabel);

        infoBox.getChildren().addAll(fileNameLabel, sizeLabel, metaBox);

        // 下载按钮区域
        VBox buttonBox = new VBox();
        buttonBox.setAlignment(Pos.CENTER);

        // 创建下载按钮（对所有文件消息都显示，但自己发送的文件可以重新下载）
        Button downloadBtn = createDownloadButton(message);

        buttonBox.getChildren().add(downloadBtn);
        buttonBox.setPadding(new Insets(0, 0, 0, 10));

        // 组装文件消息
        fileMessageBox.getChildren().addAll(iconBox, infoBox, buttonBox);
        container.getChildren().add(fileMessageBox);

        return container;
    }

    /**
     * 创建下载按钮
     */
    private Button createDownloadButton(ChatMessageModel message) {
        Button downloadBtn = new Button("📥 下载");

        // 设置按钮样式
        downloadBtn.setStyle("-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 6 12; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;");
        downloadBtn.setTooltip(new Tooltip("下载文件到本地"));

        // 设置按钮悬停效果
        downloadBtn.setOnMouseEntered(e -> {
            downloadBtn.setStyle("-fx-background-color: #45a049; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-padding: 6 12; " +
                    "-fx-background-radius: 5; " +
                    "-fx-cursor: hand;");
        });

        downloadBtn.setOnMouseExited(e -> {
            downloadBtn.setStyle("-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-padding: 6 12; " +
                    "-fx-background-radius: 5; " +
                    "-fx-cursor: hand;");
        });

        // 按钮点击事件
        downloadBtn.setOnAction(event -> {
            if (message.getFileId() == null || message.getFileId().isEmpty()) {
                DialogUtil.showError(window, "无法下载文件：文件ID无效");
                return;
            }

            // 显示确认对话框
            boolean confirm = DialogUtil.showConfirmation(
                    window,
                    String.format("确定要下载文件吗？\n\n文件名: %s\n文件大小: %s",
                            message.getFileName(), message.getFormattedFileSize())
            );

            if (confirm) {
                // 更新按钮状态
                downloadBtn.setText("⏳ 下载中...");
                downloadBtn.setDisable(true);

                // 开始下载
                chatService.downloadFile(
                        window,
                        socketClient,
                        currentUserId,
                        message.getFileId(),
                        message.getFileName(),
                        chatType,
                        targetId,
                        () -> {
                            // 下载完成后的回调
                            Platform.runLater(() -> {
                                downloadBtn.setText("✅ 已下载");
                                downloadBtn.setStyle("-fx-background-color: #888; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-padding: 6 12; " +
                                        "-fx-background-radius: 5;");
                                downloadBtn.setDisable(true);

                                DialogUtil.showInfo(window, "文件下载完成：" + message.getFileName());
                            });
                        }
                );
            }
        });

        return downloadBtn;
    }
}