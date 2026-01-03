package com.chat.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 聊天消息数据模型
 */
public class ChatMessageModel {
    public enum MessageType {
        TEXT,       // 文本消息
        FILE        // 文件消息
    }

    private MessageType type;
    private String messageId;
    private Long senderId;
    private String senderName;
    private String content;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileId;
    private long timestamp;
    private boolean isMyMessage;

    // 文本消息构造函数
    public ChatMessageModel(String messageId, Long senderId, String senderName,
                            String content, long timestamp, boolean isMyMessage) {
        this.type = MessageType.TEXT;
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.isMyMessage = isMyMessage;
    }

    // 文件消息构造函数
    public ChatMessageModel(String messageId, Long senderId, String senderName,
                            String fileName, Long fileSize, String fileType,
                            String fileId, long timestamp, boolean isMyMessage) {
        this.type = MessageType.FILE;
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.fileId = fileId;
        this.timestamp = timestamp;
        this.isMyMessage = isMyMessage;
    }

    // Getters
    public MessageType getType() { return type; }
    public String getMessageId() { return messageId; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public String getFileType() { return fileType; }
    public String getFileId() { return fileId; }
    public long getTimestamp() { return timestamp; }
    public boolean isMyMessage() { return isMyMessage; }

    // 获取格式化时间
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date(timestamp));
    }

    // 获取格式化文件大小
    public String getFormattedFileSize() {
        if (fileSize == null) return "未知大小";
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // 获取文件图标
    public String getFileIcon() {
        if (fileType == null) return "📎";
        switch (fileType.toLowerCase()) {
            case "image": return "🖼️";
            case "video": return "🎬";
            case "audio": return "🎵";
            case "document": return "📄";
            case "text": return "📝";
            case "archive": return "📦";
            default: return "📎";
        }
    }

    // 获取文件类型描述
    public String getFileTypeDescription() {
        if (fileType == null) return "文件";
        switch (fileType.toLowerCase()) {
            case "image": return "图片";
            case "video": return "视频";
            case "audio": return "音频";
            case "document": return "文档";
            case "text": return "文本";
            case "archive": return "压缩包";
            default: return "文件";
        }
    }

    @Override
    public String toString() {
        if (type == MessageType.TEXT) {
            return String.format("[%s] %s: %s", getFormattedTime(), senderName, content);
        } else {
            return String.format("[%s] %s: [文件] %s (%s)",
                    getFormattedTime(), senderName, fileName, getFormattedFileSize());
        }
    }
}