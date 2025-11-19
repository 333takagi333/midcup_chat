package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.UpdateProfileRequest;
import com.chat.protocol.UpdateProfileResponse;
import com.chat.protocol.UserInfoRequest;
import com.chat.protocol.UserInfoResponse;
import com.google.gson.Gson;

/**
 * 用户资料网络服务类 - 处理用户资料相关的网络请求
 */
public class UserProfileService {
    private final SocketClient socketClient;
    private final Gson gson = new Gson();

    public UserProfileService(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    /**
     * 加载用户信息
     */
    public UserInfoResponse loadUserInfo(Long userId) {
        try {
            UserInfoRequest request = new UserInfoRequest(userId);
            String response = socketClient.sendUserInfoRequest(request);
            if (response != null) {
                return gson.fromJson(response, UserInfoResponse.class);
            }
        } catch (Exception e) {
            System.err.println("[UserProfileService] 加载用户信息失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 更新用户资料
     */
    public boolean updateUserProfile(UpdateProfileRequest request) {
        try {
            String response = socketClient.sendUpdateProfileRequest(request);
            if (response != null) {
                UpdateProfileResponse updateResponse = gson.fromJson(response, UpdateProfileResponse.class);
                return updateResponse != null && updateResponse.isSuccess();
            }
        } catch (Exception e) {
            System.err.println("[UserProfileService] 更新用户资料失败: " + e.getMessage());
        }
        return false;
    }
}