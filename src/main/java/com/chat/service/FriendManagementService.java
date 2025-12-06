package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.FriendAddRequest;
import com.chat.protocol.FriendAddResponse;
import com.google.gson.Gson;

/**
 * 好友管理业务逻辑服务
 */
public class FriendManagementService {

    private final Gson gson = new Gson();

    /**
     * 发送好友请求
     */
    public FriendAddResponse sendFriendRequest(SocketClient client, Long targetId) {
        try {
            FriendAddRequest request = new FriendAddRequest();
            request.setToUserId(targetId);

            String response = client.sendRequest(request);
            if (response != null) {
                return gson.fromJson(response, FriendAddResponse.class);
            }
        } catch (Exception e) {
            System.err.println("发送好友请求失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 验证好友ID输入
     */
    public static String validateFriendIdInput(String friendIdStr) {
        if (friendIdStr.isEmpty()) {
            return "请输入好友ID";
        }
        try {
            Long.parseLong(friendIdStr);
            return null;
        } catch (NumberFormatException e) {
            return "请输入有效的用户ID（数字）";
        }
    }
}