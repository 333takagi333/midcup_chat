package com.chat.service;

import com.chat.model.FriendItem;
import com.chat.network.SocketClient;
import com.chat.protocol.FriendListRequest;
import com.chat.protocol.FriendListResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友相关业务：向服务器请求好友列表，并转换为 UI 层使用的 FriendItem。
 */
public class FriendService {
    private final Gson gson = new Gson();
    private SocketClient client;
    private Map<Long, FriendItem> friendCache = new HashMap<>(); // 缓存好友信息

    public void setSocketClient(SocketClient client) {
        this.client = client;
    }

    /**
     * 从服务器加载好友列表，并转换为 FriendItem 集合。
     */
    public List<FriendItem> loadFriends(SocketClient client) {
        List<FriendItem> result = new ArrayList<>();
        if (client == null || !client.isConnected()) {
            return result;
        }

        FriendListRequest request = new FriendListRequest();
        String responseJson = client.sendRequest(request);
        if (responseJson == null || responseJson.isEmpty()) {
            return result;
        }

        FriendListResponse response = gson.fromJson(responseJson, FriendListResponse.class);
        if (response == null || response.getFriends() == null) {
            return result;
        }

        // 清空缓存并重新加载
        friendCache.clear();

        for (FriendListResponse.FriendItem f : response.getFriends()) {
            if (f.getUid() == null) continue;

            FriendItem item = new FriendItem(
                    f.getUid().toString(),
                    f.getUsername() != null ? f.getUsername() : "未知用户",
                    "在线",
                    f.getAvatarUrl(),
                    ""
            );
            result.add(item);

            // 添加到缓存
            friendCache.put(f.getUid(), item);
        }

        System.out.println("[FriendService] 加载好友列表完成，共 " + result.size() + " 个好友");
        return result;
    }

    /**
     * 根据好友ID获取好友名称
     */
    public String getFriendNameById(Long friendId) {
        if (friendId == null) {
            return "未知用户";
        }

        // 从缓存中查找
        FriendItem friend = friendCache.get(friendId);
        if (friend != null) {
            return friend.getUsername();
        }

        // 如果缓存中没有，尝试重新加载好友列表
        if (client != null && client.isConnected()) {
            List<FriendItem> friends = loadFriends(client);
            friend = friendCache.get(friendId);
            if (friend != null) {
                return friend.getUsername();
            }
        }

        return "用户" + friendId;
    }

    /**
     * 根据好友ID获取好友头像
     */
    public String getFriendAvatarById(Long friendId) {
        if (friendId == null) {
            return null;
        }

        // 从缓存中查找
        FriendItem friend = friendCache.get(friendId);
        if (friend != null) {
            return friend.getAvatarUrl();
        }

        // 如果缓存中没有，尝试重新加载好友列表
        if (client != null && client.isConnected()) {
            List<FriendItem> friends = loadFriends(client);
            friend = friendCache.get(friendId);
            if (friend != null) {
                return friend.getAvatarUrl();
            }
        }

        return null;
    }

    /**
     * 获取好友信息
     */
    public FriendItem getFriendInfo(Long friendId) {
        if (friendId == null) {
            return null;
        }

        // 从缓存中查找
        FriendItem friend = friendCache.get(friendId);
        if (friend != null) {
            return friend;
        }

        // 如果缓存中没有，尝试重新加载好友列表
        if (client != null && client.isConnected()) {
            List<FriendItem> friends = loadFriends(client);
            return friendCache.get(friendId);
        }

        return null;
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        friendCache.clear();
    }
}