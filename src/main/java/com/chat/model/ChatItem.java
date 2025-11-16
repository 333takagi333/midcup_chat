package com.chat.model;

/**
 * 会话列表项模型，对应单个私聊或群聊会话。
 */
public class ChatItem {
    private final String id;          // 私聊: 对方uid，群聊: groupId
    private final String name;        // 显示名称
    private final String lastMessage; // 最近一条消息预览
    private final String time;        // 最近消息时间（格式化后字符串）
    private final String avatarUrl;   // 头像 URL
    private final boolean unread;     // 是否有未读
    private final boolean group;      // 是否群聊

    public ChatItem(String id, String name, String lastMessage, String time,
                    String avatarUrl, boolean unread, boolean group) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.unread = unread;
        this.group = group;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isUnread() { return unread; }
    public boolean isGroup() { return group; }
}

