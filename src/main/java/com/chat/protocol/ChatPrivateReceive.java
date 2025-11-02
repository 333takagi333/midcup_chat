package com.chat.protocol;

import com.chat.protocol.MessageType;

/**
 * 私聊接收：服务器 -> 客户端
 */
public class ChatPrivateReceive {
    private String type;     // 协议类型：MessageType.CHAT_PRIVATE_RECEIVE
    private String from;     // 发送方用户名
    private String to;       // 接收方用户名（本客户端）
    private String content;  // 文本内容
    private long timestamp;  // 服务器时间戳或消息时间

    public ChatPrivateReceive() {
        this.type = MessageType.CHAT_PRIVATE_RECEIVE;
    }

    public ChatPrivateReceive(String from, String to, String content, long timestamp) {
        this.type = MessageType.CHAT_PRIVATE_RECEIVE;
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
