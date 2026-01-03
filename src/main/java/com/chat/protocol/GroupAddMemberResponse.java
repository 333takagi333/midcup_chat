package com.chat.protocol;

import java.util.Date;

/**
 * 添加群成员响应协议
 */
public class GroupAddMemberResponse {
    private final String type = MessageType.GROUP_ADD_MEMBER_RESPONSE;
    private Long groupId;
    private Long targetUserId;
    private Long operatorId;
    private boolean success;
    private String message;
    private Long timestamp;

    public GroupAddMemberResponse() {
        this.timestamp = new Date().getTime();
    }

    public GroupAddMemberResponse(Long groupId, Long targetUserId, Long operatorId, boolean success, String message) {
        this.groupId = groupId;
        this.targetUserId = targetUserId;
        this.operatorId = operatorId;
        this.success = success;
        this.message = message;
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GroupAddMemberResponse{" +
                "type='" + type + '\'' +
                ", groupId=" + groupId +
                ", targetUserId=" + targetUserId +
                ", operatorId=" + operatorId +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}