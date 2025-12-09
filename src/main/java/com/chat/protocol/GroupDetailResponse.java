package com.chat.protocol;

import java.util.List;

/**
 * 群聊详情响应：服务器 -> 客户端
 */
public class GroupDetailResponse {
    private String type = MessageType.GROUP_DETAIL_RESPONSE;
    private boolean success;
    private String message;

    // 群基本信息
    private Long groupId;
    private String groupName;
    private String avatarUrl;
    private String notice;        // 群公告

    // 群成员信息
    private Integer memberCount;  // 成员数量

    // 当前用户在本群的信息
    private String myNickname;    // 我的群昵称
    private Integer role;         // 角色 0:普通成员 1:管理员 2:群主

    // 群文件列表
    private List<GroupFile> files;

    // 群成员列表（简化版）
    private List<GroupMember> members;

    public GroupDetailResponse() {}

    public static class GroupFile {
        private String fileName;
        private String fileSize;
        private String uploadTime;
        private String uploader;
        private String downloadUrl;

        public GroupFile() {}

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }

        public String getUploadTime() { return uploadTime; }
        public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }

        public String getUploader() { return uploader; }
        public void setUploader(String uploader) { this.uploader = uploader; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }

    public static class GroupMember {
        private Long userId;
        private String username;
        private String avatarUrl;
        private String nickname;   // 在群里的昵称
        private Integer role;      // 角色 0:成员 1:管理员 2:群主
        private Integer status;    // 状态 0:离线 1:在线 2:忙碌 3:隐身

        public GroupMember() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public String getMyNickname() { return myNickname; }
    public void setMyNickname(String myNickname) { this.myNickname = myNickname; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public List<GroupFile> getFiles() { return files; }
    public void setFiles(List<GroupFile> files) { this.files = files; }

    public List<GroupMember> getMembers() { return members; }
    public void setMembers(List<GroupMember> members) { this.members = members; }
}