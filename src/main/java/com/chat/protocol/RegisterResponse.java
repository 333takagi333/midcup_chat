package com.chat.protocol;

/**
 * 用户注册响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class RegisterResponse {
    private String type = MessageType.REGISTER_RESPONSE;
    private boolean success;
    private String message; // 错误或提示信息
    private Long uid;       // 注册成功后生成的 UID

    public RegisterResponse() {}

    public RegisterResponse(boolean success, String message, Long uid) {
        this.success = success;
        this.message = message;
        this.uid = uid;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUid() { return uid; }
    public void setUid(Long uid) { this.uid = uid; }
}
