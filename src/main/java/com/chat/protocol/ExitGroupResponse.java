package com.chat.protocol;

/**
 * 退出群聊响应：服务器 -> 客户端
 */
public class ExitGroupResponse {
    private String type = MessageType.EXIT_GROUP_RESPONSE;
    private boolean success;
    private String message;
    private Long groupId;
    private Long userId;

    public ExitGroupResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}