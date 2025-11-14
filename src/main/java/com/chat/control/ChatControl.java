package com.chat.control;

import com.chat.network.SocketClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 聊天窗口控制器
 */
public class ChatControl implements Initializable {

    @FXML private Label contactNameLabel;
    @FXML private ImageView contactAvatar;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;

    private String contactName;
    private String contactAvatarUrl;
    private SocketClient socketClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化聊天界面
        setupChatUI();
    }

    private void setupChatUI() {
        // 设置发送消息的快捷键（回车发送）
        messageInput.setOnAction(event -> sendMessage());
    }

    /**
     * 设置联系人信息
     */
    public void setContactInfo(String contactName, String avatarUrl, SocketClient socketClient) {
        this.contactName = contactName;
        this.contactAvatarUrl = avatarUrl;
        this.socketClient = socketClient;

        // 更新UI
        contactNameLabel.setText(contactName);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                contactAvatar.setImage(new Image(getClass().getResourceAsStream(avatarUrl)));
            } catch (Exception e) {
                // 使用默认头像
                contactAvatar.setImage(new Image(getClass().getResourceAsStream("/com/chat/images/default_avatar.png")));
            }
        }
    }

    /**
     * 发送消息
     */
    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            // 添加到聊天区域
            chatArea.appendText("我: " + message + "\n");
            messageInput.clear();

            // TODO: 通过socketClient发送消息到服务器
            if (socketClient != null && socketClient.isConnected()) {
                // 发送消息逻辑
            }
        }
    }

    /**
     * 接收消息
     */
    public void receiveMessage(String message) {
        chatArea.appendText(contactName + ": " + message + "\n");
    }
}