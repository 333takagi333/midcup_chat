package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.stage.Window;

/**
 * 好友资料服务 - 处理好友相关的所有业务逻辑
 */
public class FriendProfileService {
    private final SocketClient socketClient;
    private final Gson gson = new Gson();

    public FriendProfileService(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    /**
     * 获取好友详细信息
     */
    public FriendDetailResponse getFriendDetail(Long userId, Long friendId) {
        try {
            FriendDetailRequest request = new FriendDetailRequest(userId, friendId);
            String responseJson = socketClient.sendRequest(request);

            if (responseJson != null) {
                FriendDetailResponse response = gson.fromJson(responseJson, FriendDetailResponse.class);
                if (response != null) {
                    if (response.isSuccess()) {
                        return response;
                    } else {
                        System.err.println("[FriendProfileService] 获取好友详情失败: " + response.getMessage());
                    }
                } else {
                    System.err.println("[FriendProfileService] 好友详情响应解析失败");
                }
            } else {
                System.err.println("[FriendProfileService] 好友详情请求无响应");
            }
        } catch (Exception e) {
            System.err.println("[FriendProfileService] 获取好友详情异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除好友（完整处理逻辑）
     */
    public void deleteFriend(Long userId, Long friendId, String friendName, Window window, Runnable onSuccess) {
        // 确认对话框
        boolean confirm = DialogUtil.showConfirmation(
                window,
                "确定要删除好友 " + friendName + " 吗？此操作不可恢复。"
        );

        if (!confirm) {
            return;
        }

        new Thread(() -> {
            try {
                DeleteFriendRequest request = new DeleteFriendRequest(userId, friendId);
                String responseJson = socketClient.sendRequest(request);

                Platform.runLater(() -> {
                    if (responseJson != null) {
                        try {
                            DeleteFriendResponse response = gson.fromJson(responseJson, DeleteFriendResponse.class);
                            if (response != null && response.isSuccess()) {
                                DialogUtil.showInfo(window, "好友删除成功");
                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                            } else {
                                String errorMsg = response != null ? response.getMessage() : "删除失败";
                                DialogUtil.showError(window, "好友删除失败: " + errorMsg);
                            }
                        } catch (Exception e) {
                            DialogUtil.showError(window, "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogUtil.showError(window, "删除好友请求无响应");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("[FriendProfileService] 删除好友异常: " + e.getMessage());
                    DialogUtil.showError(window, "删除好友失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 获取性别文本
     */
    public String getGenderText(Integer genderCode) {
        if (genderCode == null) return "未知";
        switch (genderCode) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 格式化生日显示
     */
    public String formatBirthday(String birthday) {
        if (birthday == null || birthday.trim().isEmpty()) {
            return "未设置";
        }
        return birthday;
    }

    /**
     * 格式化电话显示
     */
    public String formatPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "未设置";
        }
        return phone;
    }

    /**
     * 加载好友信息并更新UI
     */
    public void loadAndDisplayFriendInfo(Long userId, Long friendId, FriendProfileUICallback callback) {
        new Thread(() -> {
            try {
                FriendDetailResponse response = getFriendDetail(userId, friendId);

                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        if (callback != null) {
                            callback.onSuccess(response);
                        }
                    } else {
                        if (callback != null) {
                            callback.onFailure(response != null ? response.getMessage() : "加载失败");
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (callback != null) {
                        callback.onFailure("加载好友详情异常: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * 回调接口用于UI更新
     */
    public interface FriendProfileUICallback {
        void onSuccess(FriendDetailResponse response);
        void onFailure(String errorMessage);
    }
}