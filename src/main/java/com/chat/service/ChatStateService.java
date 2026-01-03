package com.chat.service;

import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import com.chat.network.SocketClient;
import com.chat.protocol.ChatGroupReceive;
import com.chat.protocol.ChatPrivateReceive;
import com.chat.protocol.FriendListResponse;
import com.chat.protocol.GroupListResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages the UI state, holding the observable lists for the views.
 */
public class ChatStateService implements MessageBroadcaster.ChatListUpdateListener {
    private final ObservableList<ChatItem> chatItems = FXCollections.observableArrayList();
    private final ObservableList<FriendItem> friendItems = FXCollections.observableArrayList();
    private final ObservableList<GroupItem> groupItems = FXCollections.observableArrayList();

    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final RecentMessageService recentService = RecentMessageService.getInstance();

    // 添加对FriendService和GroupService的引用
    private FriendService friendService;
    private GroupService groupService;

    public ChatStateService() {
        // 注册为聊天列表监听器
        broadcaster.registerChatListListener(this);
    }

    /**
     * 设置FriendService引用
     */
    public void setFriendService(FriendService friendService) {
        this.friendService = friendService;
    }

    /**
     * 设置GroupService引用
     */
    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    public ObservableList<ChatItem> getChatItems() {
        return chatItems;
    }

    public ObservableList<FriendItem> getFriendItems() {
        return friendItems;
    }

    public ObservableList<GroupItem> getGroupItems() {
        return groupItems;
    }

    @Override
    public void onNewPrivateMessage(Long contactId, String contactName, String content,
                                    long timestamp, Long messageId) {
        System.out.println("[ChatStateService] 收到私聊消息: " + contactName + ": " +
                content.substring(0, Math.min(20, content.length())));

        // 1. 首先查找发送方的真实用户名（维持原有逻辑）
        String senderRealName = findFriendName(contactId);
        if (senderRealName == null || senderRealName.isEmpty()) {
            senderRealName = contactName != null ? contactName : "用户" + contactId;
        }

        // 2. 查找发送方的头像（维持原有逻辑）
        String senderAvatar = findFriendAvatar(contactId);

        // 3. 判断消息是否来自当前用户
        Long currentUserId = broadcaster.getCurrentUserId();
        boolean isFromCurrentUser = currentUserId != null && currentUserId.equals(contactId);

        // 4. 构建显示文本 - 这里是关键修改！
        String displayText;
        String displayName;

        if (isFromCurrentUser) {
            // 当前用户发送的消息
            displayName = "我";
            // 关键：这里使用真实用户名，而不是"用户+ID"
            displayText = "我: " + getMessagePreview(content);

            // 对于当前用户发送的消息，聊天列表中应该显示对方的名称
            // 这里使用findFriendName获取对方的真实用户名
            displayName = findFriendName(contactId);
            if (displayName == null || displayName.isEmpty()) {
                displayName = contactName != null ? contactName : "用户" + contactId;
            }
        } else {
            // 对方发送的消息
            displayName = senderRealName;  // 使用真实用户名
            // 关键：这里使用真实用户名，而不是"用户+ID"
            displayText = senderRealName + ": " + getMessagePreview(content);
        }

        // 5. 如果未能获取到真实用户名，使用默认格式（备用逻辑）
        if (displayName == null || displayName.isEmpty()) {
            displayName = contactName != null ? contactName : "用户" + contactId;
        }

        System.out.println("[ChatStateService] 私聊消息显示 - 显示名称: " + displayName +
                ", 发送者显示: " + senderRealName +
                ", 头像: " + (senderAvatar != null ? "有" : "无"));

        // 6. 更新聊天列表
        upsertChat(
                contactId.toString(),
                displayName,      // 聊天列表显示的名称
                senderAvatar,     // 头像
                displayText,      // 消息预览（包含真实用户名）
                !isFromCurrentUser, // 只有对方发的消息才标记为未读
                false
        );
    }

