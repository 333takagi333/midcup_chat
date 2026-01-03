package com.chat.protocol;

/**
 * 添加好友响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class FriendAddResponse {
    private String type = MessageType.FRIEND_ADD_RESPONSE;
    private boolean success;
    private String message;
    private Long requestId; // friend_request.id
    private Integer status; // 0待处理 1同意 2拒绝

    public FriendAddResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
