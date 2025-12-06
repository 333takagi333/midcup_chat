package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
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
 * 群聊界面控制器 - 仅处理UI交互
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
    private ChatService chatService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChatUI();
        startMessageListener();
    }

    private void setupChatUI() {
        messageInput.setOnAction(event -> sendMessage());
    }

    public void setGroupInfo(String groupId, String groupName, String avatarUrl,
                             SocketClient socketClient, String userId) {
        try {
            this.groupId = Long.parseLong(groupId);
            this.groupName = groupName;
            this.groupAvatarUrl = avatarUrl;
            this.socketClient = socketClient;
            this.userId = Long.parseLong(userId);
            this.chatService = new ChatService();
        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        groupNameLabel.setText(groupName);
        AvatarHelper.loadAvatar(groupAvatar, avatarUrl, true, 50);
        loadChatHistory();
    }

    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || socketClient == null || !socketClient.isConnected()
                || groupId == null || userId == null) {
            return;
        }

        // 使用Service发送消息
        boolean sent = chatService.sendGroupMessage(socketClient, groupId, userId, content);

        if (sent) {
            chatArea.appendText("我: " + content + "\n");
            messageInput.clear();
        } else {
            DialogUtil.showError(chatArea.getScene().getWindow(), "发送失败，请检查网络连接");
        }
    }

    private void loadChatHistory() {
        chatArea.appendText("--- 欢迎来到 " + groupName + " ---\n");
    }

    private void startMessageListener() {
        messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (socketClient != null && socketClient.isConnected()) {
                    String message = chatService.receiveMessage(socketClient);
                    if (message != null) {
                        Platform.runLater(() -> handleReceivedMessage(message));
                    }
                }
            }
        }, 0, 100);
    }

    private void handleReceivedMessage(String messageJson) {
        // 简化的消息处理，实际可以移到Service层
        if (messageJson.contains("\"groupId\":" + groupId)) {
            String content = extractMessageContent(messageJson);
            if (content != null) {
                String sender = extractSender(messageJson);
                chatArea.appendText(sender + ": " + content + "\n");
            }
        }
    }

    private String extractMessageContent(String json) {
        try {
            int start = json.indexOf("\"content\":\"") + 10;
            int end = json.indexOf("\"", start);
            if (start > 10 && end > start) {
                return json.substring(start, end);
            }
        } catch (Exception e) {
            System.err.println("提取消息内容失败: " + e.getMessage());
        }
        return null;
    }

    private String extractSender(String json) {
        try {
            int start = json.indexOf("\"fromUserId\":") + 13;
            int end = json.indexOf(",", start);
            if (start > 13 && end > start) {
                String senderId = json.substring(start, end).trim();
                if (senderId.equals(userId.toString())) {
                    return "我";
                } else {
                    return "用户" + senderId;
                }
            }
        } catch (Exception e) {
            System.err.println("提取发送者失败: " + e.getMessage());
        }
        return "未知用户";
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