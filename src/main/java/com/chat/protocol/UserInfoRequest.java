package com.chat.protocol;

/**
 * 用户资料请求：客户端 -> 服务器
 */
public class UserInfoRequest {
    private String type = MessageType.USER_INFO_REQUEST;
    private Long userId; // 目标用户 UID；为空时可表示查询当前用户

    public UserInfoRequest() {}

    public UserInfoRequest(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

