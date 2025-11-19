package com.chat.protocol;

/**
 * 更新资料请求：客户端 -> 服务器
 */
public class UpdateProfileRequest {
    private String type = "update_profile_request";
    private String username;
    private Integer gender;
    private String birthday;
    private String tele;
    private String avatarData; // Base64编码的头像图片数据
    private String avatarFileName; // 头像文件名

    public UpdateProfileRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getTele() { return tele; }
    public void setTele(String tele) { this.tele = tele; }

    public String getAvatarData() { return avatarData; }
    public void setAvatarData(String avatarData) { this.avatarData = avatarData; }

    public String getAvatarFileName() { return avatarFileName; }
    public void setAvatarFileName(String avatarFileName) { this.avatarFileName = avatarFileName; }
}