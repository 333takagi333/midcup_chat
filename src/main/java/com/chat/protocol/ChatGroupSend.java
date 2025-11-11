package com.chat.protocol;

/**
 * 群聊发送：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class ChatGroupSend {
    private String type = MessageType.CHAT_GROUP_SEND;
    private Long groupId;
    private Long fromUserId; // 发送者（可由服务端覆盖为会话用户）
    private String content;
    private String contentType = ContentType.TEXT;
    private String fileUrl;
    private Long fileSize;
    private String fileName;
    private long timestamp = System.currentTimeMillis(); // 客户端时间戳

    public ChatGroupSend() {}

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
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
