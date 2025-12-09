package com.chat.protocol;

/**
 * 好友详情请求：客户端 -> 服务器
 */
public class FriendDetailRequest {
    private String type = MessageType.FRIEND_DETAIL_REQUEST;
    private Long userId;        // 当前用户ID
    private Long friendId;      // 好友ID

    public FriendDetailRequest() {}

    public FriendDetailRequest(Long userId, Long friendId) {
        this.userId = userId;
        this.friendId = friendId;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }
}