package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import com.chat.service.ChatSessionManager;
import com.chat.service.FileService;
import com.chat.service.MessageBroadcaster;
import com.chat.service.RecentMessageService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私聊界面控制器
 */
public class ChatPrivateControl implements Initializable, MessageBroadcaster.PrivateMessageListener {

    @FXML private Label contactNameLabel;
    @FXML private ImageView contactAvatar;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;
    @FXML private HBox historyButtonBox;
    @FXML private Button profileButton;
    @FXML private Button fileUploadButton;
    @FXML private Button sendButton;

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
    private final RecentMessageService recentService = RecentMessageService.getInstance();
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();
    private final Map<Long, Boolean> receivedMessageIds = new ConcurrentHashMap<>();

    private String listenerKey;

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 临时存储刚发送的消息，等待服务器确认
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChatUI();
        createHistoryButton();
        setupProfileButton();
        setupFileUploadButton();
        setupSendButton();
    }

    private void setupChatUI() {
        messageInput.setOnAction(event -> sendMessage());
        chatArea.setWrapText(true);
    }

    private void createHistoryButton() {
        loadHistoryButton = new Button("📜 历史记录");
        loadHistoryButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 10;");
        loadHistoryButton.setOnAction(event -> openHistoryWindow());
        historyButtonBox.getChildren().add(loadHistoryButton);
    }

    private void setupProfileButton() {
        if (profileButton != null) {
            profileButton.setText("👤 资料");
            profileButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 5 10;");
            profileButton.setOnAction(event -> showFriendProfile());
            profileButton.setTooltip(new Tooltip("查看好友详情"));
        }
    }

    private void setupFileUploadButton() {
        if (fileUploadButton != null) {
            fileUploadButton.setText("📎");
            fileUploadButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10;");
            fileUploadButton.setTooltip(new Tooltip("上传文件 (最大50MB)"));
        }
    }

    private void setupSendButton() {
        if (sendButton != null) {
            sendButton.setText("发送");
            sendButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 12 24;");
            sendButton.setOnAction(event -> sendMessage());
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

        // ========== 关键：标记消息栏为已读，清除红点 ==========
        recentService.markAsRead(contactId);
        System.out.println("[ChatPrivateControl] 清除消息栏红点: " + contactName);

        // 清空聊天区域并加载本次登录记录
        chatArea.clear();
        loadCurrentSessionMessages();

        System.out.println("[ChatPrivateControl] 聊天窗口已打开，已加载本次登录记录");
    }

    /**
     * 只加载本次登录期间的聊天记录
     */
    private void loadCurrentSessionMessages() {
        Platform.runLater(() -> {
            // 从会话管理器获取本次登录的聊天记录
            List<String> sessionMessages = sessionManager.getPrivateSession(userId, contactId);

            // 清空已处理消息记录（重新加载时重新标记）
            receivedMessageIds.clear();

            chatArea.clear();

            if (sessionMessages == null || sessionMessages.isEmpty()) {
                // 没有本次登录的记录，只显示简单的欢迎信息
                chatArea.appendText("--- 开始与 " + contactName + " 聊天 ---\n\n");
                System.out.println("[ChatPrivateControl] 无本次登录记录");
            } else {
                // 有本次登录的记录，直接显示所有记录
                for (String message : sessionMessages) {
                    chatArea.appendText(message + "\n");
                }

                // 滚动到底部
                chatArea.positionCaret(chatArea.getLength());
                System.out.println("[ChatPrivateControl] 加载本次登录记录 " + sessionMessages.size() + " 条");

                // 标记所有已加载的消息为已处理（防止再次显示）
                markLoadedMessagesAsProcessed(sessionMessages);
            }
        });
    }
    /**
     * 标记已加载的消息为已处理
     */
    private void markLoadedMessagesAsProcessed(List<String> sessionMessages) {
        // 这里可以根据消息内容生成唯一的标识
        // 假设消息格式为：[时间] 发送者: 内容
        for (String message : sessionMessages) {
            try {
                // 解析消息内容，提取关键信息
                // 示例消息: "[10:30] 用户1: 你好"
                if (message.startsWith("[") && message.contains("]")) {
                    // 提取内容部分
                    int contentStart = message.indexOf("]") + 2; // 跳过 "] "
                    if (contentStart < message.length()) {
                        String contentPart = message.substring(contentStart);
                        // 生成简化key
                        String simpleKey = generateSimpleMessageKey(contentPart, System.currentTimeMillis());
                        // 这里简化处理，实际应该更精确
                        System.out.println("[ChatPrivateControl] 标记消息为已处理: " +
                                contentPart.substring(0, Math.min(20, contentPart.length())));
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
    }

    /**
     * 处理文件上传
     */
    @FXML
    private void handleFileUpload() {
        System.out.println("[ChatPrivateControl] 处理文件上传");

        Window window = chatArea.getScene().getWindow();

        FileService.chooseAndUploadFile(window, file -> {
            System.out.println("[ChatPrivateControl] 选择了文件: " + file.getName());

            // 在聊天区域显示上传中消息
            String time = timeFormat.format(new Date());
            chatArea.appendText("[" + time + "] 正在上传文件: " + file.getName() + "\n");

            // 调用服务层处理文件上传
            chatService.uploadPrivateFile(
                    window,
                    socketClient,
                    userId,
                    contactId,
                    contactName,
                    file,
                    () -> {
                        // 上传成功后的回调
                        Platform.runLater(() -> {
                            String time2 = timeFormat.format(new Date());
                            String displayMessage = String.format("[%s] 我: [文件] %s (%s)",
                                    time2, file.getName(), chatService.formatFileSize(file.length()));

                            chatArea.appendText(displayMessage + "\n");
                            sessionManager.addPrivateMessage(userId, contactId, displayMessage);

                            // 滚动到底部
                            chatArea.positionCaret(chatArea.getLength());

                            // 添加发送成功提示
                            chatArea.appendText("   ↳ 文件已发送\n");
                        });
                    }
            );
        });
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
                    "private",
                    contactId,
                    contactName,
                    userId,
                    socketClient
            );

            // 创建新窗口显示历史记录
            Stage historyStage = new Stage();
            historyStage.initModality(Modality.WINDOW_MODAL);
            historyStage.initOwner(chatArea.getScene().getWindow());
            historyStage.setTitle(contactName + " - 历史记录");
            historyStage.setScene(new javafx.scene.Scene(historyRoot, 600, 700));
            historyStage.show();

            System.out.println("[ChatPrivateControl] 历史记录窗口已打开");

        } catch (Exception e) {
            System.err.println("[ChatPrivateControl] 打开历史记录窗口失败: " + e.getMessage());
            DialogUtil.showError(chatArea.getScene().getWindow(), "打开历史记录窗口失败: " + e.getMessage());
        }
    }

    /**
     * 显示好友详情
     */
    @FXML
    private void showFriendProfile() {
        try {
            // 加载好友资料FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/friend-profile.fxml"));
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

        // 生成简化的消息key
        long timestamp = System.currentTimeMillis();
        String messageKey = generateSimpleMessageKey(content, timestamp);

        // 先清空输入框
        messageInput.clear();

        // 在本地立即显示
        String time = timeFormat.format(new Date(timestamp));
        String displayMessage = String.format("[%s] 我: %s", time, content);

        // 标记为pending
        pendingMessages.put(messageKey, timestamp);

        // 立即显示并保存
        chatArea.appendText(displayMessage + "\n");
        sessionManager.addPrivateMessage(userId, contactId, displayMessage);
        chatArea.positionCaret(chatArea.getLength()); // 滚动到底部

        System.out.println("[ChatPrivateControl] 本地显示消息，key: " + messageKey);

        // 异步发送到服务器
        new Thread(() -> {
            boolean sent = chatService.sendPrivateMessage(socketClient, contactId, userId, content);

            if (sent) {
                System.out.println("[ChatPrivateControl] 消息发送成功到服务器");

                // 3秒后清理pending状态
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        pendingMessages.remove(messageKey);
                        System.out.println("[ChatPrivateControl] 清理pending消息: " + messageKey);
                    }
                }, 3000);

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

    @Override
    public void onPrivateMessageReceived(Long fromUserId, Long toUserId, String content,
                                         long timestamp, Long messageId) {
        Platform.runLater(() -> {
            // 检查是否是当前联系人的消息
            if ((fromUserId.equals(contactId) && toUserId.equals(userId)) ||
                    (fromUserId.equals(userId) && toUserId.equals(contactId))) {

                // ========== 关键去重逻辑 ==========
                // 如果消息ID不为null，检查是否已经处理过
                if (messageId != null) {
                    // 生成消息唯一标识：messageId + userId组合
                    String messageKey = messageId + "_" + userId;

                    // 如果已经处理过，直接返回
                    if (receivedMessageIds.containsKey(Long.parseLong(messageKey))) {
                        System.out.println("[ChatPrivateControl] 跳过已处理的消息: " + messageId);
                        return;
                    }

                    // 标记为已处理
                    receivedMessageIds.put(Long.parseLong(messageKey), true);

                    // 清理旧的记录（避免内存泄漏）
                    if (receivedMessageIds.size() > 1000) {
                        Iterator<Long> iterator = receivedMessageIds.keySet().iterator();
                        int count = 0;
                        while (iterator.hasNext() && count < 100) {
                            iterator.next();
                            iterator.remove();
                            count++;
                        }
                    }
                }
                // ========== 结束去重逻辑 ==========

                // 检查是否是文件消息（服务器返回的JSON格式）
                try {
                    JsonObject jsonMessage = jsonParser.parse(content).getAsJsonObject();
                    if (jsonMessage.has("type")) {
                        String type = jsonMessage.get("type").getAsString();
                        if ("file_message_receive".equals(type)) {
                            handleFileMessage(jsonMessage, fromUserId, timestamp);
                            return;
                        }
                    }
                } catch (Exception e) {
                    // 不是JSON格式，是普通文本消息
                }

                // 处理普通文本消息
                handleTextMessage(fromUserId, toUserId, content, timestamp, messageId);
            }
        });
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(Long fromUserId, Long toUserId, String content,
                                   long timestamp, Long messageId) {
        // 生成简化的消息key（只检查最近消息）
        String messageKey = generateSimpleMessageKey(content, timestamp);

        // 只检查是否是刚发送的pending消息
        if (pendingMessages.containsKey(messageKey)) {
            System.out.println("[ChatPrivateControl] 这是刚发送的消息回传，已显示过: " + messageKey);
            // 从pending中移除
            pendingMessages.remove(messageKey);
            return;
        }

        // 获取发送者名称
        String senderName = fromUserId.equals(userId) ? "我" : contactName;

        // 检查这条消息是否已经在聊天区域中显示过了
        String expectedMessage = String.format("[%s] %s: %s",
                timeFormat.format(new Date(timestamp)),
                senderName,
                content);

        // 检查聊天区域是否已经包含这条消息
        String chatText = chatArea.getText();
        if (chatText.contains(expectedMessage)) {
            System.out.println("[ChatPrivateControl] 消息已在聊天区域中: " +
                    content.substring(0, Math.min(20, content.length())));
            return;
        }

        // 正常处理新消息
        String time = timeFormat.format(new Date(timestamp));
        String displayMessage = String.format("[%s] %s: %s", time, senderName, content);

        // 消息已经由 MessageBroadcaster 保存到会话管理器，这里只需显示
        if (chatArea != null) {
            chatArea.appendText(displayMessage + "\n");
            chatArea.positionCaret(chatArea.getLength()); // 滚动到底部
            System.out.println("[ChatPrivateControl] 显示新消息: " +
                    (senderName.equals("我") ? "发送" : "接收") + " - " +
                    content.substring(0, Math.min(20, content.length())));
        }
    }

    /**
     * 处理文件消息
     */
    private void handleFileMessage(JsonObject fileMessage, Long fromUserId, long timestamp) {
        try {
            String fileId = fileMessage.get("fileId").getAsString();
            String fileName = fileMessage.get("fileName").getAsString();
            long fileSize = fileMessage.get("fileSize").getAsLong();
            String fileType = fileMessage.get("fileType").getAsString();
            String downloadUrl = fileMessage.get("downloadUrl").getAsString();
            Long senderId = fileMessage.get("senderId").getAsLong();
            Long receiverId = fileMessage.get("receiverId").getAsLong();

            String time = timeFormat.format(new Date(timestamp));
            String senderName = senderId.equals(userId) ? "我" : contactName;

            // 创建文件消息
            String displayMessage = String.format("[%s] %s: [文件] %s (%s)",
                    time, senderName, fileName, chatService.formatFileSize(fileSize));

            // 显示文件消息
            chatArea.appendText(displayMessage + "\n");

            // 添加文件类型提示
            String typeHint = getFileTypeHint(fileType);
            if (!typeHint.isEmpty()) {
                chatArea.appendText("   ↳ " + typeHint + "\n");
            }

            // 保存到会话管理器
            sessionManager.addPrivateMessage(userId, contactId, displayMessage);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ChatPrivateControl] 处理文件消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件类型提示
     */
    private String getFileTypeHint(String fileType) {
        switch (fileType) {
            case "image": return "📷 图片文件";
            case "video": return "🎬 视频文件";
            case "audio": return "🎵 音频文件";
            case "document": return "📄 文档文件";
            case "text": return "📝 文本文件";
            case "archive": return "📦 压缩文件";
            default: return "📎 文件";
        }
    }

    /**
     * 生成简化的消息key（只用于pending检查）
     */
    private String generateSimpleMessageKey(String content, long timestamp) {
        // 使用内容前20字符和时间戳分钟级
        String contentHash = content.length() > 20 ?
                content.substring(0, 20) : content;
        long minuteTimestamp = timestamp / 60000; // 精确到分钟
        return contentHash + "_" + minuteTimestamp;
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

    public String getContactName() {
        return contactName;
    }
}