    @Override
    public void onNewGroupMessage(Long groupId, String groupName, String content,
                                  long timestamp, Long messageId) {
        System.out.println("[ChatStateService] 收到群聊消息: " + groupName + ": " +
                content.substring(0, Math.min(20, content.length())));

        // 1. 查找群组真实名称和头像（维持原有逻辑）
        String groupRealName = findGroupName(groupId);
        if (groupRealName == null || groupRealName.isEmpty()) {
            groupRealName = groupName != null ? groupName : "群聊" + groupId;
        }

        String groupAvatar = findGroupAvatar(groupId);

        // 2. 尝试从消息内容中提取发送者信息
        // 注意：这里需要根据您的消息格式来解析
        String senderDisplayName = "群成员";
        String messagePreview = getMessagePreview(content);

        // 尝试解析JSON格式的消息（如果是文件消息等）
        if (content.startsWith("{") && content.contains("\"type\"")) {
            try {
                com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                com.google.gson.JsonObject json = parser.parse(content).getAsJsonObject();

                if (json.has("senderId")) {
                    Long senderId = json.get("senderId").getAsLong();
                    String foundName = findFriendName(senderId);
                    if (foundName != null && !foundName.isEmpty()) {
                        senderDisplayName = foundName;  // 使用真实用户名
                    } else {
                        senderDisplayName = "用户" + senderId;
                    }
                }
                messagePreview = "[文件]";
            } catch (Exception e) {
                // 不是有效的JSON，继续处理
                messagePreview = "[文件]";
            }
        }

        // 3. 构建显示文本 - 这里是关键修改！
        String displayText;
        Long currentUserId = broadcaster.getCurrentUserId();

        // 尝试判断是否是当前用户发送的消息
        if (currentUserId != null) {
            // 如果是文件消息，检查senderId
            if (content.startsWith("{") && content.contains("\"type\"")) {
                try {
                    com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                    com.google.gson.JsonObject json = parser.parse(content).getAsJsonObject();
                    if (json.has("senderId")) {
                        Long senderId = json.get("senderId").getAsLong();
                        if (currentUserId.equals(senderId)) {
                            senderDisplayName = "我";
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            // 普通消息：检查是否以"我:"开头
            else if (content.startsWith("我:")) {
                senderDisplayName = "我";
            }
        }

        // 关键：使用真实用户名显示
        displayText = senderDisplayName + ": " + messagePreview;

        System.out.println("[ChatStateService] 群聊消息显示 - 群组: " + groupRealName +
                ", 发送者: " + senderDisplayName);

        // 4. 更新聊天列表
        upsertChat(
                groupId.toString(),
                groupRealName,     // 群组名称
                groupAvatar,       // 群组头像
                displayText,       // 消息预览（包含真实用户名）
                true,              // 群聊消息默认标记为未读
                true
        );
    }

    /**
     * 获取消息预览（截取前30个字符）
     */
    private String getMessagePreview(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 如果是文件消息，特殊处理
        if (content.startsWith("{") && content.contains("\"type\":\"file\"")) {
            return "[文件]";
        }

        // 如果是JSON格式的其他消息，也可能需要特殊处理
        if (content.startsWith("{") && content.contains("\"type\"")) {
            try {
                com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                com.google.gson.JsonObject json = parser.parse(content).getAsJsonObject();
                String type = json.has("type") ? json.get("type").getAsString() : "";

                switch (type.toLowerCase()) {
                    case "file":
                        return "[文件]";
                    case "image":
                        return "[图片]";
                    case "audio":
                        return "[语音]";
                    case "video":
                        return "[视频]";
                    default:
                        return "[消息]";
                }
            } catch (Exception e) {
                // 不是有效的JSON，继续处理
            }
        }

        // 普通文本消息，截取预览
        if (content.length() > 30) {
            return content.substring(0, 30) + "...";
        } else {
            return content;
        }
    }
    /**
     * 从好友列表中查找好友名称
     */
    private String findFriendName(Long friendId) {
        if (friendId == null || friendItems == null) {
            return null;
        }

        for (FriendItem friend : friendItems) {
            try {
                Long itemId = Long.parseLong(friend.getUserId());
                if (itemId.equals(friendId)) {
                    return friend.getUsername();
                }
            } catch (NumberFormatException e) {
                // ID格式不匹配，继续查找
            }
        }

        // 如果好友列表中没有找到，尝试使用FriendService（如果可用）
        if (friendService != null) {
            // 这里可以添加通过FriendService查找好友信息的逻辑
            // 但通常好友列表已经包含了所有好友信息
        }

        return null;
    }

    /**
     * 从好友列表中查找好友头像
     */
    private String findFriendAvatar(Long friendId) {
        if (friendId == null || friendItems == null) {
            return null;
        }

        for (FriendItem friend : friendItems) {
            try {
                Long itemId = Long.parseLong(friend.getUserId());
                if (itemId.equals(friendId)) {
                    return friend.getAvatarUrl();
                }
            } catch (NumberFormatException e) {
                // ID格式不匹配，继续查找
            }
        }

        return null;
    }

    /**
     * 从群组列表中查找群组名称
     */
    private String findGroupName(Long groupId) {
        if (groupId == null || groupItems == null) {
            return null;
        }

        for (GroupItem group : groupItems) {
            try {
                Long itemId = Long.parseLong(group.getGroupId());
                if (itemId.equals(groupId)) {
                    return group.getName();
                }
            } catch (NumberFormatException e) {
                // ID格式不匹配，继续查找
            }
        }

        return null;
    }

    /**
     * 从群组列表中查找群组头像
     */
    private String findGroupAvatar(Long groupId) {
        if (groupId == null || groupItems == null) {
            return null;
        }

        for (GroupItem group : groupItems) {
            try {
                Long itemId = Long.parseLong(group.getGroupId());
                if (itemId.equals(groupId)) {
                    return group.getAvatarUrl();
                }
            } catch (NumberFormatException e) {
                // ID格式不匹配，继续查找
            }
        }

        return null;
    }

    /**
     * 初始化消息栏（应用启动时调用）
     */
    public void initializeRecentMessages() {
        // 从 RecentMessageService 加载所有最近消息
        List<ChatItem> recentMessages = recentService.getAllRecentMessages();
        if (recentMessages != null && !recentMessages.isEmpty()) {
            chatItems.setAll(recentMessages);
            System.out.println("[ChatStateService] 初始化消息栏: " + recentMessages.size() + " 条记录");
        }
    }

    /**
     * 从服务器加载好友列表并更新
     */
    public void loadFriendsFromServer(SocketClient client, FriendService friendService) {
        if (client == null || !client.isConnected()) return;

        List<FriendItem> friends = friendService.loadFriends(client);
        if (friends != null) {
            friendItems.setAll(friends);
        }
    }

    /**
     * 从服务器加载群组列表并更新
     */
    public void loadGroupsFromServer(SocketClient client, GroupService groupService) {
        if (client == null || !client.isConnected()) return;

        List<GroupItem> groups = groupService.loadGroups(client);
        if (groups != null) {
            groupItems.setAll(groups);
        }
    }

    /**
     * 处理私聊消息
     */
    public void handlePrivateMessage(ChatPrivateReceive message) {
        if (message != null) {
            // 查找发送者信息
            String senderName = findFriendName(message.getFromUserId());
            String avatarUrl = findFriendAvatar(message.getFromUserId());

            if (senderName == null) {
                senderName = "用户" + message.getFromUserId();
            }

            String displayText = senderName + ": " + message.getContent();

            upsertChat(
                    message.getFromUserId().toString(),
                    senderName,
                    avatarUrl,
                    displayText,
                    true,
                    false
            );
        }
    }

    /**
     * 处理群聊消息
     */
    public void handleGroupMessage(ChatGroupReceive message) {
        if (message != null) {
            // 查找群组信息
            String groupName = findGroupName(message.getGroupId());
            String avatarUrl = findGroupAvatar(message.getGroupId());

            if (groupName == null) {
                groupName = "群聊" + message.getGroupId();
            }

            // 查找发送者信息
            String senderName = findFriendName(message.getFromUserId());
            if (senderName == null) {
                senderName = "用户" + message.getFromUserId();
            }

            String displayText = senderName + ": " + message.getContent();

            upsertChat(
                    message.getGroupId().toString(),
                    groupName,
                    avatarUrl,
                    displayText,
                    true,
                    true
            );
        }
    }

    public void updateFriendList(List<FriendListResponse.FriendItem> friends) {
        friendItems.clear();
        if (friends != null) {
            for (FriendListResponse.FriendItem friend : friends) {
                friendItems.add(new FriendItem(
                        friend.getUid() != null ? friend.getUid().toString() : "0",
                        friend.getUsername() != null ? friend.getUsername() : "未知用户",
                        "在线",
                        friend.getAvatarUrl(),
                        ""
                ));
            }
        }
    }

    public void updateGroupList(List<GroupListResponse.GroupItem> groups) {
        groupItems.clear();
        if (groups != null) {
            for (GroupListResponse.GroupItem group : groups) {
                groupItems.add(new GroupItem(
                        group.getId() != null ? group.getId().toString() : "0",
                        group.getName() != null ? group.getName() : "未知群组",
                        "暂无消息",
                        "0",
                        group.getAvatar()
                ));
            }
        }
    }

    public void upsertChat(String id, String name, String avatar, String lastMessage, boolean unread, boolean isGroup) {
        int foundIndex = -1;
        for (int i = 0; i < chatItems.size(); i++) {
            ChatItem item = chatItems.get(i);
            if (item.getId().equals(id) && item.isGroup() == isGroup) {
                foundIndex = i;
                break;
            }
        }

        String displayName = (name != null) ? name : (isGroup ? "群聊" + id : "用户" + id);
        String avatarUrl = avatar != null ? avatar : "";
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String messageText = lastMessage != null ? lastMessage : "";

        ChatItem newItem = new ChatItem(id, displayName, messageText, time, avatarUrl, unread, isGroup);

        if (foundIndex >= 0) {
            chatItems.remove(foundIndex);
            chatItems.add(0, newItem);
        } else {
            chatItems.add(0, newItem);
        }

        System.out.println("[ChatStateService] 更新聊天列表: " + displayName + " - " +
                messageText.substring(0, Math.min(20, messageText.length())));
    }

    /**
     * 清空所有数据
     */
    public void clearAll() {
        chatItems.clear();
        friendItems.clear();
        groupItems.clear();
        System.out.println("[ChatStateService] 已清空所有数据");
    }

    /**
     * 清理时取消注册
     */
    public void cleanup() {
        broadcaster.unregisterChatListListener(this);
        clearAll();
        System.out.println("[ChatStateService] 已清理并取消注册");
    }
}