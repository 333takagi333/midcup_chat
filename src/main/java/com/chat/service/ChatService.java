package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 聊天业务逻辑服务（包含文件上传功能）
 */
public class ChatService {

    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();
    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();

    // 时间格式化器
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    // 数据库datetime格式
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 上传私聊文件
     */
    public void uploadPrivateFile(Window window, SocketClient client, Long userId, Long contactId,
                                  String contactName, File file, Runnable onSuccess) {
        if (file == null || !file.exists()) {
            Platform.runLater(() -> showError(window, "错误", "文件不存在"));
            return;
        }

        // 检查文件大小（最大50MB）
        if (file.length() > 50 * 1024 * 1024) {
            Platform.runLater(() -> showError(window, "错误", "文件太大，最大支持50MB"));
            return;
        }

        // 1. 创建上传请求
        FileUploadRequest uploadRequest = new FileUploadRequest();
        uploadRequest.setSenderId(userId);
        uploadRequest.setReceiverId(contactId);
        uploadRequest.setFileName(file.getName());
        uploadRequest.setFileSize(file.length());
        uploadRequest.setFileType(getFileType(file));
        uploadRequest.setChatType("private");

        // 2. 发送上传请求
        new Thread(() -> {
            try {
                String response = client.sendRequest(uploadRequest);
                if (response == null) {
                    Platform.runLater(() -> showError(window, "上传失败", "无法连接到服务器"));
                    return;
                }

                JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
                if (!jsonResponse.get("success").getAsBoolean()) {
                    String errorMsg = jsonResponse.get("message").getAsString();
                    Platform.runLater(() -> showError(window, "上传失败", errorMsg));
                    return;
                }

                String fileId = jsonResponse.get("fileId").getAsString();
                String uploadUrl = jsonResponse.get("uploadUrl").getAsString();
                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();

                // 3. 上传文件到服务器
                boolean uploadSuccess = uploadFileToServer(file, uploadUrl);
                if (!uploadSuccess) {
                    Platform.runLater(() -> showError(window, "上传失败", "文件上传失败"));
                    return;
                }

                // 4. 发送文件消息
                sendFileMessage(client, userId, contactId, null, "private",
                        fileId, file.getName(), file.length(), getFileType(file), downloadUrl);

                // 5. 成功回调
                Platform.runLater(() -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    showInfo(window, "上传成功", "文件已发送");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError(window, "上传异常", e.getMessage()));
            }
        }).start();
    }

