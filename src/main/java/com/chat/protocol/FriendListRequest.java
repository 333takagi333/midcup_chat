package com.chat.protocol;

/**
 * 好友列表请求：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class FriendListRequest {
    private String type = MessageType.FRIEND_LIST_REQUEST;

    public FriendListRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
