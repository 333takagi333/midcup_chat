package com.chat.protocol;

import java.util.List;

/**
 * 好友请求列表响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class FriendRequestListResponse {
    private String type = MessageType.FRIEND_REQUEST_LIST_RESPONSE;
    private boolean success;
    private String message;
    private List<FriendRequestItem> requests;

    // 内部类
    public static class FriendRequestItem {
        private Long requestId;
        private Long fromUserId;
        private String fromUsername;
        private String requestTime;
        private Integer status; // 0:待处理, 1:已同意, 2:已拒绝

        // 构造函数
        public FriendRequestItem() {}

        public FriendRequestItem(Long requestId, Long fromUserId, String fromUsername,
                                 String requestTime, Integer status) {
            this.requestId = requestId;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.requestTime = requestTime;
            this.status = status;
        }

        // Getters and Setters
        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }

        public Long getFromUserId() { return fromUserId; }
        public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

        public String getFromUsername() { return fromUsername; }
        public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

        public String getRequestTime() { return requestTime; }
        public void setRequestTime(String requestTime) { this.requestTime = requestTime; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<FriendRequestItem> getRequests() { return requests; }
    public void setRequests(List<FriendRequestItem> requests) { this.requests = requests; }
}