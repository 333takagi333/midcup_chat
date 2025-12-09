package com.chat.protocol;

/**
 * 群聊详情请求：客户端 -> 服务器
 */
public class GroupDetailRequest {
    private String type = MessageType.GROUP_DETAIL_REQUEST;
    private Long groupId;       // 群ID
    private Long userId;        // 当前用户ID

    public GroupDetailRequest() {}

    public GroupDetailRequest(Long groupId, Long userId) {
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