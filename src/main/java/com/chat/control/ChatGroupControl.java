package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import com.chat.service.ChatSessionManager;
import com.chat.service.MessageBroadcaster;
import com.chat.service.WindowManagementService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群聊界面控制器
 */
public class ChatGroupControl implements Initializable, MessageBroadcaster.GroupMessageListener {

    @FXML private Label groupNameLabel;
    @FXML private ImageView groupAvatar;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;
    @FXML private HBox historyButtonBox;
    @FXML private Button groupDetailButton; // 群聊详情按钮

    private Button loadHistoryButton;

    private Long groupId;
    private String groupName;
    private String groupAvatarUrl;
    private SocketClient socketClient;
    private Long userId;
    private ChatService chatService;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();
    private final WindowManagementService windowService = new WindowManagementService();

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChatUI();
        createHistoryButton();
        setupGroupDetailButton();
    }

    private void setupChatUI() {
        messageInput.setOnAction(event -> sendMessage());
    }

    private void createHistoryButton() {
        loadHistoryButton = new Button("查看历史记录");
        loadHistoryButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        loadHistoryButton.setOnAction(event -> openHistoryWindow());
        historyButtonBox.getChildren().add(loadHistoryButton);
    }

    private void setupGroupDetailButton() {
        if (groupDetailButton != null) {
            groupDetailButton.setOnAction(event -> showGroupDetails());
            groupDetailButton.setTooltip(new javafx.scene.control.Tooltip("查看群聊详情"));
        }
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

            // 注册群聊消息监听器
            broadcaster.registerGroupListener(this.groupId.toString(), this);

            System.out.println("[ChatGroupControl] 设置群聊信息: " + groupName +
                    ", 群组ID: " + this.groupId + ", 监听器已注册");

        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        groupNameLabel.setText(groupName);
        AvatarHelper.loadAvatar(groupAvatar, avatarUrl, true, 50);

        // 清空聊天区域
        chatArea.clear();

        // 自动加载本次登录期间的聊天记录
        loadCurrentSessionMessages();

        System.out.println("[ChatGroupControl] 群聊窗口已打开，已自动加载本次登录记录");
    }

    /**
     * 显示群聊详情
     */
    @FXML
    private void showGroupDetails() {
        try {
            // 加载群聊详情FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/group-details.fxml"));
            VBox groupDetailsRoot = loader.load();

            // 获取控制器并设置数据
            GroupDetailsControl controller = loader.getController();
            controller.setGroupInfo(
                    groupId.toString(),
                    groupName,
                    groupAvatarUrl,
                    socketClient,
                    userId.toString()
            );

            // 创建新窗口显示群聊详情
            Stage detailsStage = new Stage();
            detailsStage.initModality(Modality.WINDOW_MODAL);
            detailsStage.initOwner(chatArea.getScene().getWindow());
            detailsStage.setTitle(groupName + " 的详情");
            detailsStage.setScene(new javafx.scene.Scene(groupDetailsRoot, 450, 550));
            detailsStage.show();

        } catch (Exception e) {
            System.err.println("[ChatGroupControl] 打开群聊详情失败: " + e.getMessage());
            DialogUtil.showError(chatArea.getScene().getWindow(), "打开群聊详情失败");
        }
    }

    /**
     * 打开历史记录窗口
     */
    private void openHistoryWindow() {
        try {
            // 加载历史记录窗口FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/ChatHistoryWindow.fxml"));
            VBox historyRoot = loader.load();

            // 获取控制器并设置数据
            ChatHistoryWindowControl controller = loader.getController();
            controller.setHistoryInfo(
                    "group",
                    groupId,
                    groupName,
                    userId,
                    socketClient
            );

            // 创建新窗口显示历史记录
            Stage historyStage = new Stage();
            historyStage.initModality(Modality.WINDOW_MODAL);
            historyStage.initOwner(chatArea.getScene().getWindow());
            historyStage.setTitle(groupName + " - 历史记录");
            historyStage.setScene(new javafx.scene.Scene(historyRoot, 600, 700));
            historyStage.show();

            System.out.println("[ChatGroupControl] 历史记录窗口已打开");

        } catch (Exception e) {
            System.err.println("[ChatGroupControl] 打开历史记录窗口失败: " + e.getMessage());
            DialogUtil.showError(chatArea.getScene().getWindow(), "打开历史记录窗口失败: " + e.getMessage());
        }
    }

    /**
     * 自动加载本次登录期间的群聊记录
     */
    private void loadCurrentSessionMessages() {
        Platform.runLater(() -> {
            // 从会话管理器获取本次登录的聊天记录
            List<String> sessionMessages = sessionManager.getGroupSession(groupId);

            if (sessionMessages == null || sessionMessages.isEmpty()) {
                // 没有本次登录的记录，只显示简单的欢迎信息
                chatArea.appendText("--- 欢迎来到 " + groupName + " ---\n\n");
            } else {
                // 有本次登录的记录，直接显示记录，不加标题
                for (String message : sessionMessages) {
                    chatArea.appendText(message + "\n");
                }
            }
        });
    }

    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || socketClient == null || !socketClient.isConnected()
                || groupId == null || userId == null) {
            return;
        }

        // 生成消息唯一标识
        long timestamp = System.currentTimeMillis();
        String messageKey = generateMessageKey(groupId, userId, content, timestamp);

        // 先清空输入框
        messageInput.clear();

        // 在本地立即显示
        String time = timeFormat.format(new Date(timestamp));
        String displayMessage = "[" + time + "] 我: " + content;

        // 标记为pending
        pendingMessages.put(messageKey, timestamp);

        // 立即显示
        chatArea.appendText(displayMessage + "\n");

        // 保存到会话管理器
        sessionManager.addGroupMessage(groupId, displayMessage);

        System.out.println("[ChatGroupControl] 本地显示群聊消息，key: " + messageKey);

        // 异步发送到服务器
        new Thread(() -> {
            boolean sent = chatService.sendGroupMessage(socketClient, groupId, userId, content);

            if (sent) {
                System.out.println("[ChatGroupControl] 群聊消息发送成功到服务器");

                // 5秒后清理pending状态
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        pendingMessages.remove(messageKey);
                        System.out.println("[ChatGroupControl] 清理pending消息: " + messageKey);
                    }
                }, 5000);

            } else {
                Platform.runLater(() -> {
                    DialogUtil.showError(chatArea.getScene().getWindow(), "发送失败，请检查网络连接");
                    pendingMessages.remove(messageKey);

                    // 标记为发送失败
                    chatArea.appendText("[发送失败] " + displayMessage + "\n");
                });
            }
        }).start();
    }

    @Override
    public void onGroupMessageReceived(Long messageGroupId, Long fromUserId, String content,
                                       long timestamp, Long messageId) {
        Platform.runLater(() -> {
            // 检查是否是当前群组的消息
            if (messageGroupId.equals(groupId)) {
                // 生成消息唯一标识
                String messageKey = generateMessageKey(messageGroupId, fromUserId, content, timestamp);

                // 去重检查
                if (processedMessageKeys.contains(messageKey)) {
                    System.out.println("[ChatGroupControl] 跳过已处理的群聊消息: " + messageKey);
                    return;
                }

                // 检查是否是刚发送的pending消息
                if (pendingMessages.containsKey(messageKey)) {
                    System.out.println("[ChatGroupControl] 这是刚发送的群聊消息回传: " + messageKey);
                    pendingMessages.remove(messageKey);
                    processedMessageKeys.add(messageKey);
                    return;
                }

                // 正常处理新消息
                String time = timeFormat.format(new Date(timestamp));
                String senderName = fromUserId.equals(userId) ? "我" : "用户" + fromUserId;
                String displayMessage = "[" + time + "] " + senderName + ": " + content;

                // 添加到已处理集合
                processedMessageKeys.add(messageKey);

                // 保存到会话管理器
                sessionManager.addGroupMessage(groupId, displayMessage);

                // 显示消息
                chatArea.appendText(displayMessage + "\n");

                System.out.println("[ChatGroupControl] 显示新群聊消息: " + displayMessage);

                // 清理旧的记录
                if (processedMessageKeys.size() > 100) {
                    Iterator<String> iterator = processedMessageKeys.iterator();
                    int count = 0;
                    while (iterator.hasNext() && count < 50) {
                        iterator.next();
                        iterator.remove();
                        count++;
                    }
                }
            } else {
                System.out.println("[ChatGroupControl] 收到非当前群组的消息: " + messageGroupId +
                        " (当前群组: " + groupId + ")");
            }
        });
    }

    /**
     * 生成群聊消息唯一标识
     */
    private String generateMessageKey(Long groupId, Long fromUserId, String content, long timestamp) {
        // 对内容取前50个字符
        String contentHash = content.length() > 50 ?
                content.substring(0, 50) + "_" + content.length() :
                content;

        // 简化时间戳（精确到秒）
        long secondTimestamp = timestamp / 1000;

        return String.format("group_%d_%d_%s_%d",
                groupId,
                fromUserId,
                contentHash,
                secondTimestamp);
    }

    public void cleanup() {
        // 移除消息监听器
        broadcaster.unregisterGroupListener(groupId.toString(), this);

        System.out.println("[ChatGroupControl] 清理完成，会话记录已保存");
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    // 提供给外部访问的方法
    public TextField getMessageInput() {
        return messageInput;
    }
}