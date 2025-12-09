package com.chat.protocol;

/**
 * 退出群聊请求：客户端 -> 服务器
 */
public class ExitGroupRequest {
    private String type = MessageType.EXIT_GROUP_REQUEST;
    private Long groupId;       // 群ID
    private Long userId;        // 用户ID

    public ExitGroupRequest() {}

    public ExitGroupRequest(Long groupId, Long userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}