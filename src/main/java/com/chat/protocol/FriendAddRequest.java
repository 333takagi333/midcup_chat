package com.chat.protocol;

/**
 * 添加好友请求：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class FriendAddRequest {
    private String type = MessageType.FRIEND_ADD_REQUEST;
    private Long toUserId; // 目标用户ID

    public FriendAddRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
}
