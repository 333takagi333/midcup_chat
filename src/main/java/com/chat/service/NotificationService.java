package com.chat.service;

import com.chat.network.SocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知管理业务逻辑服务
 */
public class NotificationService {

    private final Gson gson = new Gson();

    /**
     * 加载好友请求通知
     */
    public List<NotificationItem> loadFriendRequests(SocketClient client) {
        List<NotificationItem> notifications = new ArrayList<>();

        try {
            JsonObject request = new JsonObject();
            request.addProperty("type", "friend_request_list_request");

            String response = client.sendRequest(request);
            if (response != null) {
                JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                if (responseObj.has("success") && responseObj.get("success").getAsBoolean()
                        && responseObj.has("requests")) {

                    JsonArray requestsArray = responseObj.getAsJsonArray("requests");
                    for (int i = 0; i < requestsArray.size(); i++) {
                        JsonObject requestObj = requestsArray.get(i).getAsJsonObject();

                        Long requestId = requestObj.has("requestId") ?
                                requestObj.get("requestId").getAsLong() : 0L;
                        Long fromUserId = requestObj.has("fromUserId") ?
                                requestObj.get("fromUserId").getAsLong() : 0L;
                        String fromUsername = requestObj.has("fromUsername") ?
                                requestObj.get("fromUsername").getAsString() : "未知用户";
                        String requestTime = requestObj.has("requestTime") ?
                                requestObj.get("requestTime").getAsString() : "";
                        Integer status = requestObj.has("status") ?
                                requestObj.get("status").getAsInt() : 0;

                        if (status == 0) {
                            notifications.add(new NotificationItem(
                                    requestId, fromUserId, fromUsername, requestTime, status));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("加载通知失败: " + e.getMessage());
        }

        return notifications;
    }

    /**
     * 处理好友请求
     */
    public boolean processFriendRequest(SocketClient client, Long requestId, boolean accept) {
        try {
            JsonObject responseRequest = new JsonObject();
            responseRequest.addProperty("type", "friend_request_response");
            responseRequest.addProperty("requestId", requestId);
            responseRequest.addProperty("accept", accept);

            String response = client.sendRequest(responseRequest);
            if (response != null) {
                JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                return responseObj.has("success") && responseObj.get("success").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("处理好友请求失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 通知项数据模型
     */
    public static class NotificationItem {
        private Long requestId;
        private Long fromUserId;
        private String fromUsername;
        private String requestTime;
        private Integer status; // 0:待处理, 1:已同意, 2:已拒绝

        public NotificationItem(Long requestId, Long fromUserId, String fromUsername,
                                String requestTime, Integer status) {
            this.requestId = requestId;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.requestTime = requestTime;
            this.status = status;
        }

        public Long getRequestId() { return requestId; }
        public Long getFromUserId() { return fromUserId; }
        public String getFromUsername() { return fromUsername; }
        public String getRequestTime() { return requestTime; }
        public Integer getStatus() { return status; }

        public String getStatusText() {
            switch (status) {
                case 0: return "待处理";
                case 1: return "已同意";
                case 2: return "已拒绝";
                default: return "未知";
            }
        }
    }
}