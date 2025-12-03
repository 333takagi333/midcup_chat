package com.chat.protocol;

/**
 * 创建群聊请求：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class GroupCreateRequest {
    private String type = MessageType.GROUP_CREATE_REQUEST;
    private String groupName;

    // 默认构造函数
    public GroupCreateRequest() {}

    // 简化构造函数，不需要描述
    public GroupCreateRequest(String groupName) {
        this.groupName = groupName;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}