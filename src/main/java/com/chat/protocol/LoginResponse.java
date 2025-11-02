package com.chat.protocol;

/**
 * 登录响应数据：服务器 -> 客户端（type = login_response）
 *
 * 仅包含四个字段：
 * - type：协议类型，固定为 MessageType.LOGIN_RESPONSE
 * - uid：用户唯一标识（用户 UID），用于后续会话标识
 * - success：是否登录成功（true=成功，false=失败）
 * - message：提示信息（成功可为欢迎语，失败用于错误提示）
 */
public class LoginResponse {
    private String type;     // 必填：应为 MessageType.LOGIN_RESPONSE
    private String uid;      // 可选：登录成功时返回的用户 UID
    private boolean success; // 必填：是否成功
    private String message;  // 可选：提示信息

    public LoginResponse() {}

    public LoginResponse(String type, String uid, boolean success, String message) {
        this.type = type;
        this.uid = uid;
        this.success = success;
        this.message = message;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
