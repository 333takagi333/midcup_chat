package com.chat.service;

import com.chat.model.GroupItem;
import com.chat.network.SocketClient;
import com.chat.protocol.GroupListRequest;
import com.chat.protocol.GroupListResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * 群组相关业务：向服务器请求群组列表，并转换为 UI 层使用的 GroupItem。
 */
public class GroupService {

    private final Gson gson = new Gson();

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

        for (GroupListResponse.GroupItem g : response.getGroups()) {
            GroupItem item = new GroupItem(
                    g.getId() != null ? g.getId().toString() : "0",
                    g.getName() != null ? g.getName() : "未知群组",
                    "暂无消息",
                    "0",
                    g.getAvatar()
            );
            result.add(item);
        }
        return result;
    }
}
