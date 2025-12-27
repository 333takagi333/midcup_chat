package com.chat.protocol;

/**
 * 群聊文件接收消息
 */
public class FileGroupReceive {
    private String type = MessageType.FILE_GROUP_RECEIVE;
    private String fileId;
    private String fileName;
    private long fileSize;
    private String fileType;
    private Long senderId;
    private Long groupId;
    private String downloadUrl;
    private long timestamp;
    private Long messageId; // 可选：服务器生成的消息ID

    // 构造方法
    public FileGroupReceive() {
        this.timestamp = System.currentTimeMillis();
    }

    public FileGroupReceive(String fileId, String fileName, long fileSize,
                            Long senderId, Long groupId, String downloadUrl) {
        this();
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.senderId = senderId;
        this.groupId = groupId;
        this.downloadUrl = downloadUrl;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return "FileGroupReceive{" +
                "type='" + type + '\'' +
                ", fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", senderId=" + senderId +
                ", groupId=" + groupId +
                ", downloadUrl='" + (downloadUrl != null ? "***" : "null") + '\'' +
                ", timestamp=" + timestamp +
                ", messageId=" + messageId +
                '}';
    }
}