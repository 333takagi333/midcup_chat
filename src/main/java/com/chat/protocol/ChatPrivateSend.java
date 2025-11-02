package com.chat.protocol;

import com.chat.protocol.MessageType;

/**
 * 私聊发送：客户端 -> 服务器
 */
public class ChatPrivateSend {
    private String type;     // 协议类型：MessageType.CHAT_PRIVATE_SEND
    private String from;     // 发送方用户名
    private String to;       // 接收方用户名
    private String content;  // 文本内容
    private long timestamp;  // 客户端时间戳

    public ChatPrivateSend() {
        this.type = MessageType.CHAT_PRIVATE_SEND;
    }

    public ChatPrivateSend(String from, String to, String content) {
        this.type = MessageType.CHAT_PRIVATE_SEND;
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
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
