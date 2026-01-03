package com.chat.protocol;

/**
 * 登录响应数据：服务器 -> 客户端（type = login_response）
 *
 * 字段：
 * - type：协议类型，固定为 MessageType.LOGIN_RESPONSE
 * - uid：用户唯一标识（用户 UID），用于后续会话标识
 * - success：是否登录成功（true=成功，false=失败）
 * - message：提示信息（成功可为欢迎语，失败用于错误提示）
 * - token：可选，会话令牌
 * - timestamp：响应时间戳
 * - avatarUrl：用户头像URL（新增）
 * - username：用户名（新增，用于显示）
 */
@SuppressWarnings("unused")
public class LoginResponse {
    private String type;     // 必填：应为 MessageType.LOGIN_RESPONSE
    private String uid;      // 可选：登录成功时返回的用户 UID
    private boolean success; // 必填：是否成功
    private String message;  // 可选：提示信息
    private String token;    // 可选：登录凭证/会话令牌
    private long timestamp;  // 响应时间戳
    private String avatarUrl; // 新增：用户头像URL
    private String username;  // 新增：用户名

    public LoginResponse() {}

    public LoginResponse(String type, String uid, boolean success, String message) {
        this.type = type;
        this.uid = uid;
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}