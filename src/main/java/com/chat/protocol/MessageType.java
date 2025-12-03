package com.chat.protocol;

/**
 * 协议类型常量：统一管理所有 JSON 消息的 type 值。
 * 禁止修改！！！
 */
public final class MessageType {
    private MessageType() {}

    // 用户相关
    public static final String REGISTER_REQUEST = "register_request";       // C -> S
    public static final String REGISTER_RESPONSE = "register_response";     // S -> C
    public static final String LOGIN_REQUEST = "login_request";             // C -> S
    public static final String LOGIN_RESPONSE = "login_response";           // S -> C
    public static final String USER_INFO_REQUEST = "user_info_request";     // C -> S
    public static final String USER_INFO_RESPONSE = "user_info_response";   // S -> C

    // 聊天相关
    public static final String CHAT_PRIVATE_SEND = "chat_private_send";       // C -> S
    public static final String CHAT_PRIVATE_RECEIVE = "chat_private_receive"; // S -> C
    public static final String CHAT_GROUP_SEND = "chat_group_send";           // C -> S
    public static final String CHAT_GROUP_RECEIVE = "chat_group_receive";     // S -> C
    public static final String CHAT_HISTORY_REQUEST = "chat_history_request"; // C -> S
    public static final String CHAT_HISTORY_RESPONSE = "chat_history_response";// S -> C

    // 好友系统
    public static final String FRIEND_ADD_REQUEST = "friend_add_request";       // C -> S
    public static final String FRIEND_ADD_RESPONSE = "friend_add_response";     // S -> C
    public static final String FRIEND_LIST_REQUEST = "friend_list_request";     // C -> S
    public static final String FRIEND_LIST_RESPONSE = "friend_list_response";   // S -> C

    // 群组列表
    public static final String GROUP_LIST_REQUEST = "group_list_request";     // C -> S
    public static final String GROUP_LIST_RESPONSE = "group_list_response";   // S -> C

    // 重置密码
    public static final String RESET_PASSWORD_REQUEST = "reset_password_request";
    public static final String RESET_PASSWORD_RESPONSE = "reset_password_response";

    //创建群聊
    public static final String GROUP_CREATE_REQUEST = "group_create_request";
    public static final String GROUP_CREATE_RESPONSE = "group_create_response";

    // 好友请求相关
    public static final String FRIEND_REQUEST_RECEIVE = "friend_request_receive";
    public static final String FRIEND_REQUEST_LIST_REQUEST = "friend_request_list_request";
    public static final String FRIEND_REQUEST_LIST_RESPONSE = "friend_request_list_response";
    public static final String FRIEND_REQUEST_RESPONSE = "friend_request_response";
}
