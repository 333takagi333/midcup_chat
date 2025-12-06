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
 * 私聊界面控制器 - 仅处理UI交互
 */
public class ChatPrivateControl implements Initializable {

    @FXML private Label contactNameLabel;
    @FXML private ImageView contactAvatar;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;

    private Long contactId;
    private String contactName;
    private String contactAvatarUrl;
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

    public void setChatInfo(String contactId, String contactName, String avatarUrl,
                            SocketClient socketClient, String userId) {
        try {
            this.contactId = Long.parseLong(contactId);
            this.contactName = contactName;
            this.contactAvatarUrl = avatarUrl;
            this.socketClient = socketClient;
            this.userId = Long.parseLong(userId);
            this.chatService = new ChatService();
        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        contactNameLabel.setText(contactName);
        AvatarHelper.loadAvatar(contactAvatar, avatarUrl, false, 40);
        loadChatHistory();
    }

    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || socketClient == null || !socketClient.isConnected()
                || contactId == null || userId == null) {
            return;
        }

        // 使用Service发送消息
        boolean sent = chatService.sendPrivateMessage(socketClient, contactId, userId, content);

        if (sent) {
            chatArea.appendText("我: " + content + "\n");
            messageInput.clear();
        } else {
            DialogUtil.showError(chatArea.getScene().getWindow(), "发送失败，请检查网络连接");
        }
    }

    private void loadChatHistory() {
        chatArea.appendText("--- 开始与 " + contactName + " 聊天 ---\n");
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
        // 消息解析逻辑可以移到Service层，这里简化为直接显示
        if (messageJson.contains("\"fromUserId\":" + contactId)) {
            // 提取消息内容
            String content = extractMessageContent(messageJson);
            if (content != null) {
                chatArea.appendText(contactName + ": " + content + "\n");
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

    public void cleanup() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
    }
}