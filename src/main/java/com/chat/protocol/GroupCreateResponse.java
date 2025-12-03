package com.chat.protocol;

/**
 * 创建群聊响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class GroupCreateResponse {
    private String type = MessageType.GROUP_CREATE_RESPONSE;
    private boolean success;
    private String message;
    private Long groupId;
    private String groupName;

    public GroupCreateResponse() {}

    public GroupCreateResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}