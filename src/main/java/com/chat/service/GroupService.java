package com.chat.service;

import com.chat.model.GroupItem;
import com.chat.network.SocketClient;
import com.chat.protocol.GroupListRequest;
import com.chat.protocol.GroupListResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群组相关业务：向服务器请求群组列表，并转换为 UI 层使用的 GroupItem。
 */
public class GroupService {
    private final Gson gson = new Gson();
    private SocketClient client;
    private Map<Long, GroupItem> groupCache = new HashMap<>(); // 缓存群组信息

    public void setSocketClient(SocketClient client) {
        this.client = client;
    }

    /**
     * 从服务器加载群组列表，并转换为 GroupItem 集合。
     */
    public List<GroupItem> loadGroups(SocketClient client) {
        List<GroupItem> result = new ArrayList<>();
        if (client == null || !client.isConnected()) {
            return result;
        }

        GroupListRequest request = new GroupListRequest();
        String responseJson = client.sendRequest(request);
        if (responseJson == null || responseJson.isEmpty()) {
            return result;
        }

        GroupListResponse response = gson.fromJson(responseJson, GroupListResponse.class);
        if (response == null || response.getGroups() == null) {
            return result;
        }

        // 清空缓存并重新加载
        groupCache.clear();

        for (GroupListResponse.GroupItem g : response.getGroups()) {
            if (g.getId() == null) continue;

            GroupItem item = new GroupItem(
                    g.getId().toString(),
                    g.getName() != null ? g.getName() : "未知群组",
                    "暂无消息",
                    "0",
                    g.getAvatar()
            );
            result.add(item);

            // 添加到缓存
            groupCache.put(g.getId(), item);
        }

        System.out.println("[GroupService] 加载群组列表完成，共 " + result.size() + " 个群组");
        return result;
    }

    /**
     * 根据群组ID获取群组名称
     */
    public String getGroupNameById(Long groupId) {
        if (groupId == null) {
            return "未知群组";
        }

        // 从缓存中查找
        GroupItem group = groupCache.get(groupId);
        if (group != null) {
            return group.getName();
        }

        // 如果缓存中没有，尝试重新加载群组列表
        if (client != null && client.isConnected()) {
            List<GroupItem> groups = loadGroups(client);
            group = groupCache.get(groupId);
            if (group != null) {
                return group.getName();
            }
        }

        return "群聊" + groupId;
    }

    /**
     * 根据群组ID获取群组头像
     */
    public String getGroupAvatarById(Long groupId) {
        if (groupId == null) {
            return null;
        }

        // 从缓存中查找
        GroupItem group = groupCache.get(groupId);
        if (group != null) {
            return group.getAvatarUrl();
        }

        // 如果缓存中没有，尝试重新加载群组列表
        if (client != null && client.isConnected()) {
            List<GroupItem> groups = loadGroups(client);
            group = groupCache.get(groupId);
            if (group != null) {
                return group.getAvatarUrl();
            }
        }

        return null;
    }

    /**
     * 获取群组信息
     */
    public GroupItem getGroupInfo(Long groupId) {
        if (groupId == null) {
            return null;
        }

        // 从缓存中查找
        GroupItem group = groupCache.get(groupId);
        if (group != null) {
            return group;
        }

        // 如果缓存中没有，尝试重新加载群组列表
        if (client != null && client.isConnected()) {
            List<GroupItem> groups = loadGroups(client);
            return groupCache.get(groupId);
        }

        return null;
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        groupCache.clear();
    }
}