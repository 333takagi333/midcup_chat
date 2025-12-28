package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.FilePrivateSend;
import com.chat.protocol.FileGroupSend;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件上传服务类 - 类似AvatarService的方式，通过Socket发送文件
 */
public class FileUploadService {

    /**
     * 选择并发送文件（类似AvatarService的简洁方式）
     */
    public static void uploadFile(Window ownerWindow, SocketClient socketClient,
                                  Long userId, Long contactId, Long groupId,
                                  String chatType, FileUploadCallback callback) {

        // 1. 选择文件
        File selectedFile = selectFile(ownerWindow);
        if (selectedFile == null) {
            return; // 用户取消了选择
        }

        // 2. 验证文件大小（最大50MB）
        long maxSizeMB = 50;
        if (!validateFileSize(selectedFile, maxSizeMB)) {
            showError(ownerWindow, "文件太大",
                    String.format("文件大小不能超过 %d MB", maxSizeMB));
            return;
        }

        // 3. 显示上传提示
        showInfo(ownerWindow, "处理中",
                String.format("正在处理文件: %s", selectedFile.getName()));

        // 4. 在新线程中发送文件
        new Thread(() -> {
            try {
                // 5. 将文件转换为Base64
                String base64Data = fileToBase64(selectedFile);
                if (base64Data == null) {
                    Platform.runLater(() -> {
                        showError(ownerWindow, "处理失败", "文件编码失败");
                    });
                    return;
                }

                System.out.printf("[FileUploadService] 文件处理成功: %s, 大小: %s, Base64长度: %d%n",
                        selectedFile.getName(),
                        FileService.formatFileSize(selectedFile.length()),
                        base64Data.length());

                // 6. 创建并发送文件消息
                boolean sent = sendFileMessage(socketClient, userId, contactId, groupId,
                        chatType, selectedFile, base64Data);

                if (sent) {
                    Platform.runLater(() -> {
                        showSuccess(ownerWindow, "发送成功",
                                String.format("文件已发送: %s", selectedFile.getName()));

                        // 回调成功
                        if (callback != null) {
                            callback.onUploadSuccess(new FileUploadResult(
                                    generateFileId(),
                                    selectedFile.getName(),
                                    selectedFile.length(),
                                    "", // 不需要URL
                                    FileService.getFileTypeCategory(selectedFile)
                            ));
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showError(ownerWindow, "发送失败", "文件发送失败，请检查网络连接");
                        if (callback != null) {
                            callback.onUploadFailure("网络发送失败");
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError(ownerWindow, "处理异常", e.getMessage());
                    if (callback != null) {
                        callback.onUploadFailure(e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * 选择文件（类似AvatarService.selectAvatarFile）
     */
    private static File selectFile(Window ownerWindow) {
        return FileService.chooseAndUploadFileSimplified(ownerWindow);
    }

    /**
     * 验证文件大小
     */
    private static boolean validateFileSize(File file, long maxSizeMB) {
        if (file == null) return false;
        long maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.length() <= maxSizeBytes;
    }

    /**
     * 将文件转换为Base64字符串（类似AvatarService.imageFileToBase64）
     */
    private static String fileToBase64(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileData = new byte[(int) file.length()];
            int bytesRead = fis.read(fileData);

            if (bytesRead != file.length()) {
                System.err.println("[FileUploadService] 文件读取不完整: " + bytesRead + "/" + file.length());
                return null;
            }

            String base64 = Base64.getEncoder().encodeToString(fileData);
            System.out.println("[FileUploadService] Base64编码成功");
            return base64;

        } catch (Exception e) {
            System.err.println("[FileUploadService] Base64编码失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送文件消息
     */
    private static boolean sendFileMessage(SocketClient socketClient, Long userId,
                                           Long contactId, Long groupId, String chatType,
                                           File file, String base64Data) {

        if (socketClient == null || !socketClient.isConnected()) {
            System.err.println("[FileUploadService] Socket未连接");
            return false;
        }

        try {
            if ("private".equals(chatType)) {
                // 发送私聊文件消息
                FilePrivateSend fileMessage = new FilePrivateSend();
                fileMessage.setFileId(generateFileId());
                fileMessage.setFileName(file.getName());
                fileMessage.setFileSize(file.length());
                fileMessage.setFileType(FileService.getFileTypeCategory(file));
                fileMessage.setSenderId(userId);
                fileMessage.setReceiverId(contactId);
                fileMessage.setDownloadUrl(base64Data); // 存储Base64数据

                System.out.println("[FileUploadService] 发送私聊文件消息: " + file.getName());
                return socketClient.sendPrivateFileMessage(fileMessage);

            } else if ("group".equals(chatType)) {
                // 发送群聊文件消息
                FileGroupSend fileMessage = new FileGroupSend();
                fileMessage.setFileId(generateFileId());
                fileMessage.setFileName(file.getName());
                fileMessage.setFileSize(file.length());
                fileMessage.setFileType(FileService.getFileTypeCategory(file));
                fileMessage.setSenderId(userId);
                fileMessage.setGroupId(groupId);
                fileMessage.setDownloadUrl(base64Data); // 存储Base64数据

                System.out.println("[FileUploadService] 发送群聊文件消息: " + file.getName());
                return socketClient.sendGroupFileMessage(fileMessage);
            }

            return false;

        } catch (Exception e) {
            System.err.println("[FileUploadService] 发送文件消息失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 生成简单的文件ID
     */
    private static String generateFileId() {
        return Long.toHexString(System.currentTimeMillis()) + "_" +
                Long.toHexString((long) (Math.random() * 1000000));
    }

    // ========== 对话框显示方法 ==========

    private static void showInfo(Window window, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(window);
            alert.show();
        });
    }

    private static void showError(Window window, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(window);
            alert.show();
        });
    }

    private static void showSuccess(Window window, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(window);
            alert.show();
        });
    }


    /**
     * 文件上传回调接口
     */
    public interface FileUploadCallback {
        void onUploadSuccess(FileUploadResult result);
        void onUploadFailure(String errorMessage);
    }

    /**
     * 文件上传结果类
     */
    public static class FileUploadResult {
        private final String fileId;
        private final String fileName;
        private final long fileSize;
        private final String downloadUrl;
        private final String fileType;

        public FileUploadResult(String fileId, String fileName, long fileSize,
                                String downloadUrl, String fileType) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.downloadUrl = downloadUrl;
            this.fileType = fileType;
        }

        public String getFileId() { return fileId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getFileType() { return fileType; }

        public String getFormattedFileSize() {
            return FileService.formatFileSize(fileSize);
        }
    }
}