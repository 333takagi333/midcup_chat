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
 * 群聊界面控制器（完整版，包含文件上传功能）
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
    private SimpleDateFormat fullTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();
    private final WindowManagementService windowService = new WindowManagementService();
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    // 用于去重的集合
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Long> pendingMessages = new ConcurrentHashMap<>();
    private final Map<String, ProgressBar> fileUploadProgress = new ConcurrentHashMap<>();

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
            fileUploadButton.setText("📎 文件");
            fileUploadButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10;");
            fileUploadButton.setOnAction(event -> handleFileUpload());
            fileUploadButton.setTooltip(new Tooltip("上传文件到群聊 (最大50MB)"));
        }
    }

    private void setupSendButton() {
        if (sendButton != null) {
            sendButton.setText("发送");
            sendButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 5 20;");
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
     * 上传文件到群聊
     */
    private void uploadFile(File file) {
        long timestamp = System.currentTimeMillis();
        String time = timeFormat.format(new Date(timestamp));
        String fileKey = generateFileKey(file, timestamp);

        try {
            // 1. 显示上传开始消息
            Platform.runLater(() -> {
                chatArea.appendText("[" + time + "] 开始上传文件到群聊: " + file.getName() +
                        " (" + FileService.formatFileSize(file.length()) + ")\n");
            });

            // 2. 向服务器请求上传权限和上传URL
            JsonObject uploadRequest = new JsonObject();
            uploadRequest.addProperty("type", "file_upload_request");
            uploadRequest.addProperty("senderId", userId);
            uploadRequest.addProperty("groupId", groupId);
            uploadRequest.addProperty("fileName", file.getName());
            uploadRequest.addProperty("fileSize", file.length());
            uploadRequest.addProperty("fileType", FileService.getFileTypeCategory(file));
            uploadRequest.addProperty("chatType", "group");

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
                // 4. 上传成功，发送群聊文件消息
                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();

                // 创建群聊文件消息
                JsonObject fileMessage = new JsonObject();
                fileMessage.addProperty("type", "group_file_message_send");
                fileMessage.addProperty("senderId", userId);
                fileMessage.addProperty("groupId", groupId);
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
                    sessionManager.addGroupMessage(groupId, displayMessage);

                    // 添加共享提示
                    chatArea.appendText("   ↳ 文件已共享到群聊\n");
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
        return String.format("group_file_%d_%d_%s_%d",
                groupId, userId, file.getName(), timestamp / 1000);
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

                // 检查是否是文件消息（服务器返回的JSON格式）
                try {
                    JsonObject jsonMessage = jsonParser.parse(content).getAsJsonObject();
                    if (jsonMessage.has("type")) {
                        String type = jsonMessage.get("type").getAsString();
                        if ("group_file_message".equals(type)) {
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
        // 生成消息唯一标识
        String messageKey = generateMessageKey(groupId, fromUserId, content, timestamp);

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
                    time, senderName, fileName, FileService.formatFileSize(fileSize));

            // 显示文件消息
            chatArea.appendText(displayMessage + "\n");

            // 添加下载提示
            String downloadHint = String.format("   ↳ %s共享了文件 (%s)",
                    senderId.equals(userId) ? "您" : senderName,
                    FileService.getFileTypeDescription(fileType));
            chatArea.appendText(downloadHint + "\n");

            // 保存到会话管理器
            sessionManager.addGroupMessage(groupId, displayMessage);

            // 如果是别人发的文件，提供下载提示
            if (!senderId.equals(userId)) {
                chatArea.appendText("   ↳ 右键聊天区域选择'下载文件'选项\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ChatGroupControl] 处理群聊文件消息失败: " + e.getMessage());
        }
    }

    /**
     * 下载群聊文件
     */
    private void downloadGroupFile(String fileName, String downloadUrl) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存群聊文件");
        fileChooser.setInitialFileName(fileName);

        File saveFile = fileChooser.showSaveDialog(chatArea.getScene().getWindow());
        if (saveFile != null) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        chatArea.appendText("开始下载群聊文件: " + fileName + "\n");
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

    public Long getUserId() {
        return userId;
    }

    /**
     * 右键菜单事件处理（用于下载文件）
     */
    @FXML
    private void handleContextMenu() {
        // 可以在这里实现右键菜单
    }
}