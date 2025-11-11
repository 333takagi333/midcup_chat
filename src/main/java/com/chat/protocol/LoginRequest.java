package com.chat.protocol;

/**
 * 登录请求数据
 */
@SuppressWarnings("unused")
public class LoginRequest {
    private String type;     // 协议类型：MessageType.LOGIN_REQUEST
    private String username;
    private String password;

    public LoginRequest() {
        this.type = MessageType.LOGIN_REQUEST;
    }

    public LoginRequest(String username, String password) {
        this.type = MessageType.LOGIN_REQUEST;
        this.username = username;
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
