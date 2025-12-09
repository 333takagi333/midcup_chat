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
     * 更新群昵称（完整处理逻辑）
     */
    public void updateGroupNickname(Long groupId, Long userId, String nickname, Window window) {
        if (nickname.isEmpty()) {
            Platform.runLater(() -> DialogUtil.showError(window, "昵称不能为空"));
            return;
        }

        if (nickname.length() > 20) {
            Platform.runLater(() -> DialogUtil.showError(window, "昵称不能超过20个字符"));
            return;
        }

        new Thread(() -> {
            try {
                // 使用通用的Map构建请求
                Map<String, Object> request = new HashMap<>();
                request.put("type", "update_nickname_request");
                request.put("groupId", groupId);
                request.put("userId", userId);
                request.put("nickname", nickname);
                request.put("timestamp", System.currentTimeMillis());

                String responseJson = socketClient.sendRequest(request);

                Platform.runLater(() -> {
                    if (responseJson != null) {
                        try {
                            Map<String, Object> response = gson.fromJson(responseJson, Map.class);
                            if (response != null && "update_nickname_response".equals(response.get("type"))) {
                                Boolean success = (Boolean) response.get("success");
                                if (success != null && success) {
                                    DialogUtil.showInfo(window, "昵称更新成功");
                                } else {
                                    String errorMsg = (String) response.get("message");
                                    DialogUtil.showError(window, "昵称更新失败: " + errorMsg);
                                }
                            } else {
                                DialogUtil.showError(window, "响应格式不正确");
                            }
                        } catch (Exception e) {
                            DialogUtil.showError(window, "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogUtil.showError(window, "更新昵称请求无响应");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("[GroupDetailsService] 更新昵称异常: " + e.getMessage());
                    DialogUtil.showError(window, "更新昵称失败: " + e.getMessage());
                });
            }
        }).start();
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
     * 更新群公告（完整处理逻辑）
     */
    public void updateGroupNotice(Long groupId, String notice, Long userId, Window window) {
        if (notice.isEmpty()) {
            Platform.runLater(() -> DialogUtil.showError(window, "群公告不能为空"));
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("type", "group_notice_update_request");
                request.put("groupId", groupId);
                request.put("notice", notice);
                request.put("userId", userId);
                request.put("timestamp", System.currentTimeMillis());

                String responseJson = socketClient.sendRequest(request);

                Platform.runLater(() -> {
                    if (responseJson != null) {
                        try {
                            Map<String, Object> response = gson.fromJson(responseJson, Map.class);
                            if (response != null && "group_notice_update_response".equals(response.get("type"))) {
                                Boolean success = (Boolean) response.get("success");
                                if (success != null && success) {
                                    DialogUtil.showInfo(window, "群公告更新成功");
                                } else {
                                    String errorMsg = (String) response.get("message");
                                    DialogUtil.showError(window, "群公告更新失败: " + errorMsg);
                                }
                            } else {
                                DialogUtil.showError(window, "响应格式不正确");
                            }
                        } catch (Exception e) {
                            DialogUtil.showError(window, "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogUtil.showError(window, "更新公告请求无响应");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("[GroupDetailsService] 更新群公告异常: " + e.getMessage());
                    DialogUtil.showError(window, "更新群公告失败: " + e.getMessage());
                });
            }
        }).start();
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
     * 获取状态文本
     */
    public String getStatusText(Integer status) {
        if (status == null) return "离线";
        switch (status) {
            case 1: return "在线";
            case 2: return "忙碌";
            case 3: return "隐身";
            default: return "离线";
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
     * 格式化成员信息
     */
    public String formatMemberInfo(GroupDetailResponse.GroupMember member) {
        if (member == null) return "";
        String nickname = member.getNickname() != null ? member.getNickname() : member.getUsername();
        return String.format("%s (%s) [%s]",
                nickname,
                getRoleText(member.getRole()),
                getStatusText(member.getStatus())
        );
    }

    /**
     * 回调接口用于UI更新
     */
    public interface GroupDetailsUICallback {
        void onSuccess(GroupDetailResponse response);
        void onFailure(String errorMessage);
    }
}