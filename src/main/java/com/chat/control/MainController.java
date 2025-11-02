package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.ChatPrivateReceive;
import com.chat.protocol.ChatPrivateSend;
import com.chat.protocol.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主界面控制器：三层布局，顶部显示当前用户名；中部显示消息列表；底部为输入栏发送消息。
 */
public class MainController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private TabPane tabPane;
    @FXML private Tab chatTab;
    @FXML private ListView<String> chatListView;
    @FXML private TextField toField;
    @FXML private TextField messageTextField;
    @FXML private Button sendButton;
    @FXML private Tab usersTab;
    @FXML private ListView<String> usersListView;

    private String currentUsername;
    private SocketClient socketClient;
    private String userId; // 登录成功后服务端返回的 UID
    private ObservableList<String> messages;

    private volatile boolean receiving = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化聊天列表的数据模型，避免因未设置 items 导致的 NPE 或不可见问题
        if (chatListView != null) {
            chatListView.setItems(FXCollections.observableArrayList());
        }
        // 输入框与按钮按回车/点击发送（与 FXML onAction 重复也没关系，双重保障）
        if (messageTextField != null) {
            messageTextField.setOnAction(this::onSend);
        }
        if (sendButton != null) {
            sendButton.setOnAction(this::onSend);
        }
    }

    /**
     * 发送按钮/回车发送处理。
     */
    @FXML
    public void onSend(ActionEvent event) {
        String to = toField != null ? toField.getText() : null;
        String content = messageTextField != null ? messageTextField.getText() : null;

        if (to == null || to.isBlank() || content == null || content.isBlank()) {
            return;
        }
        if (socketClient == null) {
            appendMessage("[系统] 未连接到服务器，无法发送。");
            return;
        }

        ChatPrivateSend payload = new ChatPrivateSend(currentUsername, to, content);
        boolean ok = socketClient.sendMessage(payload);
        if (ok) {
            appendMessage("我 -> " + to + ": " + content);
            messageTextField.clear();
        } else {
            appendMessage("[系统] 发送失败，请检查网络或重试。");
        }
    }

    /** 顶部显示用户名 */
    public void setUsername(String username) {
        this.currentUsername = username;
        if (usernameLabel != null) {
            usernameLabel.setText("当前用户: " + username);
        }
    }

    /** 注入 SocketClient 并启动接收线程。 */
    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        startReceiver();
    }

    /** 设置 UID（可用于后续会话识别、拉取会话列表等）。 */
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }

    private void startReceiver() {
        if (socketClient == null) return;
        receiving = true;
        Thread t = new Thread(() -> {
            Gson gson = new Gson();
            while (receiving && socketClient.isConnected()) {
                try {
                    String line = socketClient.receiveMessage();
                    if (line == null) {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                        continue;
                    }

                    System.out.println("Received message: " + line); // 输出接收到的消息到控制台

                    // 严格按标准外层解析：{ type, data, timestamp? }
                    JsonObject root = JsonParser.parseString(line).getAsJsonObject();
                    String type = root.has("type") ? root.get("type").getAsString() : "";
                    if (MessageType.CHAT_PRIVATE_RECEIVE.equals(type)) {
                        if (!root.has("data") || !root.get("data").isJsonObject()) {
                            // 非法消息，忽略
                            continue;
                        }
                        ChatPrivateReceive msg = gson.fromJson(root.getAsJsonObject("data"), ChatPrivateReceive.class);
                        if (msg != null) {
                            appendMessage(msg.getFrom() + " -> 我: " + msg.getContent());
                        }
                    }
                } catch (Exception e) {
                    // 忽略单次解析异常，继续循环
                }
            }
        }, "chat-receiver");
        t.setDaemon(true);
        t.start();
    }

    private void appendMessage(String text) {
        if (chatListView == null) return;
        if (Platform.isFxApplicationThread()) {
            chatListView.getItems().add(text);
            chatListView.scrollTo(chatListView.getItems().size() - 1);
        } else {
            Platform.runLater(() -> {
                chatListView.getItems().add(text);
                chatListView.scrollTo(chatListView.getItems().size() - 1);
            });
        }
    }
}