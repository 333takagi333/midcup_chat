package com.chat.protocol;

import java.util.List;

/**
 * 历史消息响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class ChatHistoryResponse {
    private String type = MessageType.CHAT_HISTORY_RESPONSE;
    private String chatType; // private / group
    private boolean success = true;
    private String message;

    private List<HistoryMessageItem> messages;

    public ChatHistoryResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<HistoryMessageItem> getMessages() { return messages; }
    public void setMessages(List<HistoryMessageItem> messages) { this.messages = messages; }

    // 映射 message 表
    public static class HistoryMessageItem {
        private Long id;
        private Long senderId;
        private Long receiverId; // 私聊时存在
        private Long groupId;    // 群聊时存在
        private String content;
        private String contentType = ContentType.TEXT;
        private String fileUrl;
        private Long fileSize;
        private String fileName;
        private String timestamp; // 改为String类型，接收DATETIME格式
        private Integer isRead; // 0/1

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
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
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public Integer getIsRead() { return isRead; }
        public void setIsRead(Integer isRead) { this.isRead = isRead; }
    }
}