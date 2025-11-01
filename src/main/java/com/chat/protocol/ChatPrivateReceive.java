package com.chat.protocol;

/**
 * 私聊接收：服务器 -> 客户端
 */
public class ChatPrivateReceive {
    private String from;     // 发送方用户名
    private String to;       // 接收方用户名（本客户端）
    private String content;  // 文本内容
    private long timestamp;  // 服务器时间戳或消息时间

    public ChatPrivateReceive() {}

    public ChatPrivateReceive(String from, String to, String content, long timestamp) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

