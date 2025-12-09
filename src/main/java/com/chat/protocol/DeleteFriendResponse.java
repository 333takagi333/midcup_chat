package com.chat.protocol;

/**
 * 删除好友响应：服务器 -> 客户端
 */
public class DeleteFriendResponse {
    private String type = MessageType.DELETE_FRIEND_RESPONSE;
    private boolean success;
    private String message;
    private Long userId;
    private Long friendId;

    public DeleteFriendResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }
}