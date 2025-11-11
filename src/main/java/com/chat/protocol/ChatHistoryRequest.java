package com.chat.protocol;

/**
 * 历史消息请求：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class ChatHistoryRequest {
    private String type = MessageType.CHAT_HISTORY_REQUEST;

    // chatType = "private" 或 "group"
    private String chatType;

    // 私聊：目标用户名或 UID（建议 username，保持与在线管理一致）
    private Long targetUserId;

    // 群聊：目标群 ID
    private Long groupId;

    private Integer limit = 50;        // 返回条数，默认 50
    private Long beforeTimestamp;      // 拉取早于该时间的消息（用于翻页）

    public ChatHistoryRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Long getBeforeTimestamp() { return beforeTimestamp; }
    public void setBeforeTimestamp(Long beforeTimestamp) { this.beforeTimestamp = beforeTimestamp; }
}
