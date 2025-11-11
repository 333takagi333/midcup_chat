package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.MessageType;
import com.chat.protocol.ChatPrivateSend;
import com.chat.protocol.ContentType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主界面控制器 - 支持消息发送和接收
 */
public class MainController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private TextField targetUserField;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button testButton;
    @FXML private TextArea statusArea;

    private String currentUsername;
    private String userId;
    private SocketClient socketClient;
    private volatile boolean receiving = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageField.setOnAction(event -> sendMessage());
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * 发送消息（服务器期待字段：type, fromUserId, toUserId, content, contentType, timestamp）
     */
    @FXML
    private void sendMessage() {
        String targetUser = targetUserField.getText().trim();
        String messageContent = messageField.getText().trim();

        if (!validateInput(targetUser, messageContent)) return;

        Long fromId;
        Long toId;
        try {
            fromId = Long.parseLong(userId);
            toId = Long.parseLong(targetUser);
        } catch (NumberFormatException e) {
            showAlert("用户ID必须为数字 (from/to)");
            return;
        }

        // 使用协议类，Gson 会生成所需 JSON 字段
        ChatPrivateSend payload = new ChatPrivateSend(fromId, toId, messageContent, ContentType.TEXT);
        boolean success = socketClient.sendMessage(payload);

        if (success) {
            appendStatus("我(" + fromId + ") -> " + toId + ": " + messageContent);
            messageField.clear();
        } else {
            appendStatus("✗ 消息发送失败");
        }
    }

    /**
     * 测试连接状态
     */
    @FXML
    private void testConnection() {
        boolean connected = socketClient != null && socketClient.isConnected();
        appendStatus(connected ? "✓ 连接正常" : "✗ 连接断开");
    }

    private boolean validateInput(String targetUser, String messageContent) {
        if (targetUser.isEmpty()) {
            showAlert("请输入目标用户ID");
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
        if (userId == null || userId.isBlank()) {
            showAlert("当前用户ID缺失，请重新登录");
            return false;
        }
        return true;
    }

    public void setUsername(String username) {
        this.currentUsername = username;
        usernameLabel.setText("当前用户: " + username);
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        if (socketClient != null) {
            startReceiver();
        }
    }

    /**
     * 启动消息接收线程
     */
    private void startReceiver() {
        if (socketClient == null) return;
        receiving = true;

        Thread receiverThread = new Thread(() -> {
            while (receiving && socketClient.isConnected()) {
                try {
                    String line = socketClient.receiveMessage();
                    if (line == null) {
                        Thread.sleep(50);
                        continue;
                    }
                    // 控制台输出原始消息
                    System.out.println("Received raw message: " + line);
                    processServerMessage(line);
                } catch (Exception e) {
                    if (!socketClient.isConnected()) break;
                }
            }
        }, "message-receiver");

        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * 处理从服务器接收到的消息
     */
    private void processServerMessage(String message) {
        try {
            JsonObject root = JsonParser.parseString(message).getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";

            Platform.runLater(() -> {
                try {
                    if (MessageType.CHAT_PRIVATE_RECEIVE.equals(type)) {
                        processPrivateMessage(root);
                    } else {
                        appendStatus("[其他] " + message);
                    }
                } catch (Exception e) {
                    appendStatus("[解析错误] " + message);
                }
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                appendStatus("[格式错误] " + message);
            });
        }
    }

    /**
     * 处理私聊消息：优先使用 fromUserId 字段
     */
    private void processPrivateMessage(JsonObject root) {
        try {
            String fromDisplay;
            if (root.has("fromUserId")) {
                fromDisplay = String.valueOf(root.get("fromUserId").getAsLong());
            } else if (root.has("from")) {
                fromDisplay = root.get("from").getAsString();
            } else {
                fromDisplay = "未知";
            }
            String content = root.has("content") ? root.get("content").getAsString() : "";
            appendStatus(fromDisplay + " -> 我: " + content);
        } catch (Exception e) {
            appendStatus("[消息错误]");
        }
    }

    /**
     * 在状态区域添加消息
     */
    private void appendStatus(String message) {
        Platform.runLater(() -> {
            statusArea.appendText(message + "\n");
            statusArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * 显示提示对话框
     */
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void cleanup() {
        receiving = false;
        if (socketClient != null) {
            socketClient.disconnect();
        }
    }
}
