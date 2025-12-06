package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * 主界面数据管理服务
 */
public class MainDataService {

    private final FriendService friendService;
    private final GroupService groupService;

    public MainDataService() {
        this.friendService = new FriendService();
        this.groupService = new GroupService();
    }

    /**
     * 刷新好友列表
     */
    public List<FriendItem> refreshFriends(SocketClient client) {
        try {
            if (client == null || !client.isConnected()) {
                System.err.println("[MainDataService] 无法刷新好友列表：未连接到服务器");
                return null;
            }

            System.out.println("[MainDataService] 刷新好友列表...");
            List<FriendItem> friends = friendService.loadFriends(client);
            System.out.println("[MainDataService] 好友列表刷新完成，共 " + (friends != null ? friends.size() : 0) + " 个好友");
            return friends;

        } catch (Exception e) {
            System.err.println("[MainDataService] 刷新好友列表失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 刷新群组列表
     */
    public List<GroupItem> refreshGroups(SocketClient client) {
        try {
            if (client == null || !client.isConnected()) {
                System.err.println("[MainDataService] 无法刷新群组列表：未连接到服务器");
                return null;
            }

            System.out.println("[MainDataService] 刷新群组列表...");
            List<GroupItem> groups = groupService.loadGroups(client);
            System.out.println("[MainDataService] 群组列表刷新完成，共 " + (groups != null ? groups.size() : 0) + " 个群组");
            return groups;

        } catch (Exception e) {
            System.err.println("[MainDataService] 刷新群组列表失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 刷新所有数据
     */
    public RefreshResult refreshAll(SocketClient client) {
        RefreshResult result = new RefreshResult();

        try {
            result.setFriends(refreshFriends(client));
            result.setGroups(refreshGroups(client));

            if (result.getFriends() != null && result.getGroups() != null) {
                result.setSuccess(true);
                result.setMessage("刷新成功");
            } else {
                result.setSuccess(false);
                result.setMessage("刷新失败，部分数据获取失败");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("刷新失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新好友ObservableList数据 - 修复泛型擦除冲突
     */
    public void updateFriendList(ObservableList<FriendItem> friendList, List<FriendItem> newFriends) {
        if (friendList != null && newFriends != null) {
            friendList.setAll(newFriends);
        }
    }

    /**
     * 更新群组ObservableList数据 - 修复泛型擦除冲突
     */
    public void updateGroupList(ObservableList<GroupItem> groupList, List<GroupItem> newGroups) {
        if (groupList != null && newGroups != null) {
            groupList.setAll(newGroups);
        }
    }

    /**
     * 刷新结果包装类
     */
    public static class RefreshResult {
        private boolean success;
        private String message;
        private List<FriendItem> friends;
        private List<GroupItem> groups;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<FriendItem> getFriends() { return friends; }
        public void setFriends(List<FriendItem> friends) { this.friends = friends; }

        public List<GroupItem> getGroups() { return groups; }
        public void setGroups(List<GroupItem> groups) { this.groups = groups; }
    }
}