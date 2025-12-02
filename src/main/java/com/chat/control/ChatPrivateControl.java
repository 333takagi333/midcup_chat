package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ChatPrivateSend;
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
 * 私聊界面控制器
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
     * 设置聊天信息
     */
    public void setChatInfo(String contactId, String contactName, String avatarUrl, SocketClient socketClient, String userId) {
        try {
            this.contactId = Long.parseLong(contactId);
            this.contactName = contactName;
            this.contactAvatarUrl = avatarUrl;
            this.socketClient = socketClient;
            this.userId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        // 更新UI
        contactNameLabel.setText(contactName);
        // 使用AvatarHelper加载头像
        AvatarHelper.loadAvatar(contactAvatar, avatarUrl, false, 40);

        loadChatHistory();
    }

    /**
     * 发送消息
     */
    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (!content.isEmpty() && socketClient != null && socketClient.isConnected() &&
                contactId != null && userId != null) {

            ChatPrivateSend message = new ChatPrivateSend();
            message.setToUserId(contactId);
            message.setFromUserId(userId);
            message.setContent(content);

            boolean sent = socketClient.sendPrivateMessage(message);
            if (sent) {
                chatArea.appendText("我: " + content + "\n");
                messageInput.clear();
            } else {
                DialogUtil.showError(chatArea.getScene().getWindow(), "发送失败，请检查网络连接");
            }
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
            ChatPrivateSend receivedMessage = gson.fromJson(messageJson, ChatPrivateSend.class);
            if (receivedMessage != null && contactId.equals(receivedMessage.getFromUserId())) {
                chatArea.appendText(contactName + ": " + receivedMessage.getContent() + "\n");
            }
        } catch (Exception e) {
            System.err.println("解析私聊消息失败: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
    }
}