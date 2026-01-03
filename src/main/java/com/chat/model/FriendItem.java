package com.chat.model;

/**
 * 联系人列表项模型，对应单个好友。
 */
public class FriendItem {
    private final String userId;
    private final String username;
    private final String avatarUrl;   // 头像 URL

    public FriendItem(String userId, String username, String status,
                      String avatarUrl, String signature) {
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
}

