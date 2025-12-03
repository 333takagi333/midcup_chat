package com.chat.protocol;

/**
 * 用户资料响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class UserInfoResponse {
    private String type = MessageType.USER_INFO_RESPONSE;
    private boolean success;
    private String message;

    // user_auth + user_profile 映射（脱敏）
    private Long uid;
    private String username;
    private String avatarUrl;
    private Integer gender;   // 0未知 1男 2女
    private String birthday;  // ISO 日期字符串，如 1990-01-01
    private String tele;
    private String secretKey;

    public UserInfoResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUid() { return uid; }
    public void setUid(Long uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getTele() { return tele; }
    public void setTele(String tele) { this.tele = tele; }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
