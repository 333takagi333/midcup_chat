package com.chat.model;

/**
 * 群聊列表项模型，对应单个群聊。
 */
public class GroupItem {
    private final String groupId;
    private final String name;
    private final String lastMessage;
    private final String memberCount;
    private final String avatarUrl;

    public GroupItem(String groupId, String name, String lastMessage,
                     String memberCount, String avatarUrl) {
        this.groupId = groupId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.memberCount = memberCount;
        this.avatarUrl = avatarUrl;
    }

    public String getGroupId() { return groupId; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getMemberCount() { return memberCount; }
    public String getAvatarUrl() { return avatarUrl; }
}
