package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.GroupCreateRequest;
import com.chat.protocol.GroupCreateResponse;
import com.google.gson.Gson;

/**
 * 群组管理业务逻辑服务
 */
public class GroupManagementService {

    private final Gson gson = new Gson();

    /**
     * 创建群组
     */
    public GroupCreateResponse createGroup(SocketClient client, String groupName) {
        try {
            GroupCreateRequest request = new GroupCreateRequest(groupName);
            String response = client.sendRequest(request);

            if (response != null) {
                return gson.fromJson(response, GroupCreateResponse.class);
            }
        } catch (Exception e) {
            System.err.println("创建群组失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 验证群组名称输入
     */
    public static String validateGroupNameInput(String groupName) {
        if (groupName.isEmpty()) {
            return "请输入群聊名称";
        }
        if (groupName.length() > 20) {
            return "群聊名称不能超过20个字符";
        }
        return null;
    }
}