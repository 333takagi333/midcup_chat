package com.chat.protocol;

/**
 * 修改密码请求协议
 */
public class ChangePasswordRequest {
    private String userId;
    private String oldPassword;
    private String newPassword;

    // 默认构造函数（GSON需要）
    public ChangePasswordRequest() {
    }

    // 带参数的构造函数
    public ChangePasswordRequest(String userId, String oldPassword, String newPassword) {
        this.userId = userId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    // Getter 和 Setter 方法
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest{" +
                "userId='" + userId + '\'' +
                ", oldPassword='" + (oldPassword != null ? "[PROTECTED]" : "null") + '\'' +
                ", newPassword='" + (newPassword != null ? "[PROTECTED]" : "null") + '\'' +
                '}';
    }
}