    /**
     * 上传群聊文件
     */
    public void uploadGroupFile(Window window, SocketClient client, Long userId, Long groupId,
                                String groupName, File file, Runnable onSuccess) {
        if (file == null || !file.exists()) {
            Platform.runLater(() -> showError(window, "错误", "文件不存在"));
            return;
        }

        // 检查文件大小（最大50MB）
        if (file.length() > 50 * 1024 * 1024) {
            Platform.runLater(() -> showError(window, "错误", "文件太大，最大支持50MB"));
            return;
        }

        // 1. 创建上传请求
        FileUploadRequest uploadRequest = new FileUploadRequest();
        uploadRequest.setSenderId(userId);
        uploadRequest.setGroupId(groupId);
        uploadRequest.setFileName(file.getName());
        uploadRequest.setFileSize(file.length());
        uploadRequest.setFileType(getFileType(file));
        uploadRequest.setChatType("group");

        // 2. 发送上传请求
        new Thread(() -> {
            try {
                String response = client.sendRequest(uploadRequest);
                if (response == null) {
                    Platform.runLater(() -> showError(window, "上传失败", "无法连接到服务器"));
                    return;
                }

                JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
                if (!jsonResponse.get("success").getAsBoolean()) {
                    String errorMsg = jsonResponse.get("message").getAsString();
                    Platform.runLater(() -> showError(window, "上传失败", errorMsg));
                    return;
                }

                String fileId = jsonResponse.get("fileId").getAsString();
                String uploadUrl = jsonResponse.get("uploadUrl").getAsString();
                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();

                // 3. 上传文件到服务器
                boolean uploadSuccess = uploadFileToServer(file, uploadUrl);
                if (!uploadSuccess) {
                    Platform.runLater(() -> showError(window, "上传失败", "文件上传失败"));
                    return;
                }

                // 4. 发送群聊文件消息
                sendFileMessage(client, userId, null, groupId, "group",
                        fileId, file.getName(), file.length(), getFileType(file), downloadUrl);

                // 5. 成功回调
                Platform.runLater(() -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    showInfo(window, "上传成功", "文件已共享到群聊");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError(window, "上传异常", e.getMessage()));
            }
        }).start();
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
     * 发送文件消息
     */
    private void sendFileMessage(SocketClient client, Long senderId, Long receiverId, Long groupId,
                                 String chatType, String fileId, String fileName, long fileSize,
                                 String fileType, String downloadUrl) {
        try {
            JsonObject fileMessage = new JsonObject();

            if ("private".equals(chatType)) {
                fileMessage.addProperty("type", "file_message_send");
                fileMessage.addProperty("senderId", senderId);
                fileMessage.addProperty("receiverId", receiverId);
            } else if ("group".equals(chatType)) {
                fileMessage.addProperty("type", "group_file_message_send");
                fileMessage.addProperty("senderId", senderId);
                fileMessage.addProperty("groupId", groupId);
            }

            fileMessage.addProperty("fileId", fileId);
            fileMessage.addProperty("fileName", fileName);
            fileMessage.addProperty("fileSize", fileSize);
            fileMessage.addProperty("fileType", fileType);
            fileMessage.addProperty("downloadUrl", downloadUrl);
            fileMessage.addProperty("timestamp", System.currentTimeMillis());

            // 发送文件消息
            client.sendMessage(fileMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文件类型
     */
    private String getFileType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".gif") || name.endsWith(".bmp")) {
            return "image";
        } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") ||
                name.endsWith(".wmv") || name.endsWith(".mkv")) {
            return "video";
        } else if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") ||
                name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") ||
                name.endsWith(".pptx")) {
            return "document";
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac")) {
            return "audio";
        } else if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")) {
            return "archive";
        } else if (name.endsWith(".txt")) {
            return "text";
        } else {
            return "other";
        }
    }

    /**
     * 格式化文件大小
     */
    public String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 显示错误对话框
     */
    private void showError(Window window, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(window);
        alert.show();
    }

    /**
     * 显示信息对话框
     */
    private void showInfo(Window window, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(window);
        alert.show();
    }

    // ========== 以下是原有的方法 ==========

    /**
     * 发送私聊消息
     */
    public boolean sendPrivateMessage(SocketClient client, Long contactId, Long userId, String content) {
        try {
            ChatPrivateSend message = new ChatPrivateSend();
            message.setToUserId(contactId);
            message.setFromUserId(userId);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());

            System.out.println("[ChatService] 发送私聊消息: " + userId + " -> " + contactId + ", 内容: " + content);

            boolean sent = client.sendMessage(message);

            if (sent) {
                System.out.println("[ChatService] 消息发送成功，等待服务器回传");
            } else {
                System.err.println("[ChatService] 消息发送失败");
            }

            return sent;
        } catch (Exception e) {
            System.err.println("发送私聊消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送群聊消息
     */
    public boolean sendGroupMessage(SocketClient client, Long groupId, Long userId, String content) {
        try {
            ChatGroupSend message = new ChatGroupSend();
            message.setGroupId(groupId);
            message.setFromUserId(userId);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());

            System.out.println("[ChatService] 发送群聊消息: 群组" + groupId + ", 发送者" + userId + ", 内容: " + content);

            boolean sent = client.sendMessage(message);

            if (sent) {
                System.out.println("[ChatService] 群聊消息发送成功，等待服务器回传");
            }

            return sent;
        } catch (Exception e) {
            System.err.println("发送群聊消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理接收到的消息并广播（不处理历史消息响应）
     */
    public void processMessage(String messageJson) {
        try {
            System.out.println("[ChatService] 处理消息: " + messageJson);

            JsonObject jsonObject = jsonParser.parse(messageJson).getAsJsonObject();

            if (jsonObject.has("type")) {
                String type = jsonObject.get("type").getAsString();

                // ========== 重要：跳过历史消息响应 ==========
                if (MessageType.CHAT_HISTORY_RESPONSE.equals(type)) {
                    System.out.println("[ChatService] 收到历史消息响应，跳过处理（由 HistoryService 处理）");
                    return;
                }

                // 私聊消息
                if (MessageType.CHAT_PRIVATE_RECEIVE.equals(type)) {
                    Long messageId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : null;
                    Long fromUserId = jsonObject.get("fromUserId").getAsLong();
                    Long toUserId = jsonObject.get("toUserId").getAsLong();
                    String content = jsonObject.get("content").getAsString();
                    long timestamp = jsonObject.has("timestamp") ?
                            jsonObject.get("timestamp").getAsLong() : System.currentTimeMillis();

                    // 获取发送方用户名
                    String senderName = "用户" + fromUserId;

                    broadcaster.broadcastPrivateMessage(
                            fromUserId,
                            toUserId,
                            content,
                            timestamp,
                            senderName,
                            messageId
                    );

                    System.out.println("[ChatService] 已处理并广播私聊消息: " + fromUserId + " -> " + toUserId);
                }
                // 群聊消息
                else if (MessageType.CHAT_GROUP_RECEIVE.equals(type)) {
                    Long messageId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : null;
                    Long groupId = jsonObject.get("groupId").getAsLong();
                    Long fromUserId = jsonObject.get("fromUserId").getAsLong();
                    String content = jsonObject.get("content").getAsString();
                    long timestamp = jsonObject.has("timestamp") ?
                            jsonObject.get("timestamp").getAsLong() : System.currentTimeMillis();

                    String groupName = "群聊" + groupId;

                    broadcaster.broadcastGroupMessage(
                            groupId,
                            fromUserId,
                            content,
                            timestamp,
                            groupName,
                            messageId
                    );

                    System.out.println("[ChatService] 已处理并广播群聊消息: 群组" + groupId);
                }
                // 文件消息
                else if ("file_message_receive".equals(type) || "group_file_message_receive".equals(type)) {
                    handleFileMessage(jsonObject);
                }
                // 其他类型的消息（好友请求、系统通知等）
                else {
                    System.out.println("[ChatService] 收到其他类型消息: " + type);
                }
            }
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理文件消息
     */
    private void handleFileMessage(JsonObject fileMessage) {
        try {
            String type = fileMessage.get("type").getAsString();
            String fileId = fileMessage.get("fileId").getAsString();
            String fileName = fileMessage.get("fileName").getAsString();
            long fileSize = fileMessage.get("fileSize").getAsLong();
            String fileType = fileMessage.get("fileType").getAsString();
            String downloadUrl = fileMessage.get("downloadUrl").getAsString();
            Long senderId = fileMessage.get("senderId").getAsLong();
            long timestamp = fileMessage.get("timestamp").getAsLong();

            if ("file_message_receive".equals(type)) {
                // 私聊文件消息
                Long receiverId = fileMessage.get("receiverId").getAsLong();

                // 广播私聊文件消息
                broadcastFileMessage("private", senderId, receiverId, null,
                        fileName, fileSize, fileType, downloadUrl, timestamp);

            } else if ("group_file_message_receive".equals(type)) {
                // 群聊文件消息
                Long groupId = fileMessage.get("groupId").getAsLong();

                // 广播群聊文件消息
                broadcastFileMessage("group", senderId, null, groupId,
                        fileName, fileSize, fileType, downloadUrl, timestamp);
            }

            System.out.println("[ChatService] 处理文件消息: " + fileName + " (" + formatFileSize(fileSize) + ")");

        } catch (Exception e) {
            System.err.println("处理文件消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 广播文件消息
     */
    private void broadcastFileMessage(String chatType, Long senderId, Long receiverId, Long groupId,
                                      String fileName, long fileSize, String fileType,
                                      String downloadUrl, long timestamp) {
        // 这里可以添加文件消息的广播逻辑
        // 可以扩展MessageBroadcaster来支持文件消息
        System.out.println("[ChatService] 广播文件消息: " + fileName + " (" + chatType + ")");
    }

    /**
     * 解析数据库datetime字符串为Date对象
     */
    public Date parseDbDateTime(String datetimeStr) {
        if (datetimeStr == null || datetimeStr.trim().isEmpty()) {
            return new Date();
        }

        try {
            // 尝试解析为数据库datetime格式
            return dbDateFormat.parse(datetimeStr);
        } catch (ParseException e) {
            System.err.println("[ChatService] 解析datetime失败: " + datetimeStr + ", 错误: " + e.getMessage());
            return new Date();
        }
    }

    /**
     * 格式化数据库datetime字符串为显示时间（HH:mm）
     */
    public String formatDbDateTimeForDisplay(String datetimeStr) {
        Date date = parseDbDateTime(datetimeStr);
        return timeFormat.format(date);
    }

    /**
     * 将数据库datetime字符串转换为Long类型时间戳（用于比较和分页）
     */
    public Long dbDateTimeToTimestamp(String datetimeStr) {
        Date date = parseDbDateTime(datetimeStr);
        return date.getTime();
    }

    /**
     * 获取 Gson 实例（用于解析JSON）
     */
    public Gson getGson() {
        return gson;
    }
}