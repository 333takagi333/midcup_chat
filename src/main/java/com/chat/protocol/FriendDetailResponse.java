package com.chat.protocol;

/**
 * 好友详情响应：服务器 -> 客户端
 */
public class FriendDetailResponse {
    private String type = MessageType.FRIEND_DETAIL_RESPONSE;
    private boolean success;
    private String message;

    // 好友基本信息
    private Long friendId;
    private String username;
    private String avatarUrl;
    private Integer gender;      // 0未知 1男 2女
    private String birthday;     // ISO格式日期
    private String tele;         // 电话号码


    public FriendDetailResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getTele() { return tele; }
    public void setTele(String tele) { this.tele = tele; }

}