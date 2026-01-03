package com.chat.protocol;

/**
 * 群聊接收：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class ChatGroupReceive {
    private String type = MessageType.CHAT_GROUP_RECEIVE;
    private Long groupId;
    private Long fromUserId;
    private String content;
    private String contentType = ContentType.TEXT;

    private String fileUrl;
    private Long fileSize;
    private String fileName;

    private long timestamp; // 服务器消息时间

    private Long id;          // message.id
    private Integer isRead;   // 0/1 当前用户是否已读（可选）

    public ChatGroupReceive() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
}
