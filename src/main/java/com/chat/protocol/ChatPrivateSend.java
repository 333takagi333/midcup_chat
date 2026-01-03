package com.chat.protocol;

/**
 * 私聊发送：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class ChatPrivateSend {
    private String type;     // 协议类型：MessageType.CHAT_PRIVATE_SEND
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String contentType;
    private String fileUrl;
    private Long fileSize;
    private String fileName;
    private long timestamp;  // 客户端时间戳

    public ChatPrivateSend() {
        this.type = MessageType.CHAT_PRIVATE_SEND;
    }

    public ChatPrivateSend(Long fromUserId, Long toUserId, String content, String contentType) {
        this.type = MessageType.CHAT_PRIVATE_SEND;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.content = content;
        this.contentType = contentType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
