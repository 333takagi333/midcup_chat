package com.chat.protocol;

/**
 * 协议类型常量：统一管理所有 JSON 消息的 type 值。
 */
public final class MessageType {
    private MessageType() {}

    // 用户相关
    public static final String LOGIN_REQUEST = "login_request";
    public static final String LOGIN_RESPONSE = "login_response";

    // 聊天相关（仅私聊，按当前阶段需求）
    public static final String CHAT_PRIVATE_SEND = "chat_private_send";       // C -> S
    public static final String CHAT_PRIVATE_RECEIVE = "chat_private_receive"; // S -> C
}

