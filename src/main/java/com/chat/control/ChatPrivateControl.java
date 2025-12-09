package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import com.chat.service.ChatSessionManager;
import com.chat.service.MessageBroadcaster;
import com.chat.protocol.ChatHistoryResponse;
import com.chat.protocol.ChatHistoryResponse.HistoryMessageItem;
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
 * 私聊界面控制器
 */
public class ChatPrivateControl implements Initializable, MessageBroadcaster.PrivateMessageListener, ChatService.ChatHistoryCallback {

    @FXML private Label contactNameLabel;
    @FXML private ImageView contactAvatar;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;
    @FXML private HBox historyButtonBox;
    @FXML private Button profileButton; // 好友详情按钮

    private Button loadHistoryButton;

    private Long contactId;
    private String contactName;
    private String contactAvatarUrl;
    private SocketClient socketClient;
    private Long userId;
    private ChatService chatService;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();

    private String listenerKey;

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 临时存储刚发送的消息，等待服务器确认
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();

    // 用于历史消息分页
    private Long earliestTimestamp = null;
    private boolean isLoadingHistory = false;
    // 标记是否已经加载过历史消息
    private boolean hasLoadedHistory = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChatUI();
        createHistoryButton();
        setupProfileButton();
    }

    private void setupChatUI() {
        messageInput.setOnAction(event -> sendMessage());
    }

    private void createHistoryButton() {
        loadHistoryButton = new Button("加载历史记录");
        loadHistoryButton.getStyleClass().add("history-button");
        loadHistoryButton.setOnAction(event -> loadHistoryMessages());
        historyButtonBox.getChildren().add(loadHistoryButton);
    }

    private void setupProfileButton() {
        if (profileButton != null) {
            profileButton.setOnAction(event -> showFriendProfile());
            profileButton.setTooltip(new javafx.scene.control.Tooltip("查看好友详情"));
        }
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

            // 生成监听器key
            this.listenerKey = "private_" + userId + "_" + this.contactId;

            // 注册消息监听器
            broadcaster.registerPrivateListener(listenerKey, this);

            System.out.println("[ChatPrivateControl] 设置聊天信息: " + contactName +
                    ", 监听器key: " + listenerKey);

        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        contactNameLabel.setText(contactName);
        AvatarHelper.loadAvatar(contactAvatar, avatarUrl, false, 40);

        // 清空聊天区域
        chatArea.clear();

        // 自动加载本次登录期间的聊天记录
        loadCurrentSessionMessages();

        System.out.println("[ChatPrivateControl] 聊天窗口已打开，已自动加载本次登录记录");
    }

    /**
     * 自动加载本次登录期间的聊天记录
     */
    private void loadCurrentSessionMessages() {
        Platform.runLater(() -> {
            // 从会话管理器获取本次登录的聊天记录
            List<String> sessionMessages = sessionManager.getPrivateSession(userId, contactId);

            if (sessionMessages == null || sessionMessages.isEmpty()) {
                // 没有本次登录的记录，只显示简单的欢迎信息
                chatArea.appendText("--- 开始与 " + contactName + " 聊天 ---\n\n");
            } else {
                // 有本次登录的记录，直接显示记录，不加标题
                for (String message : sessionMessages) {
                    chatArea.appendText(message + "\n");
                }
            }
        });
    }

    /**
     * 显示好友详情
     */
    @FXML
    private void showFriendProfile() {
        try {
            // 加载好友资料FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/ui/friend-profile.fxml"));
            VBox friendProfileRoot = loader.load();

            // 获取控制器并设置数据
            FriendProfileControl controller = loader.getController();
            controller.setFriendInfo(
                    contactId.toString(),
                    contactName,
                    contactAvatarUrl,
                    socketClient,
                    userId.toString()
            );

            // 创建新窗口显示好友资料
            Stage profileStage = new Stage();
            profileStage.initModality(Modality.WINDOW_MODAL);
            profileStage.initOwner(chatArea.getScene().getWindow());
            profileStage.setTitle(contactName + " 的资料");
            profileStage.setScene(new javafx.scene.Scene(friendProfileRoot, 400, 500));
            profileStage.show();

        } catch (Exception e) {
            System.err.println("[ChatPrivateControl] 打开好友资料失败: " + e.getMessage());
            DialogUtil.showError(chatArea.getScene().getWindow(), "打开好友资料失败");
        }
    }

    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || socketClient == null || !socketClient.isConnected()
                || contactId == null || userId == null) {
            return;
        }

        // 生成消息唯一标识（用于去重）
        long timestamp = System.currentTimeMillis();
        String messageKey = generateMessageKey(userId, contactId, content, timestamp);

        // 先清空输入框
        messageInput.clear();

        // 在本地立即显示（给用户即时反馈）
        String time = timeFormat.format(new Date(timestamp));
        String displayMessage = "[" + time + "] 我: " + content;

        // 标记这个消息为"已发送待确认"
        pendingMessages.put(messageKey, timestamp);

        // 立即显示
        chatArea.appendText(displayMessage + "\n");

        // 保存到会话管理器
        sessionManager.addPrivateMessage(userId, contactId, displayMessage);

        System.out.println("[ChatPrivateControl] 本地显示消息，key: " + messageKey);

        // 异步发送到服务器
        new Thread(() -> {
            boolean sent = chatService.sendPrivateMessage(socketClient, contactId, userId, content);

            if (sent) {
                System.out.println("[ChatPrivateControl] 消息发送成功到服务器");

                // 5秒后清理pending状态（假设服务器会在5秒内回传）
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        pendingMessages.remove(messageKey);
                        System.out.println("[ChatPrivateControl] 清理pending消息: " + messageKey);
                    }
                }, 5000);

            } else {
                // 发送失败
                Platform.runLater(() -> {
                    DialogUtil.showError(chatArea.getScene().getWindow(), "发送失败，请检查网络连接");

                    // 从pending中移除
                    pendingMessages.remove(messageKey);

                    // 在消息前添加失败标记
                    chatArea.appendText("[发送失败] " + displayMessage + "\n");
                });
            }
        }).start();
    }

    /**
     * 加载历史消息按钮点击事件 - 加载此前登录的所有聊天历史记录
     */
    private void loadHistoryMessages() {
        if (socketClient == null || !socketClient.isConnected() || contactId == null) {
            DialogUtil.showError(chatArea.getScene().getWindow(), "未连接到服务器");
            return;
        }

        if (isLoadingHistory) {
            System.out.println("[ChatPrivateControl] 历史消息正在加载中，请稍候");
            return;
        }

        isLoadingHistory = true;

        // 禁用按钮防止重复点击
        loadHistoryButton.setDisable(true);
        loadHistoryButton.setText("加载中...");

        System.out.println("[ChatPrivateControl] 开始加载历史聊天记录");

        // 第一次加载，不传时间戳，获取最新的消息
        chatService.loadHistoryMessages(
                socketClient,
                "private",
                contactId,
                listenerKey,
                50,  // 第一次加载50条
                null, // 不传时间戳，获取最新的消息
                this  // 回调接口
        );
    }

    @Override
    public void onHistoryLoaded(ChatHistoryResponse response, String error) {
        Platform.runLater(() -> {
            isLoadingHistory = false;

            if (error != null) {
                System.err.println("[ChatPrivateControl] 加载历史消息失败: " + error);
                DialogUtil.showError(chatArea.getScene().getWindow(), "加载历史消息失败: " + error);
                loadHistoryButton.setDisable(false);
                loadHistoryButton.setText("加载历史记录");
                return;
            }

            if (response == null || response.getMessages() == null || response.getMessages().isEmpty()) {
                System.out.println("[ChatPrivateControl] 没有历史消息");
                hasLoadedHistory = true;
                loadHistoryButton.setText("没有更多历史");
                loadHistoryButton.setDisable(true);
                return;
            }

            // 处理历史消息
            List<HistoryMessageItem> messages = response.getMessages();
            System.out.println("[ChatPrivateControl] 成功加载 " + messages.size() + " 条历史消息");

            // 保存当前文本（这是本次登录的记录）
            String currentText = chatArea.getText();

            // 清空并重新组织显示
            chatArea.clear();

            // 先显示历史消息（从旧到新）
            Long newEarliestTimestamp = null;
            List<String> historyMessages = new ArrayList<>();

            for (HistoryMessageItem item : messages) {
                String time = timeFormat.format(new Date(item.getTimestamp()));
                String senderName = item.getSenderId().equals(userId) ? "我" : contactName;
                String displayMessage = "[" + time + "] " + senderName + ": " + item.getContent();

                historyMessages.add(displayMessage);

                // 记录最早的时间戳
                if (newEarliestTimestamp == null || item.getTimestamp() < newEarliestTimestamp) {
                    newEarliestTimestamp = item.getTimestamp();
                }
            }

            // 显示历史消息，不加标题
            for (String message : historyMessages) {
                chatArea.appendText(message + "\n");
            }

            // 再显示当前会话的记录（本次登录的记录）
            if (!currentText.trim().isEmpty()) {
                chatArea.appendText(currentText);
            }

            // 更新最早时间戳
            earliestTimestamp = newEarliestTimestamp;
            hasLoadedHistory = true;

            // 检查是否需要加载更多
            if (messages.size() >= 50) {
                // 还有更多消息，启用加载更多按钮
                loadHistoryButton.setText("加载更多历史");
                loadHistoryButton.setDisable(false);
            } else {
                // 已加载所有消息
                loadHistoryButton.setText("已加载全部历史");
                loadHistoryButton.setDisable(true);
            }
        });
    }

    @Override
    public void onPrivateMessageReceived(Long fromUserId, Long toUserId, String content,
                                         long timestamp, Long messageId) {
        Platform.runLater(() -> {
            // 检查是否是当前联系人的消息
            if ((fromUserId.equals(contactId) && toUserId.equals(userId)) ||
                    (fromUserId.equals(userId) && toUserId.equals(contactId))) {

                // 生成消息唯一标识
                String messageKey = generateMessageKey(fromUserId, toUserId, content, timestamp);

                // 关键去重逻辑
                if (processedMessageKeys.contains(messageKey)) {
                    System.out.println("[ChatPrivateControl] 跳过已处理的消息: " + messageKey);
                    return;
                }

                // 检查是否是刚发送的pending消息
                if (pendingMessages.containsKey(messageKey)) {
                    System.out.println("[ChatPrivateControl] 这是刚发送的消息回传，已显示过: " + messageKey);
                    // 从pending中移除，但不再显示
                    pendingMessages.remove(messageKey);
                    processedMessageKeys.add(messageKey);
                    return;
                }

                // 正常处理新消息
                String time = timeFormat.format(new Date(timestamp));
                String senderName = fromUserId.equals(userId) ? "我" : contactName;
                String displayMessage = "[" + time + "] " + senderName + ": " + content;

                // 添加到已处理集合
                processedMessageKeys.add(messageKey);

                // 保存到会话管理器
                sessionManager.addPrivateMessage(userId, contactId, displayMessage);

                // 显示消息
                chatArea.appendText(displayMessage + "\n");

                System.out.println("[ChatPrivateControl] 显示新消息: " + displayMessage);

                // 清理旧的已处理记录（避免内存泄漏）
                if (processedMessageKeys.size() > 100) {
                    Iterator<String> iterator = processedMessageKeys.iterator();
                    int count = 0;
                    while (iterator.hasNext() && count < 50) {
                        iterator.next();
                        iterator.remove();
                        count++;
                    }
                }
            }
        });
    }

    /**
     * 生成消息唯一标识
     */
    private String generateMessageKey(Long fromUserId, Long toUserId, String content, long timestamp) {
        // 使用发送者、接收者、内容和时间戳生成key
        String contentHash = content.length() > 50 ?
                content.substring(0, 50) + "_" + content.length() :
                content;

        // 简化时间戳（精确到秒）
        long secondTimestamp = timestamp / 1000;

        return String.format("private_%d_%d_%s_%d",
                Math.min(fromUserId, toUserId),
                Math.max(fromUserId, toUserId),
                contentHash,
                secondTimestamp);
    }

    public void cleanup() {
        // 移除消息监听器
        if (listenerKey != null) {
            broadcaster.unregisterPrivateListener(listenerKey, this);
        }

        System.out.println("[ChatPrivateControl] 清理完成，会话记录已保存");
    }

    // 提供给外部访问的方法
    public TextField getMessageInput() {
        return messageInput;
    }

    public Long getContactId() {
        return contactId;
    }

    public Long getUserId() {
        return userId;
    }
}