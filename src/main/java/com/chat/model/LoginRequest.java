package com.chat.model;

/**
 * 登录请求数据模型，封装用户名与加密后的密码。
 */
public class LoginRequest {
    private String username;
    private String password;

    /**
     * 构造函数。
     * @param username 用户名
     * @param password 加密后的密码
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * 获取用户名。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取加密后的密码。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置加密后的密码。
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
