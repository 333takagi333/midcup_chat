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

    public ChatStateService() {
        // 注册为聊天列表监听器
        broadcaster.registerChatListListener(this);
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

        // 这里不再直接更新最近消息服务（由MessageBroadcaster处理）
        // 只更新聊天列表UI

        // 截取预览消息
        String preview = content.length() > 30 ?
                content.substring(0, 30) + "..." : content;

        // 获取当前用户ID（从MessageBroadcaster）
        Long currentUserId = broadcaster.getCurrentUserId();

        // 判断消息是否来自当前用户
        boolean isFromCurrentUser = messageId != null &&
                currentUserId != null && currentUserId.equals(contactId);

        String displayText;
        if (isFromCurrentUser) {
            // 当前用户发送的消息，显示"我: 内容"
            displayText = "我: " + preview;
        } else {
            // 对方发送的消息，显示"对方: 内容"
            displayText = contactName + ": " + preview;
        }

        // 更新聊天列表
        upsertChat(
                contactId.toString(),
                contactName,
                null,
                displayText,
                true, // 有未读消息
                false
        );
    }

    @Override
    public void onNewGroupMessage(Long groupId, String groupName, String content,
                                  long timestamp, Long messageId) {
        System.out.println("[ChatStateService] 收到群聊消息: " + groupName + ": " +
                content.substring(0, Math.min(20, content.length())));

        // 截取预览消息
        String preview = content.length() > 30 ?
                content.substring(0, 30) + "..." : content;

        // 获取当前用户ID
        Long currentUserId = broadcaster.getCurrentUserId();

        // 这里简化处理，实际应该从消息中获取发送者ID
        // 假设如果消息包含"我:"前缀，则是当前用户发送的
        boolean isFromCurrentUser = content.startsWith("我:");

        String displayText;
        if (isFromCurrentUser) {
            displayText = "我: " + preview;
        } else {
            // 在群聊中显示"用户X: 内容"或"发送者: 内容"
            displayText = "群成员: " + preview;
        }

        // 更新聊天列表
        upsertChat(
                groupId.toString(),
                groupName,
                null,
                displayText,
                true, // 有未读消息
                true
        );
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
            upsertChat(
                    message.getFromUserId().toString(),
                    null,
                    null,
                    message.getContent(),
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
            upsertChat(
                    message.getGroupId().toString(),
                    null,
                    null,
                    message.getContent(),
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