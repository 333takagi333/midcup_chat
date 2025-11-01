package com.chat.model;

/**
 * 聊天消息数据模型
 */
public class ChatMessage {
    private String fromUser;
    private String toUser;
    private String content;
    private long timestamp;

    public ChatMessage(String fromUser, String toUser, String content) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}