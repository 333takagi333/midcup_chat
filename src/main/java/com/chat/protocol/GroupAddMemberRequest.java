package com.chat.protocol;

import java.util.Date;

/**
 * 添加群成员请求协议
 */
public class GroupAddMemberRequest {
    private final String type = MessageType.GROUP_ADD_MEMBER_REQUEST;
    private Long groupId;
    private Long targetUserId;    // 要添加的用户ID
    private Long operatorId;      // 操作者ID（当前用户）
    private Long timestamp;

    public GroupAddMemberRequest() {
        this.timestamp = new Date().getTime();
    }

    public GroupAddMemberRequest(Long groupId, Long targetUserId, Long operatorId) {
        this.groupId = groupId;
        this.targetUserId = targetUserId;
        this.operatorId = operatorId;
        this.timestamp = new Date().getTime();
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GroupAddMemberRequest{" +
                "type='" + type + '\'' +
                ", groupId=" + groupId +
                ", targetUserId=" + targetUserId +
                ", operatorId=" + operatorId +
                ", timestamp=" + timestamp +
                '}';
    }
}