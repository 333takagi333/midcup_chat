package com.chat.control;

import com.chat.model.Request;
import com.chat.model.ChatMessage;
import com.chat.network.SocketClient;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主界面控制器 - 消息发送测试
 */
public class MainController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private TextField targetUserField;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button testButton;
    @FXML private TextArea statusArea;

    private String currentUsername;
    private SocketClient socketClient;
    private Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageField.setOnAction(event -> sendMessage());
        appendStatus("主界面已就绪");
    }

    public void setUsername(String username) {
        this.currentUsername = username;
        usernameLabel.setText("当前用户: " + username);
        appendStatus("用户: " + username + " 已登录");
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        if (socketClient != null) {
            appendStatus("服务器连接正常");
            startResponseListener();
        }
    }

    @FXML
    private void sendMessage() {
        String targetUser = targetUserField.getText().trim();
        String messageContent = messageField.getText().trim();

        if (!validateInput(targetUser, messageContent)) return;

        // 创建聊天消息对象
        ChatMessage chatMessage = new ChatMessage(currentUsername, targetUser, messageContent);

        // 使用新的消息类型 "CHAT"
        Request request = new Request("CHAT", chatMessage);

        String jsonRequest = gson.toJson(request);

        // 显示发送的内容
        appendStatus("我 -> " + targetUser + ": " + messageContent);
        appendStatus("等待服务器响应...");

        new Thread(() -> {
            boolean success = socketClient.sendMessage(jsonRequest);
            Platform.runLater(() -> {
                if (success) {
                    // 发送成功，等待服务器响应，这里不显示成功消息
                    messageField.clear();
                } else {
                    appendStatus("✗ 消息发送失败");
                }
            });
        }).start();
    }

    @FXML
    private void testConnection() {
        boolean connected = socketClient != null && socketClient.isConnected();
        appendStatus(connected ? "✓ 连接正常" : "✗ 连接断开");
    }

    private boolean validateInput(String targetUser, String messageContent) {
        if (targetUser.isEmpty()) {
            showAlert("请输入目标用户名");
            return false;
        }
        if (messageContent.isEmpty()) {
            showAlert("请输入消息内容");
            return false;
        }
        if (socketClient == null || !socketClient.isConnected()) {
            showAlert("未连接到服务器");
            return false;
        }
        return true;
    }

    private void processServerResponse(String response) {
        try {
            // 解析服务器响应
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String type = jsonResponse.has("type") ? jsonResponse.get("type").getAsString() : "";
            String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "";

            if ("CHAT_RESPONSE".equals(type)) {
                if ("SUCCESS".equals(status)) {
                    appendStatus("✓ 服务器已确认收到消息");
                } else {
                    appendStatus("✗ 服务器处理消息失败");
                }
            } else {
                // 其他类型的响应
                appendStatus("收到回复: " + response);
            }
        } catch (Exception e) {
            appendStatus("收到回复: " + response);
        }
    }

    private void startResponseListener() {
        Thread listenerThread = new Thread(() -> {
            while (socketClient != null && socketClient.isConnected()) {
                try {
                    String response = socketClient.receiveMessage();
                    if (response != null) {
                        // 处理服务器响应
                        processServerResponse(response);
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    if (!socketClient.isConnected()) break;
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void appendStatus(String message) {
        Platform.runLater(() -> {
            statusArea.appendText(message + "\n");
            statusArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            DialogUtil.showInfo(sendButton.getScene().getWindow(), message);
        });
    }


}