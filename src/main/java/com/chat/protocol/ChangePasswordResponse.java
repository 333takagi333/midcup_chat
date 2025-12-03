package com.chat.protocol;

/**
 * 修改密码响应协议
 */
public class ChangePasswordResponse {
    private boolean success;
    private String message;
    private String timestamp;

    // 默认构造函数（GSON需要）
    public ChangePasswordResponse() {
    }

    // 带参数的构造函数
    public ChangePasswordResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    // Getter 和 Setter 方法
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChangePasswordResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}