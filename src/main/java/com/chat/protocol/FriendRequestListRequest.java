package com.chat.protocol;

/**
 * 好友请求列表请求：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class FriendRequestListRequest {
    private String type = MessageType.FRIEND_REQUEST_LIST_REQUEST;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}