package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.service.ChatService;
import com.chat.service.ChatSessionManager;
import com.chat.service.FileService;
import com.chat.service.MessageBroadcaster;
import com.chat.service.WindowManagementService;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私聊界面控制器（完整版，包含文件上传功能）
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
    private SimpleDateFormat fullTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();
    private final WindowManagementService windowService = new WindowManagementService();
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    private String listenerKey;

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 临时存储刚发送的消息，等待服务器确认
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();
    // 存储文件上传进度
    private final Map<String, ProgressBar> fileUploadProgress = new ConcurrentHashMap<>();

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
        // 设置聊天区域自动换行
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
            fileUploadButton.setText("📎 文件");
            fileUploadButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10;");
            fileUploadButton.setOnAction(event -> handleFileUpload());
            fileUploadButton.setTooltip(new Tooltip("上传文件 (最大50MB)"));
        }
    }

    private void setupSendButton() {
        if (sendButton != null) {
            sendButton.setText("发送");
            sendButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 5 20;");
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
     * 处理文件上传
     */
    @FXML
    private void handleFileUpload() {
        FileService.chooseAndUploadFile(chatArea.getScene().getWindow(), file -> {
            // 在新线程中上传文件
            new Thread(() -> uploadFile(file)).start();
        });
    }

    /**
     * 上传文件
     */
    private void uploadFile(File file) {
        long timestamp = System.currentTimeMillis();
        String time = timeFormat.format(new Date(timestamp));
        String fileKey = generateFileKey(file, timestamp);

        try {
            // 1. 显示上传开始消息
            Platform.runLater(() -> {
                chatArea.appendText("[" + time + "] 开始上传文件: " + file.getName() +
                        " (" + FileService.formatFileSize(file.length()) + ")\n");
            });

            // 2. 向服务器请求上传权限和上传URL
            JsonObject uploadRequest = new JsonObject();
            uploadRequest.addProperty("type", "file_upload_request");
            uploadRequest.addProperty("senderId", userId);
            uploadRequest.addProperty("receiverId", contactId);
            uploadRequest.addProperty("fileName", file.getName());
            uploadRequest.addProperty("fileSize", file.length());
            uploadRequest.addProperty("fileType", FileService.getFileTypeCategory(file));
            uploadRequest.addProperty("chatType", "private");

            String response = socketClient.sendRequest(uploadRequest);

            if (response == null) {
                Platform.runLater(() -> {
                    chatArea.appendText("[" + time + "] 上传失败：无法连接到服务器\n");
                    DialogUtil.showError(chatArea.getScene().getWindow(), "上传失败：无法连接到服务器");
                });
                return;
            }

            // 解析响应
            JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
            boolean success = jsonResponse.get("success").getAsBoolean();

            if (!success) {
                String errorMsg = jsonResponse.get("message").getAsString();
                Platform.runLater(() -> {
                    chatArea.appendText("[" + time + "] 上传失败：" + errorMsg + "\n");
                    DialogUtil.showError(chatArea.getScene().getWindow(), "上传失败：" + errorMsg);
                });
                return;
            }

            String fileId = jsonResponse.get("fileId").getAsString();
            String uploadUrl = jsonResponse.get("uploadUrl").getAsString();

            // 3. 上传文件到服务器
            boolean uploadSuccess = uploadFileToServer(file, uploadUrl);

            if (uploadSuccess) {
                // 4. 上传成功，发送文件消息
                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();

                // 创建文件消息
                JsonObject fileMessage = new JsonObject();
                fileMessage.addProperty("type", "file_message_send");
                fileMessage.addProperty("senderId", userId);
                fileMessage.addProperty("receiverId", contactId);
                fileMessage.addProperty("fileId", fileId);
                fileMessage.addProperty("fileName", file.getName());
                fileMessage.addProperty("fileSize", file.length());
                fileMessage.addProperty("fileType", FileService.getFileTypeCategory(file));
                fileMessage.addProperty("downloadUrl", downloadUrl);
                fileMessage.addProperty("timestamp", timestamp);

                // 发送文件消息
                socketClient.sendMessage(fileMessage);

                // 5. 在本地显示文件消息
                Platform.runLater(() -> {
                    String displayMessage = String.format("[%s] 我: [文件] %s (%s)\n",
                            timeFormat.format(new Date(timestamp)),
                            file.getName(),
                            FileService.formatFileSize(file.length()));

                    chatArea.appendText(displayMessage);

                    // 保存到会话管理器
                    sessionManager.addPrivateMessage(userId, contactId, displayMessage);

                    // 添加下载提示
                    chatArea.appendText("   ↳ 文件已上传，好友可以下载\n");
                });

            } else {
                Platform.runLater(() -> {
                    chatArea.appendText("[" + time + "] 上传失败\n");
                    DialogUtil.showError(chatArea.getScene().getWindow(), "文件上传失败");
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                chatArea.appendText("[" + time + "] 上传异常：" + e.getMessage() + "\n");
                DialogUtil.showError(chatArea.getScene().getWindow(), "上传异常：" + e.getMessage());
            });
        }
    }

    /**
     * 将文件上传到服务器
     */
    private boolean uploadFileToServer(File file, String uploadUrl) {
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(file.length()));
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // 上传文件
            Files.copy(file.toPath(), connection.getOutputStream());

            int responseCode = connection.getResponseCode();
            boolean success = responseCode == 200 || responseCode == 201;

            connection.disconnect();
            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 生成文件唯一标识
     */
    private String generateFileKey(File file, long timestamp) {
        return String.format("file_%d_%d_%s_%d",
                userId, contactId, file.getName(), timestamp / 1000);
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

        // 生成消息唯一标识（用于去重）
        long timestamp = System.currentTimeMillis();
        String messageKey = generateMessageKey(userId, contactId, content, timestamp);

        // 先清空输入框
        messageInput.clear();

        // 在本地立即显示（给用户即时反馈）
        String time = timeFormat.format(new Date(timestamp));
        String displayMessage = String.format("[%s] 我: %s", time, content);

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

    @Override
    public void onPrivateMessageReceived(Long fromUserId, Long toUserId, String content,
                                         long timestamp, Long messageId) {
        Platform.runLater(() -> {
            // 检查是否是当前联系人的消息
            if ((fromUserId.equals(contactId) && toUserId.equals(userId)) ||
                    (fromUserId.equals(userId) && toUserId.equals(contactId))) {

                // 检查是否是文件消息（服务器返回的JSON格式）
                try {
                    JsonObject jsonMessage = jsonParser.parse(content).getAsJsonObject();
                    if (jsonMessage.has("type")) {
                        String type = jsonMessage.get("type").getAsString();
                        if ("file_message".equals(type)) {
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
        String displayMessage = String.format("[%s] %s: %s", time, senderName, content);

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

            // 创建可点击的文件链接
            String displayMessage = String.format("[%s] %s: [文件] %s (%s)",
                    time, senderName, fileName, FileService.formatFileSize(fileSize));

            // 显示文件消息
            chatArea.appendText(displayMessage + "\n");

            // 添加下载提示
            String downloadHint = String.format("   ↳ 点击下载文件 (%s)",
                    FileService.getFileTypeDescription(fileType));
            chatArea.appendText(downloadHint + "\n");

            // 保存到会话管理器
            sessionManager.addPrivateMessage(userId, contactId, displayMessage);

            // 可以在这里添加下载功能
            if (!senderId.equals(userId)) {
                // 如果是别人发的文件，提供下载链接
                addDownloadContextMenu(fileName, downloadUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ChatPrivateControl] 处理文件消息失败: " + e.getMessage());
        }
    }

    /**
     * 添加上下文菜单（用于下载文件）
     */
    private void addDownloadContextMenu(String fileName, String downloadUrl) {
        // 由于TextArea不支持上下文菜单，我们可以添加一个提示消息
        // 在实际项目中，可以考虑使用ListView或WebView来显示聊天内容
        Platform.runLater(() -> {
            chatArea.appendText("   ↳ 右键聊天区域选择'下载文件'选项\n");
        });
    }

    /**
     * 下载文件
     */
    private void downloadFile(String fileName, String downloadUrl) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存文件");
        fileChooser.setInitialFileName(fileName);

        File saveFile = fileChooser.showSaveDialog(chatArea.getScene().getWindow());
        if (saveFile != null) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        chatArea.appendText("开始下载文件: " + fileName + "\n");
                    });

                    URL url = new URL(downloadUrl);
                    URLConnection connection = url.openConnection();
                    try (InputStream in = connection.getInputStream()) {
                        Files.copy(in, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        Platform.runLater(() -> {
                            chatArea.appendText("文件下载完成: " + saveFile.getAbsolutePath() + "\n");
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        chatArea.appendText("下载失败: " + e.getMessage() + "\n");
                        DialogUtil.showError(chatArea.getScene().getWindow(), "下载失败: " + e.getMessage());
                    });
                }
            }).start();
        }
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

    public String getContactName() {
        return contactName;
    }

    /**
     * 右键菜单事件处理（用于下载文件）
     */
    @FXML
    private void handleContextMenu() {
        // 可以在这里实现右键菜单
    }
}