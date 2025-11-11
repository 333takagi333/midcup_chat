package com.chat.protocol;

import java.util.List;

/**
 * 好友列表响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class FriendListResponse {
    private String type = MessageType.FRIEND_LIST_RESPONSE;
    private List<FriendItem> friends;

    public FriendListResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<FriendItem> getFriends() { return friends; }
    public void setFriends(List<FriendItem> friends) { this.friends = friends; }

    public static class FriendItem {
        private Long uid;
        private String username;
        private String avatarUrl;

        public Long getUid() { return uid; }
        public void setUid(Long uid) { this.uid = uid; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
}
