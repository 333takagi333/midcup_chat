package com.chat.protocol;

/**
 * 私聊接收：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class ChatPrivateReceive {
    private String type;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String contentType;
    private String fileUrl;
    private Long fileSize;
    private String fileName;
    private long timestamp;
    private Long id;        // message.id
    private Integer isRead; // 0/1 当前用户是否已读

    public ChatPrivateReceive() {
        this.type = MessageType.CHAT_PRIVATE_RECEIVE;
    }

    public ChatPrivateReceive(Long fromUserId, Long toUserId, String content, String contentType, long timestamp) {
        this.type = MessageType.CHAT_PRIVATE_RECEIVE;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.content = content;
        this.contentType = contentType;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }
}

