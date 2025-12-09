package com.chat.protocol;

/**
 * 更新群昵称请求：客户端 -> 服务器
 */
public class UpdateNicknameRequest {
    private String type = MessageType.UPDATE_NICKNAME_REQUEST;
    private Long groupId;       // 群ID
    private Long userId;        // 用户ID
    private String nickname;    // 新的昵称

    public UpdateNicknameRequest() {}

    public UpdateNicknameRequest(Long groupId, Long userId, String nickname) {
        this.groupId = groupId;
        this.userId = userId;
        this.nickname = nickname;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}