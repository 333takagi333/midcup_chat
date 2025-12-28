package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.ChatService;
import com.chat.service.ChatSessionManager;
import com.chat.service.FileService;
import com.chat.service.FileUploadService;
import com.chat.service.MessageBroadcaster;
import com.chat.service.RecentMessageService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import com.chat.ui.ChatMessageCellFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.util.Callback;

import java.io.File;
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
    @FXML private ListView<com.chat.model.ChatMessageModel> messageListView;
    @FXML private TextField messageInput;
    @FXML private HBox historyButtonBox;
    @FXML private Button groupDetailButton;
    @FXML private Button fileUploadButton;
    @FXML private Button sendButton;

    private Button loadHistoryButton;
    private ObservableList<com.chat.model.ChatMessageModel> messageList;

    private Long groupId;
    private String groupName;
    private String groupAvatarUrl;
    private SocketClient socketClient;
    private Long userId;
    private ChatService chatService;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();
    private final RecentMessageService recentService = RecentMessageService.getInstance();
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();
    private final Map<String, com.chat.model.ChatMessageModel> messageCache = new ConcurrentHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化消息列表
        messageList = FXCollections.observableArrayList();
        messageListView.setItems(messageList);

        setupChatUI();
        createHistoryButton();
        setupGroupDetailButton();
        setupFileUploadButton();
        setupSendButton();
    }

    private void setupChatUI() {
        messageInput.setOnAction(event -> sendMessage());

        // 设置ListView的单元格工厂（稍后在setGroupInfo中初始化）
        messageListView.setCellFactory(param -> {
            if (socketClient != null && chatService != null && groupId != null && userId != null) {
                return new ChatMessageCellFactory(socketClient, userId,
                        messageListView.getScene().getWindow(), chatService, "group", groupId);
            }
            return new ListCell<>() {
                @Override
                protected void updateItem(com.chat.model.ChatMessageModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.toString());
                        setGraphic(null);
                    }
                }
            };
        });
    }

    private void createHistoryButton() {
        loadHistoryButton = new Button("📜 历史记录");
        loadHistoryButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
        loadHistoryButton.setOnAction(event -> openHistoryWindow());
        loadHistoryButton.setTooltip(new Tooltip("查看群聊历史记录"));
        historyButtonBox.getChildren().add(loadHistoryButton);
    }

    private void setupGroupDetailButton() {
        if (groupDetailButton != null) {
            groupDetailButton.setText("👥 详情");
            groupDetailButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
            groupDetailButton.setOnAction(event -> showGroupDetails());
            groupDetailButton.setTooltip(new Tooltip("查看群聊详情"));
        }
    }

    private void setupFileUploadButton() {
        if (fileUploadButton != null) {
            fileUploadButton.setText("📎");
            fileUploadButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
            fileUploadButton.setTooltip(new Tooltip("上传文件到群聊 (最大50MB)"));
        }
    }

    private void setupSendButton() {
        if (sendButton != null) {
            sendButton.setText("发送");
            sendButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 6;");
            sendButton.setOnAction(event -> sendMessage());
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

            // 设置单元格工厂（需要所有信息都准备好）
            messageListView.setCellFactory(param ->
                    new ChatMessageCellFactory(socketClient, this.userId,
                            messageListView.getScene().getWindow(), chatService, "group", this.groupId));

            System.out.println("[ChatGroupControl] 设置群聊信息: " + groupName +
                    ", 群组ID: " + this.groupId + ", 监听器已注册");

        } catch (NumberFormatException e) {
            System.err.println("ID格式错误: " + e.getMessage());
            return;
        }

        groupNameLabel.setText(groupName);
        AvatarHelper.loadAvatar(groupAvatar, avatarUrl, true, 50);

        // ========== 关键：标记消息栏为已读，清除红点 ==========
        recentService.markAsRead(groupId.toString());
        System.out.println("[ChatGroupControl] 清除群聊消息栏红点: " + groupName);

        // 清空聊天区域并加载本次登录记录
        messageList.clear();
        messageCache.clear();
        loadCurrentSessionMessages();

        System.out.println("[ChatGroupControl] 群聊窗口已打开，已加载本次登录记录");
    }

    /**
     * 只加载本次登录期间的群聊记录
     */
    private void loadCurrentSessionMessages() {
        Platform.runLater(() -> {
            // 从会话管理器获取本次登录的聊天记录
            List<String> sessionMessages = sessionManager.getGroupSession(groupId);

            // 清空消息缓存
            messageList.clear();
            messageCache.clear();

            if (sessionMessages == null || sessionMessages.isEmpty()) {
                // 没有本次登录的记录，显示简单欢迎信息
                com.chat.model.ChatMessageModel welcomeMessage = new com.chat.model.ChatMessageModel(
                        "welcome_" + System.currentTimeMillis(),
                        userId,
                        "系统",
                        "--- 欢迎来到 " + groupName + " ---",
                        System.currentTimeMillis(),
                        false
                );
                messageList.add(welcomeMessage);
                System.out.println("[ChatGroupControl] 无本次登录记录");
            } else {
                // 有本次登录的记录，转换并显示所有记录
                for (String message : sessionMessages) {
                    // 解析消息字符串为ChatMessageModel
                    com.chat.model.ChatMessageModel messageModel = parseMessageString(message);
                    if (messageModel != null) {
                        messageList.add(messageModel);
                        messageCache.put(messageModel.getMessageId(), messageModel);
                    }
                }

                // 滚动到底部
                messageListView.scrollTo(messageList.size() - 1);
                System.out.println("[ChatGroupControl] 加载本次登录记录 " + sessionMessages.size() + " 条");
            }
        });
    }

    /**
     * 解析消息字符串为ChatMessageModel
     */
    private com.chat.model.ChatMessageModel parseMessageString(String messageStr) {
        try {
            // 示例消息格式: "[18:10] 用户1: 你好" 或 "[18:10] 用户1: [文件] 2.txt (1.5 KB)"
            if (messageStr.startsWith("[") && messageStr.contains("]")) {
                int timeEnd = messageStr.indexOf("]");
                String timePart = messageStr.substring(1, timeEnd);

                int colonIndex = messageStr.indexOf(":", timeEnd);
                if (colonIndex == -1) return null;

                String sender = messageStr.substring(timeEnd + 2, colonIndex).trim();
                String content = messageStr.substring(colonIndex + 2).trim();

                boolean isMyMessage = "我".equals(sender);
                Long senderId = isMyMessage ? userId : 0L; // 对于群聊，需要从数据库获取senderId

                // 生成消息ID
                String messageId = "msg_" + System.currentTimeMillis() + "_" + content.hashCode() + "_" + UUID.randomUUID().toString().substring(0, 8);

                // 检查是否是文件消息
                if (content.startsWith("[文件]")) {
                    // 解析文件消息
                    // 格式: [文件] 文件名 (大小)
                    String fileInfo = content.substring(4).trim();
                    int parenIndex = fileInfo.lastIndexOf("(");
                    if (parenIndex != -1) {
                        String fileName = fileInfo.substring(0, parenIndex).trim();
                        String sizeStr = fileInfo.substring(parenIndex + 1, fileInfo.length() - 1).trim();

                        // 解析文件大小
                        long fileSize = parseFileSize(sizeStr);
                        String fileType = FileService.getFileTypeCategory(new File(fileName));

                        // 生成文件ID
                        String fileId = "file_" + senderId + "_" +
                                System.currentTimeMillis() + "_" + fileName.hashCode();

                        return new com.chat.model.ChatMessageModel(
                                messageId,
                                senderId,
                                sender,
                                fileName,
                                fileSize,
                                fileType,
                                fileId,
                                System.currentTimeMillis(),
                                isMyMessage
                        );
                    }
                }

                // 文本消息
                return new com.chat.model.ChatMessageModel(
                        messageId,
                        senderId,
                        sender,
                        content,
                        System.currentTimeMillis(),
                        isMyMessage
                );
            }
        } catch (Exception e) {
            System.err.println("[ChatGroupControl] 解析消息失败: " + messageStr + ", 错误: " + e.getMessage());
        }
        return null;
    }

    /**
     * 解析文件大小字符串
     */
    private long parseFileSize(String sizeStr) {
        try {
            if (sizeStr.endsWith(" B")) {
                return Long.parseLong(sizeStr.replace(" B", "").trim());
            } else if (sizeStr.endsWith(" KB")) {
                double kb = Double.parseDouble(sizeStr.replace(" KB", "").trim());
                return (long)(kb * 1024);
            } else if (sizeStr.endsWith(" MB")) {
                double mb = Double.parseDouble(sizeStr.replace(" MB", "").trim());
                return (long)(mb * 1024 * 1024);
            } else if (sizeStr.endsWith(" GB")) {
                double gb = Double.parseDouble(sizeStr.replace(" GB", "").trim());
                return (long)(gb * 1024 * 1024 * 1024);
            }
        } catch (Exception e) {
            System.err.println("解析文件大小失败: " + sizeStr);
        }
        return 0;
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
            detailsStage.initOwner(messageListView.getScene().getWindow());
            detailsStage.setTitle(groupName + " 的详情");
            detailsStage.setScene(new javafx.scene.Scene(groupDetailsRoot, 450, 550));
            detailsStage.show();

        } catch (Exception e) {
            System.err.println("[ChatGroupControl] 打开群聊详情失败: " + e.getMessage());
            DialogUtil.showError(messageListView.getScene().getWindow(), "打开群聊详情失败");
        }
    }

    /**
     * 处理文件上传
     */
    @FXML
    private void handleFileUpload() {
        System.out.println("[ChatGroupControl] 处理文件上传");

        Window window = messageListView.getScene().getWindow();

        FileUploadService.uploadFile(
                window,
                socketClient,
                userId,
                null, // contactId为null，因为是群聊
                groupId,
                "group",
                new FileUploadService.FileUploadCallback() {
                    @Override
                    public void onUploadSuccess(FileUploadService.FileUploadResult result) {
                        Platform.runLater(() -> {
                            // 在聊天区域显示文件消息
                            String time = timeFormat.format(new Date());
                            String displayMessage = String.format("[%s] 我: [文件] %s (%s)",
                                    time, result.getFileName(), result.getFormattedFileSize());

                            // 创建文件消息模型
                            com.chat.model.ChatMessageModel fileMessage = new com.chat.model.ChatMessageModel(
                                    "file_" + System.currentTimeMillis() + "_" + result.getFileName().hashCode(),
                                    userId,
                                    "我",
                                    result.getFileName(),
                                    result.getFileSize(),
                                    result.getFileType(),
                                    result.getFileId(),
                                    System.currentTimeMillis(),
                                    true
                            );

                            // 添加到消息列表
                            messageList.add(fileMessage);
                            messageCache.put(fileMessage.getMessageId(), fileMessage);
                            messageListView.scrollTo(messageList.size() - 1);

                            // 保存到会话管理器
                            sessionManager.addGroupMessage(groupId, displayMessage);
                        });
                    }

                    @Override
                    public void onUploadFailure(String errorMessage) {
                        Platform.runLater(() -> {
                            DialogUtil.showError(window, "上传失败");
                        });
                    }
                }
        );
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
            historyStage.initOwner(messageListView.getScene().getWindow());
            historyStage.setTitle(groupName + " - 历史记录");
            historyStage.setScene(new javafx.scene.Scene(historyRoot, 600, 700));
            historyStage.show();

            System.out.println("[ChatGroupControl] 历史记录窗口已打开");

        } catch (Exception e) {
            System.err.println("[ChatGroupControl] 打开历史记录窗口失败: " + e.getMessage());
            DialogUtil.showError(messageListView.getScene().getWindow(), "打开历史记录窗口失败: " + e.getMessage());
        }
    }

    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || socketClient == null || !socketClient.isConnected()
                || groupId == null || userId == null) {
            return;
        }

        // 生成简化的消息key
        long timestamp = System.currentTimeMillis();
        String messageKey = generateSimpleMessageKey(content, timestamp);

        // 先清空输入框
        messageInput.clear();

        // 在本地立即显示
        String time = timeFormat.format(new Date(timestamp));
        String displayMessage = "[" + time + "] 我: " + content;

        // 创建消息模型
        com.chat.model.ChatMessageModel messageModel = new com.chat.model.ChatMessageModel(
                "temp_" + messageKey,
                userId,
                "我",
                content,
                timestamp,
                true
        );

        // 标记为pending
        pendingMessages.put(messageKey, timestamp);

        // 立即显示并保存
        Platform.runLater(() -> {
            messageList.add(messageModel);
            messageCache.put(messageModel.getMessageId(), messageModel);
            messageListView.scrollTo(messageList.size() - 1);
        });

        sessionManager.addGroupMessage(groupId, displayMessage);

        System.out.println("[ChatGroupControl] 本地显示群聊消息，key: " + messageKey);

        // 异步发送到服务器
        new Thread(() -> {
            boolean sent = chatService.sendGroupMessage(socketClient, groupId, userId, content);

            if (sent) {
                System.out.println("[ChatGroupControl] 群聊消息发送成功到服务器");

                // 3秒后清理pending状态
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        pendingMessages.remove(messageKey);
                        System.out.println("[ChatGroupControl] 清理pending消息: " + messageKey);
                    }
                }, 3000);

            } else {
                Platform.runLater(() -> {
                    DialogUtil.showError(messageListView.getScene().getWindow(), "发送失败，请检查网络连接");
                    pendingMessages.remove(messageKey);

                    // 标记为发送失败
                    com.chat.model.ChatMessageModel failedMessage = new com.chat.model.ChatMessageModel(
                            "failed_" + messageKey,
                            userId,
                            "我",
                            "[发送失败] " + content,
                            System.currentTimeMillis(),
                            true
                    );
                    messageList.add(failedMessage);
                    messageListView.scrollTo(messageList.size() - 1);
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

                // 检查是否是文件消息（服务器返回的JSON格式）
                try {
                    JsonObject jsonMessage = jsonParser.parse(content).getAsJsonObject();
                    if (jsonMessage.has("type")) {
                        String type = jsonMessage.get("type").getAsString();
                        if ("group_file_message_receive".equals(type)) {
                            handleGroupFileMessage(jsonMessage, fromUserId, timestamp);
                            return;
                        }
                    }
                } catch (Exception e) {
                    // 不是JSON格式，是普通文本消息
                }

                // 处理普通文本消息
                handleGroupTextMessage(messageGroupId, fromUserId, content, timestamp, messageId);
            } else {
                System.out.println("[ChatGroupControl] 收到非当前群组的消息: " + messageGroupId +
                        " (当前群组: " + groupId + ")");
            }
        });
    }

    /**
     * 处理群聊文本消息
     */
    private void handleGroupTextMessage(Long groupId, Long fromUserId, String content,
                                        long timestamp, Long messageId) {
        // 生成简化的消息key
        String messageKey = generateSimpleMessageKey(content, timestamp);

        // 检查是否是刚发送的pending消息
        if (pendingMessages.containsKey(messageKey)) {
            System.out.println("[ChatGroupControl] 这是刚发送的群聊消息回传: " + messageKey);
            pendingMessages.remove(messageKey);
            return;
        }

        // 正常处理新消息
        String time = timeFormat.format(new Date(timestamp));
        String senderName = fromUserId.equals(userId) ? "我" : "用户" + fromUserId;
        String displayMessage = "[" + time + "] " + senderName + ": " + content;

        // 创建消息模型
        String cacheKey = "msg_" + (messageId != null ? messageId : content.hashCode());
        if (messageCache.containsKey(cacheKey)) {
            System.out.println("[ChatGroupControl] 消息已在列表中: " + content.substring(0, Math.min(20, content.length())));
            return;
        }

        com.chat.model.ChatMessageModel messageModel = new com.chat.model.ChatMessageModel(
                cacheKey,
                fromUserId,
                senderName,
                content,
                timestamp,
                fromUserId.equals(userId)
        );

        // 添加到消息列表
        messageList.add(messageModel);
        messageCache.put(cacheKey, messageModel);
        messageListView.scrollTo(messageList.size() - 1);

        System.out.println("[ChatGroupControl] 显示新群聊消息: " +
                (senderName.equals("我") ? "发送" : "接收") + " - " +
                content.substring(0, Math.min(20, content.length())));

        // 保存到会话管理器
        sessionManager.addGroupMessage(groupId, displayMessage);
    }

    /**
     * 处理群聊文件消息
     */
    private void handleGroupFileMessage(JsonObject fileMessage, Long fromUserId, long timestamp) {
        try {
            String fileId = fileMessage.get("fileId").getAsString();
            String fileName = fileMessage.get("fileName").getAsString();
            long fileSize = fileMessage.get("fileSize").getAsLong();
            String fileType = fileMessage.get("fileType").getAsString();
            String downloadUrl = fileMessage.get("downloadUrl").getAsString();
            Long senderId = fileMessage.get("senderId").getAsLong();
            Long messageGroupId = fileMessage.get("groupId").getAsLong();
            Long messageId = fileMessage.has("messageId") ? fileMessage.get("messageId").getAsLong() : null;

            String time = timeFormat.format(new Date(timestamp));

            // 创建文件消息模型
            boolean isMyMessage = senderId.equals(userId);
            com.chat.model.ChatMessageModel messageModel = new com.chat.model.ChatMessageModel(
                    "file_" + (messageId != null ? messageId : System.currentTimeMillis()),
                    senderId,
                    isMyMessage ? "我" : "用户" + senderId,
                    fileName,
                    fileSize,
                    fileType,
                    fileId,
                    timestamp,
                    isMyMessage
            );

            Platform.runLater(() -> {
                // 添加到消息列表
                messageList.add(messageModel);
                messageCache.put(messageModel.getMessageId(), messageModel);
                messageListView.scrollTo(messageList.size() - 1);
            });

            // 保存到会话管理器
            String displayMessage = String.format("[%s] %s: [文件] %s (%s)",
                    time, isMyMessage ? "我" : "用户" + senderId,
                    fileName, chatService.formatFileSize(fileSize));
            sessionManager.addGroupMessage(groupId, displayMessage);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ChatGroupControl] 处理群聊文件消息失败: " + e.getMessage());
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
        if (groupId != null) {
            broadcaster.unregisterGroupListener(groupId.toString(), this);
        }

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

    public Long getUserId() {
        return userId;
    }
}