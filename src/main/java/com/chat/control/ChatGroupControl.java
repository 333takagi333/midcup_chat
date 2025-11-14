package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ChatGroupSend;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 群聊界面控制器
 */
public class ChatGroupControl implements Initializable {

    @FXML private Label groupNameLabel;
    @FXML private ImageView groupAvatar;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;

    private Long groupId;  // 改为 Long 类型
    private String groupName;
    private String groupAvatarUrl;
    private SocketClient socketClient;
    private Long userId;   // 改为 Long 类型
    private Timer messageTimer;
    private Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChatUI();
        startMessageListener();
    }

    private void setupChatUI() {
        messageInput.setOnAction(event -> sendMessage());
    }

    /**
     * 设置群聊信息
     */
    public void setGroupInfo(String groupId, String groupName, String avatarUrl, SocketClient socketClient, String userId) {
        try {
            this.groupId = Long.parseLong(groupId);
            this.groupName = groupName;
            this.groupAvatarUrl = avatarUrl;
            this.socketClient = socketClient;
            this.userId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        // 更新UI
        groupNameLabel.setText(groupName);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                groupAvatar.setImage(new Image(getClass().getResourceAsStream(avatarUrl)));
            } catch (Exception e) {
                groupAvatar.setImage(new Image(getClass().getResourceAsStream("/com/chat/images/default_group.png")));
            }
        }

        loadChatHistory();
    }

    /**
     * 发送消息
     */
    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (!content.isEmpty() && socketClient != null && socketClient.isConnected() &&
                groupId != null && userId != null) {

            ChatGroupSend message = new ChatGroupSend();
            message.setGroupId(groupId);
            message.setFromUserId(userId);
            message.setContent(content);
            // contentType 使用默认的 TEXT
            // timestamp 会自动设置为当前时间

            boolean sent = socketClient.sendGroupMessage(message);
            if (sent) {
                // 添加到聊天区域（显示发送者名称）
                chatArea.appendText("我: " + content + "\n");
                messageInput.clear();
            } else {
                DialogUtil.showError(chatArea.getScene().getWindow(), "发送失败，请检查网络连接");
            }
        } else {
            if (groupId == null || userId == null) {
                DialogUtil.showError(chatArea.getScene().getWindow(), "群聊信息不完整");
            }
        }
    }

    /**
     * 发送文件消息
     */
    public void sendFileMessage(String filePath, String fileName, long fileSize) {
        if (socketClient != null && socketClient.isConnected() && groupId != null && userId != null) {
            ChatGroupSend message = new ChatGroupSend();
            message.setGroupId(groupId);
            message.setFromUserId(userId);
            message.setContentType("FILE"); // 或者使用 ContentType.FILE
            message.setFileName(fileName);
            message.setFileSize(fileSize);
            // fileUrl 可能需要先上传文件到服务器获取URL

            boolean sent = socketClient.sendGroupMessage(message);
            if (sent) {
                chatArea.appendText("我: [文件] " + fileName + "\n");
            }
        }
    }

    private void loadChatHistory() {
        chatArea.appendText("--- 欢迎来到 " + groupName + " ---\n");
        // TODO: 从服务器加载历史消息
    }

    private void startMessageListener() {
        messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (socketClient != null && socketClient.isConnected()) {
                    String message = socketClient.receiveMessage();
                    if (message != null) {
                        Platform.runLater(() -> handleReceivedMessage(message));
                    }
                }
            }
        }, 0, 100);
    }

    private void handleReceivedMessage(String messageJson) {
        try {
            // 解析群聊消息
            ChatGroupSend receivedMessage = gson.fromJson(messageJson, ChatGroupSend.class);
            if (receivedMessage != null && groupId.equals(receivedMessage.getGroupId())) {
                String senderName = getSenderName(receivedMessage.getFromUserId());
                String content = formatMessageContent(receivedMessage);
                chatArea.appendText(senderName + ": " + content + "\n");
            }
        } catch (Exception e) {
            System.err.println("解析群消息失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取发送者名称
     */
    private String getSenderName(Long senderId) {
        // TODO: 从本地缓存或服务器获取用户名称
        if (senderId.equals(userId)) {
            return "我";
        } else {
            return "用户" + senderId;
        }
    }

    /**
     * 格式化消息内容
     */
    private String formatMessageContent(ChatGroupSend message) {
        if ("FILE".equals(message.getContentType())) {
            return "[文件] " + (message.getFileName() != null ? message.getFileName() : "未知文件");
        } else {
            return message.getContent();
        }
    }

    public void cleanup() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
    }

    // Getter 方法
    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }
}