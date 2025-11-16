package com.chat.model;

/**
 * 联系人列表项模型，对应单个好友。
 */
public class FriendItem {
    private final String userId;
    private final String username;
    private final String status;      // 在线状态/描述
    private final String avatarUrl;   // 头像 URL
    private final String signature;   // 个性签名

    public FriendItem(String userId, String username, String status,
                      String avatarUrl, String signature) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.avatarUrl = avatarUrl;
        this.signature = signature;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getStatus() { return status; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getSignature() { return signature; }
}

