package com.chat.service;

import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages the UI state, holding the observable lists for the views.
 */
public class ChatStateService {
    private final ObservableList<ChatItem> chatItems = FXCollections.observableArrayList();
    private final ObservableList<FriendItem> friendItems = FXCollections.observableArrayList();
    private final ObservableList<GroupItem> groupItems = FXCollections.observableArrayList();

    public ObservableList<ChatItem> getChatItems() {
        return chatItems;
    }

    public ObservableList<FriendItem> getFriendItems() {
        return friendItems;
    }

    public ObservableList<GroupItem> getGroupItems() {
        return groupItems;
    }

    public void updateFriendList(List<com.chat.protocol.FriendListResponse.FriendItem> friends) {
        friendItems.clear();
        if (friends != null) {
            for (com.chat.protocol.FriendListResponse.FriendItem friend : friends) {
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

    public void updateGroupList(List<com.chat.protocol.GroupListResponse.GroupItem> groups) {
        groupItems.clear();
        if (groups != null) {
            for (com.chat.protocol.GroupListResponse.GroupItem group : groups) {
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
    }

    public void clearAll() {
        chatItems.clear();
        friendItems.clear();
        groupItems.clear();
    }
}

