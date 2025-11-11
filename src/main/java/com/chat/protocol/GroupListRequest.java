package com.chat.protocol;

/**
 * 群聊列表请求：客户端 -> 服务器
 */
@SuppressWarnings("unused")
public class GroupListRequest {
    private String type = MessageType.GROUP_LIST_REQUEST;

    public GroupListRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
