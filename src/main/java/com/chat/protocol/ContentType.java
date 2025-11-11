package com.chat.protocol;

/**
 * 统一的消息内容类型常量，对应 message.content_type 字段。
 */
public final class ContentType {
    private ContentType() {}

    public static final String TEXT = "text";      // 纯文本
    public static final String IMAGE = "image";    // 图片
    public static final String FILE = "file";      // 通用文件
    public static final String AUDIO = "audio";    // 语音/音频（可选）
    public static final String VIDEO = "video";    // 视频（可选）
}

