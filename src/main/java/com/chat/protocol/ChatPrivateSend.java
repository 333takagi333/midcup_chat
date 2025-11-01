package com.chat.protocol;

/**
 * 私聊发送：客户端 -> 服务器
 */
public class ChatPrivateSend {
    private String from;     // 发送方用户名
    private String to;       // 接收方用户名
    private String content;  // 文本内容
    private long timestamp;  // 客户端时间戳

    public ChatPrivateSend() {}

    public ChatPrivateSend(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
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

