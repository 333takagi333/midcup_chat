package com.chat.service;

import com.chat.model.FriendItem;
import com.chat.network.SocketClient;
import com.chat.protocol.FriendListRequest;
import com.chat.protocol.FriendListResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友相关业务：向服务器请求好友列表，并转换为 UI 层使用的 FriendItem。
 */
public class FriendService {

    private final Gson gson = new Gson();

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

        for (FriendListResponse.FriendItem f : response.getFriends()) {
            FriendItem item = new FriendItem(
                    f.getUid() != null ? f.getUid().toString() : "0",
                    f.getUsername() != null ? f.getUsername() : "未知用户",
                    "在线",
                    f.getAvatarUrl(),
                    ""
            );
            result.add(item);
        }
        return result;
    }
}

