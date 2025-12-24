package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.stage.Window;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 群聊详情服务 - 处理群组相关的所有业务逻辑
 */
public class GroupDetailsService {
    private final SocketClient socketClient;
    private final Gson gson = new Gson();

    public GroupDetailsService(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    /**
     * 获取群组详细信息
     */
    public GroupDetailResponse getGroupDetail(Long groupId, Long userId) {
        try {
            GroupDetailRequest request = new GroupDetailRequest(groupId, userId);
            String responseJson = socketClient.sendRequest(request);

            if (responseJson != null) {
                GroupDetailResponse response = gson.fromJson(responseJson, GroupDetailResponse.class);
                if (response != null) {
                    if (response.isSuccess()) {
                        return response;
                    } else {
                        System.err.println("[GroupDetailsService] 获取群详情失败: " + response.getMessage());
                    }
                } else {
                    System.err.println("[GroupDetailsService] 群详情响应解析失败");
                }
            } else {
                System.err.println("[GroupDetailsService] 群详情请求无响应");
            }
        } catch (Exception e) {
            System.err.println("[GroupDetailsService] 获取群详情异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 退出群聊（完整处理逻辑）
     */
    public void exitGroup(Long groupId, Long userId, String groupName, Window window, Runnable onSuccess) {
        // 确认对话框
        boolean confirm = DialogUtil.showConfirmation(
                window,
                "确定要退出群聊 " + groupName + " 吗？退出后将不再接收群消息。"
        );

        if (!confirm) {
            return;
        }

        new Thread(() -> {
            try {
                // 使用通用的Map构建请求
                Map<String, Object> request = new HashMap<>();
                request.put("type", "exit_group_request");
                request.put("groupId", groupId);
                request.put("userId", userId);
                request.put("timestamp", System.currentTimeMillis());

                String responseJson = socketClient.sendRequest(request);

                Platform.runLater(() -> {
                    if (responseJson != null) {
                        try {
                            Map<String, Object> response = gson.fromJson(responseJson, Map.class);
                            if (response != null && "exit_group_response".equals(response.get("type"))) {
                                Boolean success = (Boolean) response.get("success");
                                if (success != null && success) {
                                    DialogUtil.showInfo(window, "已成功退出群聊");
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                } else {
                                    String errorMsg = (String) response.get("message");
                                    DialogUtil.showError(window, "退出群聊失败: " + errorMsg);
                                }
                            } else {
                                DialogUtil.showError(window, "响应格式不正确");
                            }
                        } catch (Exception e) {
                            DialogUtil.showError(window, "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogUtil.showError(window, "退出群聊请求无响应");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("[GroupDetailsService] 退出群聊异常: " + e.getMessage());
                    DialogUtil.showError(window, "退出群聊失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 下载群文件（完整处理逻辑）
     */
    public void downloadGroupFile(Long groupId, String fileName, String selectedFile, Window window) {
        // 提取文件名
        if (selectedFile == null || selectedFile.equals("暂无文件")) {
            Platform.runLater(() -> DialogUtil.showError(window, "请先选择要下载的文件"));
            return;
        }

        // 处理文件名（去除尺寸和上传者信息）
        String[] parts = selectedFile.split("\\s+");
        if (parts.length == 0) {
            Platform.runLater(() -> DialogUtil.showError(window, "文件格式不正确"));
            return;
        }

        String actualFileName = parts[0];

        Platform.runLater(() -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存文件");
            fileChooser.setInitialFileName(actualFileName);
            java.io.File saveFile = fileChooser.showSaveDialog(window);

            if (saveFile != null) {
                new Thread(() -> {
                    try {
                        Map<String, Object> request = new HashMap<>();
                        request.put("type", "group_file_download_request");
                        request.put("groupId", groupId);
                        request.put("fileName", actualFileName);
                        request.put("savePath", saveFile.getAbsolutePath());
                        request.put("timestamp", System.currentTimeMillis());

                        String responseJson = socketClient.sendRequest(request);

                        Platform.runLater(() -> {
                            if (responseJson != null) {
                                try {
                                    Map<String, Object> response = gson.fromJson(responseJson, Map.class);
                                    if (response != null && "group_file_download_response".equals(response.get("type"))) {
                                        Boolean success = (Boolean) response.get("success");
                                        if (success != null && success) {
                                            DialogUtil.showInfo(window, "文件下载成功: " + saveFile.getAbsolutePath());
                                        } else {
                                            String errorMsg = (String) response.get("message");
                                            DialogUtil.showError(window, "文件下载失败: " + errorMsg);
                                        }
                                    } else {
                                        DialogUtil.showError(window, "响应格式不正确");
                                    }
                                } catch (Exception e) {
                                    DialogUtil.showError(window, "解析响应失败: " + e.getMessage());
                                }
                            } else {
                                DialogUtil.showError(window, "下载请求无响应");
                            }
                        });

                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            System.err.println("[GroupDetailsService] 下载文件异常: " + e.getMessage());
                            DialogUtil.showError(window, "文件下载失败: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }

    /**
     * 加载群信息并更新UI
     */
    public void loadAndDisplayGroupInfo(Long groupId, Long userId, GroupDetailsUICallback callback) {
        new Thread(() -> {
            try {
                GroupDetailResponse response = getGroupDetail(groupId, userId);

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
                        callback.onFailure("加载群详情异常: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * 获取角色文本
     */
    public String getRoleText(Integer role) {
        if (role == null) return "成员";
        switch (role) {
            case 1: return "管理员";
            case 2: return "群主";
            default: return "成员";
        }
    }

    /**
     * 格式化文件信息
     */
    public String formatFileInfo(GroupDetailResponse.GroupFile file) {
        if (file == null) return "";
        return String.format("%s (%s) - %s",
                file.getFileName(),
                file.getFileSize() != null ? file.getFileSize() : "未知大小",
                file.getUploader() != null ? file.getUploader() : "未知上传者"
        );
    }

    /**
     * 格式化成员信息（简化版本，不显示在线状态）
     */
    public String formatMemberInfo(GroupDetailResponse.GroupMember member) {
        if (member == null) return "";
        String nickname = member.getNickname() != null ? member.getNickname() : member.getUsername();
        return String.format("%s [%s]",
                nickname,
                getRoleText(member.getRole())
        );
    }

    // ========== 新增的添加成员功能 ==========

    /**
     * 好友选择回调接口
     */
    public interface FriendSelectCallback {
        void onFriendSelected(Long friendId, String friendUsername);
    }

    /**
     * 获取当前用户的好友列表用于添加成员
     */
    public List<Map<String, Object>> getFriendsForAddMember(Long userId, Long groupId) {
        try {
            System.out.println("[GroupDetailsService] 获取好友用于添加成员: userId=" + userId + ", groupId=" + groupId);

            // 1. 获取好友列表
            List<Map<String, Object>> allFriends = getAllFriends(userId);
            System.out.println("[GroupDetailsService] 所有好友数量: " + allFriends.size());

            if (allFriends.isEmpty()) {
                System.out.println("[GroupDetailsService] 好友列表为空");
                return allFriends;
            }

            // 2. 获取群成员用户名列表 - 使用真实的当前用户ID
            Set<String> groupMemberUsernames = getGroupMemberUsernames(groupId, userId);
            System.out.println("[GroupDetailsService] 群成员用户名数量: " + groupMemberUsernames.size());

            // 3. 通过用户名过滤好友
            List<Map<String, Object>> filteredFriends = filterFriendsByUsername(allFriends, groupMemberUsernames);
            System.out.println("[GroupDetailsService] 过滤后好友数量: " + filteredFriends.size());

            return filteredFriends;

        } catch (Exception e) {
            System.err.println("[GroupDetailsService] 获取好友列表异常: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取群成员用户名集合
     */
    private Set<String> getGroupMemberUsernames(Long groupId, Long userId) {
        Set<String> usernames = new HashSet<>();
        try {
            // 使用真实的当前用户ID获取群详情
            GroupDetailResponse response = getGroupDetail(groupId, userId);

            // 调试日志
            System.out.println("[GroupDetailsService] 获取群详情结果: " +
                    (response != null ? "成功" : "失败") +
                    ", groupId=" + groupId + ", userId=" + userId);

            if (response != null && response.isSuccess() && response.getMembers() != null) {
                System.out.println("[GroupDetailsService] 群成员数量: " + response.getMembers().size());

                for (GroupDetailResponse.GroupMember member : response.getMembers()) {
                    String username = member.getUsername();
                    if (username != null) {
                        usernames.add(username);
                        System.out.println("[GroupDetailsService] 添加群成员用户名: " + username);
                    }
                }
            } else {
                System.err.println("[GroupDetailsService] 无法获取群成员列表: " +
                        (response != null ? response.getMessage() : "响应为空"));

                // 如果因为用户不在群中而失败，尝试通过其他方式获取群成员
                // 这里可以添加备用方案
                if (response != null && response.getMessage() != null &&
                        response.getMessage().contains("不在该群聊中")) {
                    System.out.println("[GroupDetailsService] 用户不在群中，尝试备用方案...");
                    // 可以在这里调用一个不需要用户验证的API（如果有的话）
                }
            }
        } catch (Exception e) {
            System.err.println("[GroupDetailsService] 获取群成员用户名异常: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[GroupDetailsService] 群成员用户名集合大小: " + usernames.size());
        return usernames;
    }

    /**
     * 通过用户名过滤好友（最可靠的方式）
     */
    private List<Map<String, Object>> filterFriendsByUsername(
            List<Map<String, Object>> allFriends,
            Set<String> groupMemberUsernames) {

        List<Map<String, Object>> availableFriends = new ArrayList<>();

        for (Map<String, Object> friend : allFriends) {
            try {
                String username = (String) friend.get("username");

                // 如果好友用户名不在群成员用户名集合中，则可以添加
                if (username != null && !groupMemberUsernames.contains(username)) {
                    availableFriends.add(friend);
                }
            } catch (Exception e) {
                // 跳过处理异常的好友
                continue;
            }
        }

        return availableFriends;
    }

    /**
     * 获取所有好友
     */
    private List<Map<String, Object>> getAllFriends(Long userId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", MessageType.FRIEND_LIST_REQUEST);
            request.put("userId", userId);
            request.put("timestamp", System.currentTimeMillis());

            String responseJson = socketClient.sendRequest(request);

            if (responseJson != null) {
                Map<String, Object> response = gson.fromJson(responseJson, Map.class);

                if (response != null && MessageType.FRIEND_LIST_RESPONSE.equals(response.get("type"))) {
                    Object friendsObj = response.get("friends");

                    if (friendsObj instanceof List) {
                        return (List<Map<String, Object>>) friendsObj;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[GroupDetailsService] 获取好友列表异常: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 添加成员到群聊（完整处理逻辑）
     */
    public void addMemberToGroup(Long groupId, Long targetUserId, String targetUsername,
                                 Long operatorId, Window window, Runnable onSuccess) {
        // 验证参数
        if (targetUserId == null || targetUsername == null || targetUsername.isEmpty()) {
            Platform.runLater(() -> DialogUtil.showError(window, "请选择要添加的成员"));
            return;
        }

        // 确认对话框
        boolean confirm = DialogUtil.showConfirmation(
                window,
                "确定要将 " + targetUsername + " 添加到群聊吗？"
        );

        if (!confirm) {
            return;
        }

        new Thread(() -> {
            try {
                // 使用新的协议类
                GroupAddMemberRequest request = new GroupAddMemberRequest(
                        groupId, targetUserId, operatorId
                );

                String responseJson = socketClient.sendGroupAddMemberRequest(request);

                Platform.runLater(() -> {
                    if (responseJson != null) {
                        try {
                            GroupAddMemberResponse response = gson.fromJson(responseJson, GroupAddMemberResponse.class);
                            if (response != null && MessageType.GROUP_ADD_MEMBER_RESPONSE.equals(response.getType())) {
                                if (response.isSuccess()) {
                                    DialogUtil.showInfo(window, "添加成员成功");
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                } else {
                                    String errorMsg = response.getMessage();
                                    DialogUtil.showError(window, "添加成员失败: " + errorMsg);
                                }
                            } else {
                                DialogUtil.showError(window, "响应格式不正确");
                            }
                        } catch (Exception e) {
                            DialogUtil.showError(window, "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogUtil.showError(window, "添加成员请求无响应");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("[GroupDetailsService] 添加成员异常: " + e.getMessage());
                    DialogUtil.showError(window, "添加成员失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 回调接口用于UI更新
     */
    public interface GroupDetailsUICallback {
        void onSuccess(GroupDetailResponse response);
        void onFailure(String errorMessage);
    }
}