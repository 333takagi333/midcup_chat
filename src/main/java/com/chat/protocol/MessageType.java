package com.chat.protocol;

/**
 * 协议类型常量：统一管理所有 JSON 消息的 type 值。
 */
public final class MessageType {
    private MessageType() {}

    // 用户相关
    // C -> S：客户端发起登录请求，请求数据参见 com.chat.protocol.LoginRequest
    public static final String LOGIN_REQUEST = "login_request";
    // S -> C：服务器返回登录结果，响应数据参见 com.chat.protocol.LoginResponse
    public static final String LOGIN_RESPONSE = "login_response";

    // 聊天相关（仅私聊，按当前阶段需求）
    // C -> S：客户端发送私聊消息（com.chat.protocol.ChatPrivateSend）
    public static final String CHAT_PRIVATE_SEND = "chat_private_send";       // C -> S
    // S -> C：服务器向目标客户端投递的私聊消息（com.chat.protocol.ChatPrivateReceive）
    public static final String CHAT_PRIVATE_RECEIVE = "chat_private_receive"; // S -> C
}
