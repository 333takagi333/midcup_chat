package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ChatGroupSend;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

    private Long groupId;
    private String groupName;
    private String groupAvatarUrl;
    private SocketClient socketClient;
    private Long userId;
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
        // 使用AvatarHelper加载头像
        AvatarHelper.loadAvatar(groupAvatar, avatarUrl, true, 50);

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

            boolean sent = socketClient.sendGroupMessage(message);
            if (sent) {
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
            message.setContentType("FILE");
            message.setFileName(fileName);
            message.setFileSize(fileSize);

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

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }
}