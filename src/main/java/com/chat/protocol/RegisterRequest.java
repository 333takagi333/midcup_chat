package com.chat.protocol;

/**
 * 用户注册请求：客户端 -> 服务器
 */
public class RegisterRequest {
    private String type = MessageType.REGISTER_REQUEST;
    private String username;
    private String password; // 明文密码，服务端进行加盐哈希

    public RegisterRequest() {}

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

