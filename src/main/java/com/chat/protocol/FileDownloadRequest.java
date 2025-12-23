package com.chat.protocol;

/**
 * 文件下载请求（简化版）
 */
public class FileDownloadRequest {
    private String type = MessageType.FILE_DOWNLOAD_REQUEST;
    private String fileId;        // 文件ID
    private Long userId;          // 请求下载的用户ID
    private String chatType;      // "private" 或 "group"

    // 构造方法
    public FileDownloadRequest() {
    }

    public FileDownloadRequest(String fileId, Long userId) {
        this.fileId = fileId;
        this.userId = userId;
    }

    // getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getChatType() {
        return chatType;
    }

    public void setChatType(String chatType) {
        this.chatType = chatType;
    }

    @Override
    public String toString() {
        return "FileDownloadRequest{" +
                "type='" + type + '\'' +
                ", fileId='" + fileId + '\'' +
                ", userId=" + userId +
                ", chatType='" + chatType + '\'' +
                '}';
    }
}