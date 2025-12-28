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
 * 聊天业务逻辑服务（只包含文件下载功能，上传功能独立到FileUploadService）
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
    private String serverBaseUrl = "http://localhost:8080/";

    // 可以设置服务器地址
    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
        System.out.println("[ChatService] 设置服务器地址: " + serverBaseUrl);
    }

    // 获取完整的URL
    private String getFullUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // 如果已经是完整URL，直接返回
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        // 如果是相对路径，添加服务器地址
        String fullUrl = serverBaseUrl + (url.startsWith("/") ? url : "/" + url);
        System.out.println("[ChatService] 转换相对路径: " + url + " -> " + fullUrl);
        return fullUrl;
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        String name = fileName.toLowerCase();
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

    // ========== 文本消息相关方法 ==========

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
                // 私聊文件接收消息
                else if (MessageType.FILE_PRIVATE_RECEIVE.equals(type)) {
                    handleFileMessage(jsonObject);
                }
                // 群聊文件接收消息
                else if (MessageType.FILE_GROUP_RECEIVE.equals(type)) {
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
     * 处理文件消息（只处理接收，不处理上传）
     */
    private void handleFileMessage(JsonObject fileMessage) {
        try {
            String type = fileMessage.get("type").getAsString();

            // 处理私聊文件接收消息
            if (MessageType.FILE_PRIVATE_RECEIVE.equals(type)) {
                // 可以直接反序列化为FilePrivateReceive对象
                FilePrivateReceive receiveMessage = gson.fromJson(fileMessage, FilePrivateReceive.class);

                // 广播私聊文件消息
                broadcastPrivateFileMessage(receiveMessage);

                System.out.println("[ChatService] 处理私聊文件消息: " + receiveMessage.getFileName() +
                        " (" + formatFileSize(receiveMessage.getFileSize()) + ")");
            }
            // 处理群聊文件接收消息
            else if (MessageType.FILE_GROUP_RECEIVE.equals(type)) {
                // 可以直接反序列化为FileGroupReceive对象
                FileGroupReceive receiveMessage = gson.fromJson(fileMessage, FileGroupReceive.class);

                // 广播群聊文件消息
                broadcastGroupFileMessage(receiveMessage);

                System.out.println("[ChatService] 处理群聊文件消息: " + receiveMessage.getFileName() +
                        " (" + formatFileSize(receiveMessage.getFileSize()) + ")");
            }

        } catch (Exception e) {
            System.err.println("处理文件消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 广播私聊文件消息
     */
    private void broadcastPrivateFileMessage(FilePrivateReceive message) {
        try {
            // 通知接收方
            String senderName = "用户" + message.getSenderId();

            // 将文件消息对象序列化为JSON字符串
            String fileContent = gson.toJson(message);

            broadcaster.broadcastPrivateMessage(
                    message.getSenderId(),
                    message.getReceiverId(),
                    fileContent, // 将文件消息对象序列化为JSON字符串
                    message.getTimestamp(),
                    senderName,
                    message.getMessageId()
            );

        } catch (Exception e) {
            System.err.println("广播私聊文件消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 广播群聊文件消息
     */
    private void broadcastGroupFileMessage(FileGroupReceive message) {
        try {
            // 通知群聊成员
            String groupName = "群聊" + message.getGroupId();

            // 将文件消息对象序列化为JSON字符串
            String fileContent = gson.toJson(message);

            broadcaster.broadcastGroupMessage(
                    message.getGroupId(),
                    message.getSenderId(),
                    fileContent,
                    message.getTimestamp(),
                    groupName,
                    message.getMessageId()
            );

        } catch (Exception e) {
            System.err.println("广播群聊文件消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 文件下载功能 ==========

    /**
     * 下载文件
     */
    public void downloadFile(Window window, SocketClient client, Long userId,
                             String fileId, String fileName, String chatType,
                             Long targetId, Runnable onSuccess) {
        if (fileId == null || fileId.isEmpty()) {
            Platform.runLater(() -> showError(window, "错误", "文件ID不能为空"));
            return;
        }

        System.out.println("[ChatService] 开始下载文件:");
        System.out.println("  - fileId: " + fileId);
        System.out.println("  - fileName: " + fileName);
        System.out.println("  - chatType: " + chatType);
        System.out.println("  - userId: " + userId);
        System.out.println("  - targetId: " + targetId);

        // 1. 创建下载请求 - 确保使用正确的类型
        FileDownloadRequest downloadRequest = new FileDownloadRequest();
        downloadRequest.setFileId(fileId);
        downloadRequest.setUserId(userId);
        downloadRequest.setChatType(chatType); // "private" 或 "group"
        downloadRequest.setFileName(fileName);

        // 设置目标ID
        if ("private".equals(chatType) && targetId != null) {
            downloadRequest.setContactId(targetId);
        } else if ("group".equals(chatType) && targetId != null) {
            downloadRequest.setGroupId(targetId);
        }

        // 调试：打印请求内容
        System.out.println("[ChatService] 创建下载请求:");
        System.out.println("  - type: " + downloadRequest.getType()); // 应该是 "file_download_request"
        System.out.println("  - chatType: " + downloadRequest.getChatType());
        System.out.println("  - fileId: " + downloadRequest.getFileId());
        System.out.println("  - fileName: " + downloadRequest.getFileName());

        if (downloadRequest.getGroupId() != null) {
            System.out.println("  - groupId: " + downloadRequest.getGroupId());
        }
        if (downloadRequest.getContactId() != null) {
            System.out.println("  - contactId: " + downloadRequest.getContactId());
        }

        // 2. 发送下载请求
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    showInfo(window, "下载开始", "正在获取文件下载链接...");
                });

                // 发送下载请求
                String response = client.sendFileDownloadRequest(downloadRequest);
                System.out.println("[ChatService] 收到服务器响应: " + response);

                if (response == null) {
                    Platform.runLater(() -> showError(window, "下载失败", "无法连接到服务器"));
                    return;
                }

                // 解析响应
                JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();

                // 检查响应类型
                String responseType = jsonResponse.has("type") ? jsonResponse.get("type").getAsString() : "unknown";
                System.out.println("[ChatService] 响应类型: " + responseType);

                if (!jsonResponse.get("success").getAsBoolean()) {
                    String errorMsg = jsonResponse.get("message").getAsString();
                    System.err.println("[ChatService] 下载失败: " + errorMsg);
                    Platform.runLater(() -> showError(window, "下载失败", errorMsg));
                    return;
                }

                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();
                String actualFileName = jsonResponse.has("fileName") ?
                        jsonResponse.get("fileName").getAsString() : fileName;

                System.out.println("[ChatService] 服务器返回信息:");
                System.out.println("  - 原始下载链接: " + downloadUrl);
                System.out.println("  - 文件名: " + actualFileName);

                // 3. 显示下载成功信息
                Platform.runLater(() -> {
                    showInfo(window, "下载成功", "已获取文件下载链接\n点击确定开始下载文件: " + actualFileName);

                    // 调用下载文件方法
                    downloadFileFromUrl(window, downloadUrl, actualFileName, onSuccess);
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("[ChatService] 下载异常: " + e.getMessage());
                Platform.runLater(() -> showError(window, "下载异常", e.getMessage()));
            }
        }).start();
    }

    /**
     * 从URL下载文件
     */
    private void downloadFileFromUrl(Window window, String downloadUrl, String fileName, Runnable onSuccess) {
        try {
            // 创建文件保存对话框
            java.io.File initialFile = new java.io.File(fileName);
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存文件");
            fileChooser.setInitialFileName(initialFile.getName());

            // 设置文件类型过滤器
            String extension = getFileExtension(fileName);
            if (!extension.isEmpty()) {
                fileChooser.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter(extension.toUpperCase() + " 文件", "*." + extension)
                );
            }
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("所有文件", "*.*")
            );

            // 显示保存对话框
            java.io.File saveFile = fileChooser.showSaveDialog(window);
            if (saveFile == null) {
                // 用户取消了保存
                return;
            }

            // 在新线程中下载文件
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        showInfo(window, "下载开始", "正在下载文件: " + fileName);
                    });

                    // 关键：构建完整的下载URL
                    String fullDownloadUrl = buildDownloadUrl(downloadUrl);
                    System.out.println("[ChatService] 下载链接转换: " + downloadUrl + " -> " + fullDownloadUrl);

                    URL url = new URL(fullDownloadUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);

                    // 检查响应码
                    int responseCode = connection.getResponseCode();
                    System.out.println("[ChatService] 服务器响应码: " + responseCode);
                    if (responseCode != 200) {
                        Platform.runLater(() -> {
                            showError(window, "下载失败", "服务器返回错误码: " + responseCode);
                        });
                        return;
                    }

                    // 获取文件大小
                    long fileSize = connection.getContentLengthLong();
                    System.out.println("[ChatService] 文件大小: " + fileSize + " bytes");

                    // 创建输入流和输出流
                    try (java.io.InputStream inputStream = connection.getInputStream();
                         java.io.FileOutputStream outputStream = new java.io.FileOutputStream(saveFile)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytesRead = 0;

                        // 读取并写入文件
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            // 可以在这里添加下载进度更新
                            if (fileSize > 0) {
                                int progress = (int) ((totalBytesRead * 100) / fileSize);
                                System.out.println("[ChatService] 下载进度: " + progress + "%");
                            }
                        }

                        System.out.println("[ChatService] 下载完成，总字节: " + totalBytesRead);
                    }

                    // 关闭连接
                    connection.disconnect();

                    // 下载成功
                    Platform.runLater(() -> {
                        String message = String.format("文件下载完成\n保存位置: %s\n文件大小: %s",
                                saveFile.getAbsolutePath(), formatFileSize(saveFile.length()));

                        showInfo(window, "下载完成", message);

                        // 调用成功回调
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        showError(window, "下载失败", "下载过程中出现错误: " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError(window, "错误", "创建下载任务失败: " + e.getMessage()));
        }
    }

    /**
     * 构建完整的下载URL（关键方法）
     */
    private String buildDownloadUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("下载URL不能为空");
        }

        // 如果已经是完整URL，直接返回
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }

        // 清理路径：移除开头的斜杠
        String cleanUrl = relativeUrl;
        if (cleanUrl.startsWith("/")) {
            cleanUrl = cleanUrl.substring(1);
        }

        // 添加服务器前缀
        String fullUrl = serverBaseUrl + "/" + cleanUrl;

        System.out.println("[ChatService] 构建下载URL: ");
        System.out.println("  - 原始路径: " + relativeUrl);
        System.out.println("  - 清理后: " + cleanUrl);
        System.out.println("  - 服务器地址: " + serverBaseUrl);
        System.out.println("  - 完整URL: " + fullUrl);

        return fullUrl;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 简化版本：直接下载文件（不显示保存对话框）
     */
    public void downloadFileDirectly(Window window, SocketClient client, Long userId,
                                     String fileId, String fileName, String chatType,
                                     Long targetId, String savePath, Runnable onSuccess) {
        if (savePath == null || savePath.isEmpty()) {
            downloadFile(window, client, userId, fileId, fileName, chatType, targetId, onSuccess);
            return;
        }

        // 创建下载请求
        FileDownloadRequest downloadRequest = new FileDownloadRequest();
        downloadRequest.setFileId(fileId);
        downloadRequest.setUserId(userId);
        downloadRequest.setChatType(chatType);
        downloadRequest.setFileName(fileName);

        if ("private".equals(chatType) && targetId != null) {
            downloadRequest.setContactId(targetId);
        } else if ("group".equals(chatType) && targetId != null) {
            downloadRequest.setGroupId(targetId);
        }

        new Thread(() -> {
            try {
                String response = client.sendFileDownloadRequest(downloadRequest);
                if (response == null) {
                    Platform.runLater(() -> showError(window, "下载失败", "无法连接到服务器"));
                    return;
                }

                JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
                if (!jsonResponse.get("success").getAsBoolean()) {
                    String errorMsg = jsonResponse.get("message").getAsString();
                    Platform.runLater(() -> showError(window, "下载失败", errorMsg));
                    return;
                }

                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();
                String actualFileName = jsonResponse.has("fileName") ?
                        jsonResponse.get("fileName").getAsString() : fileName;

                // 构建完整的保存路径
                java.io.File saveFile = new java.io.File(savePath, actualFileName);

                // 下载文件
                downloadFileFromUrlToPath(window, downloadUrl, saveFile, onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError(window, "下载异常", e.getMessage()));
            }
        }).start();
    }

    /**
     * 下载文件到指定路径
     */
    private void downloadFileFromUrlToPath(Window window, String downloadUrl,
                                           java.io.File saveFile, Runnable onSuccess) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    showInfo(window, "下载开始", "正在下载文件: " + saveFile.getName());
                });

                // 关键：构建完整的下载URL
                String fullDownloadUrl = buildDownloadUrl(downloadUrl);
                System.out.println("[ChatService] 下载到指定路径URL: " + downloadUrl + " -> " + fullDownloadUrl);

                URL url = new URL(fullDownloadUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                System.out.println("[ChatService] 服务器响应码: " + responseCode);
                if (responseCode != 200) {
                    Platform.runLater(() -> {
                        showError(window, "下载失败", "服务器返回错误码: " + responseCode);
                    });
                    return;
                }

                try (java.io.InputStream inputStream = connection.getInputStream();
                     java.io.FileOutputStream outputStream = new java.io.FileOutputStream(saveFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }

                    System.out.println("[ChatService] 下载完成，总字节: " + totalBytes);
                }

                connection.disconnect();

                Platform.runLater(() -> {
                    String message = String.format("文件下载完成\n保存位置: %s", saveFile.getAbsolutePath());
                    showInfo(window, "下载完成", message);

                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError(window, "下载失败", "下载过程中出现错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示确认对话框
     */
    private boolean showConfirm(Window window, String title, String message) {
        final boolean[] result = {false};
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(window);

            alert.showAndWait().ifPresent(response -> {
                result[0] = response == javafx.scene.control.ButtonType.OK;
            });
        });

        // 等待用户响应
        try {
            Thread.sleep(100); // 短暂等待对话框响应
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result[0];
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