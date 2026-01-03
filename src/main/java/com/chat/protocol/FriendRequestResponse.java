package com.chat.protocol;

/**
 * 好友请求响应：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class FriendRequestResponse {
    private String type = MessageType.FRIEND_REQUEST_RESPONSE;
    private Long requestId;
    private boolean accept; // true:接受, false:拒绝

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public boolean isAccept() { return accept; }
    public void setAccept(boolean accept) { this.accept = accept; }
}