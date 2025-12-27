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
import java.text.SimpleDateFormat;  // 添加import
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
    @FXML private Button groupDetailButton;
    @FXML private Button fileUploadButton;
    @FXML private Button sendButton;

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
    private final RecentMessageService recentService = RecentMessageService.getInstance();
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChatUI();
        createHistoryButton();
        setupGroupDetailButton();
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

    private void setupGroupDetailButton() {
        if (groupDetailButton != null) {
            groupDetailButton.setText("👥 详情");
            groupDetailButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 5 10;");
            groupDetailButton.setOnAction(event -> showGroupDetails());
            groupDetailButton.setTooltip(new Tooltip("查看群聊详情"));
        }
    }

    private void setupFileUploadButton() {
        if (fileUploadButton != null) {
            fileUploadButton.setText("📎");
            fileUploadButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10;");
            fileUploadButton.setTooltip(new Tooltip("上传文件到群聊 (最大50MB)"));
        }
    }

    private void setupSendButton() {
        if (sendButton != null) {
            sendButton.setText("发送");
            sendButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 15;");
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
        chatArea.clear();
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

            if (sessionMessages == null || sessionMessages.isEmpty()) {
                // 没有本次登录的记录，显示简单欢迎信息
                chatArea.appendText("--- 欢迎来到 " + groupName + " ---\n\n");
                System.out.println("[ChatGroupControl] 无本次登录记录");
            } else {
                // 有本次登录的记录，直接显示所有记录
                for (String message : sessionMessages) {
                    chatArea.appendText(message + "\n");
                }

                // 滚动到底部
                chatArea.positionCaret(chatArea.getLength());
                System.out.println("[ChatGroupControl] 加载本次登录记录 " + sessionMessages.size() + " 条");
            }
        });
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
     * 处理文件上传
     */
    @FXML
    private void handleFileUpload() {
        System.out.println("[ChatGroupControl] 处理文件上传");

        Window window = chatArea.getScene().getWindow();

        FileService.chooseAndUploadFile(window, file -> {
            System.out.println("[ChatGroupControl] 选择了文件: " + file.getName());

            // 在聊天区域显示上传中消息
            String time = timeFormat.format(new Date());
            chatArea.appendText("[" + time + "] 正在上传文件: " + file.getName() + "\n");

            // 调用服务层处理文件上传
            chatService.uploadGroupFile(
                    window,
                    socketClient,
                    userId,
                    groupId,
                    groupName,
                    file,
                    () -> {
                        // 上传成功后的回调
                        Platform.runLater(() -> {
                            String time2 = timeFormat.format(new Date());
                            String displayMessage = String.format("[%s] 我: [文件] %s (%s)",
                                    time2, file.getName(), chatService.formatFileSize(file.length()));

                            chatArea.appendText(displayMessage + "\n");
                            sessionManager.addGroupMessage(groupId, displayMessage);

                            // 滚动到底部
                            chatArea.positionCaret(chatArea.getLength());

                            // 添加共享提示
                            chatArea.appendText("   ↳ 文件已共享到群聊\n");
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

        // 标记为pending
        pendingMessages.put(messageKey, timestamp);

        // 立即显示并保存
        chatArea.appendText(displayMessage + "\n");
        sessionManager.addGroupMessage(groupId, displayMessage);
        chatArea.positionCaret(chatArea.getLength()); // 滚动到底部

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

        // 消息已经由 MessageBroadcaster 保存到会话管理器，这里只需显示
        if (chatArea != null) {
            chatArea.appendText(displayMessage + "\n");
            chatArea.positionCaret(chatArea.getLength()); // 滚动到底部
        }

        System.out.println("[ChatGroupControl] 显示新群聊消息: " +
                (senderName.equals("我") ? "发送" : "接收") + " - " +
                content.substring(0, Math.min(20, content.length())));
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

            String time = timeFormat.format(new Date(timestamp));
            String senderName = senderId.equals(userId) ? "我" : "用户" + senderId;

            // 创建文件消息
            String displayMessage = String.format("[%s] %s: [文件] %s (%s)",
                    time, senderName, fileName, chatService.formatFileSize(fileSize));

            // 显示文件消息
            chatArea.appendText(displayMessage + "\n");

            // 添加文件类型提示
            String typeHint = getFileTypeHint(fileType);
            if (!typeHint.isEmpty()) {
                String hintText = senderId.equals(userId) ?
                        "您共享了" + typeHint : senderName + "共享了" + typeHint;
                chatArea.appendText("   ↳ " + hintText + "\n");
            }

            // 保存到会话管理器
            sessionManager.addGroupMessage(groupId, displayMessage);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ChatGroupControl] 处理群聊文件消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件类型提示
     */
    private String getFileTypeHint(String fileType) {
        switch (fileType) {
            case "image": return "图片文件";
            case "video": return "视频文件";
            case "audio": return "音频文件";
            case "document": return "文档文件";
            case "text": return "文本文件";
            case "archive": return "压缩文件";
            default: return "文件";
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