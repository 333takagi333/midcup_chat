package com.chat.protocol;

/**
 * 更新资料响应：服务器 -> 客户端
 */
public class UpdateProfileResponse {
    private String type = "update_profile_response";
    private boolean success;
    private String message;
    private String avatarUrl; // 更新后的头像URL

    public UpdateProfileResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}