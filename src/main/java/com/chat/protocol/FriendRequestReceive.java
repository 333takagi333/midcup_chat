package com.chat.protocol;

/**
 * 好友请求接收：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class FriendRequestReceive {
    private String type = MessageType.FRIEND_REQUEST_RECEIVE;
    private Long requestId;
    private Long fromUserId;
    private String fromUsername;
    private String requestTime;

    public FriendRequestReceive() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getRequestTime() { return requestTime; }
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
}