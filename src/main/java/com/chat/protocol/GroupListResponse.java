package com.chat.protocol;

import java.util.List;

/**
 * 群聊列表响应：服务器 -> 客户端
 */
@SuppressWarnings("unused")
public class GroupListResponse {
    private String type = MessageType.GROUP_LIST_RESPONSE;
    private List<GroupItem> groups;

    public GroupListResponse() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<GroupItem> getGroups() { return groups; }
    public void setGroups(List<GroupItem> groups) { this.groups = groups; }

    public static class GroupItem {
        private Long id;
        private String name;
        private String avatar;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
}
