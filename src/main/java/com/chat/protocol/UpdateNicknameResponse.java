package com.chat.protocol;

/**
 * 更新群昵称响应：服务器 -> 客户端
 */
public class UpdateNicknameResponse {
    private String type = MessageType.UPDATE_NICKNAME_RESPONSE;
    private boolean success;
    private String message;
    private Long groupId;
    private Long userId;
    private String nickname;

    public UpdateNicknameResponse() {}

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

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}