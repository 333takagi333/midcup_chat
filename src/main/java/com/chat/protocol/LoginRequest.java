package com.chat.protocol;

/**
 * 登录请求数据
 */
@SuppressWarnings("unused")
public class LoginRequest {
    private String type;     // 协议类型：MessageType.LOGIN_REQUEST
    private Long uid;        // 修改：uid改为Long类型
    private String password;

    public LoginRequest() {
        this.type = MessageType.LOGIN_REQUEST;
    }

    // 构造函数，接收String类型的uid，内部转换为Long
    public LoginRequest(String uid, String password) {
        this.type = MessageType.LOGIN_REQUEST;
        try {
            this.uid = Long.parseLong(uid);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("UID必须是数字: " + uid);
        }
        this.password = password;
    }

    // 构造函数，直接接收Long类型的uid
    public LoginRequest(Long uid, String password) {
        this.type = MessageType.LOGIN_REQUEST;
        this.uid = uid;
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}