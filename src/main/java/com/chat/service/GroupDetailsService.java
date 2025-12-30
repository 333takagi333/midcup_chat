package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.stage.Window;

import java.io.File;
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
    private final JsonParser jsonParser = new JsonParser();

    // 存储文件列表缓存
    private Map<String, List<GroupDetailResponse.GroupFile>> groupFilesCache = new HashMap<>();

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
                        // 缓存文件列表
                        if (response.getFiles() != null) {
                            String cacheKey = groupId + "_" + userId;
                            groupFilesCache.put(cacheKey, response.getFiles());
                            System.out.println("[GroupDetailsService] 缓存文件列表，数量: " + response.getFiles().size());
                        }
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
     * 下载群文件（使用缓存中的真实fileId）
     */
    public void downloadGroupFile(Long groupId, Long userId, String selectedFile, Window window) {
        if (selectedFile == null || selectedFile.trim().isEmpty() || "暂无文件".equals(selectedFile)) {
            Platform.runLater(() -> DialogUtil.showError(window, "请先选择要下载的文件"));
            return;
        }

        System.out.println("[GroupDetailsService] 开始下载群文件:");
        System.out.println("  - selectedFile: " + selectedFile);
        System.out.println("  - groupId: " + groupId);
        System.out.println("  - userId: " + userId);

        // 1. 从缓存中获取文件列表
        String cacheKey = groupId + "_" + userId;
        List<GroupDetailResponse.GroupFile> cachedFiles = groupFilesCache.get(cacheKey);

        if (cachedFiles == null || cachedFiles.isEmpty()) {
            System.err.println("[GroupDetailsService] 文件列表缓存为空，重新获取群详情");

            // 重新获取群详情
            GroupDetailResponse response = getGroupDetail(groupId, userId);
            if (response != null && response.getFiles() != null) {
                cachedFiles = response.getFiles();
            } else {
                Platform.runLater(() -> DialogUtil.showError(window, "无法获取文件列表"));
                return;
            }
        }

        // 2. 解析选择的文件名
        String cleanFileName = extractCleanFileName(selectedFile);
        System.out.println("[GroupDetailsService] 解析的文件名: " + cleanFileName);

        // 3. 在文件列表中查找匹配的文件
        String realFileId = null;
        for (GroupDetailResponse.GroupFile file : cachedFiles) {
            if (file.getFileName() != null && file.getFileName().contains(cleanFileName)) {
                realFileId = extractFileIdFromFileName(file.getFileName());
                System.out.println("[GroupDetailsService] 找到匹配文件: " + file.getFileName() + ", fileId: " + realFileId);
                break;
            }
        }

        if (realFileId == null) {
            System.err.println("[GroupDetailsService] 未找到匹配的文件，尝试所有文件:");
            for (GroupDetailResponse.GroupFile file : cachedFiles) {
                System.err.println("  - " + file.getFileName());
            }
            Platform.runLater(() -> DialogUtil.showError(window, "未找到匹配的文件信息"));
            return;
        }

        // 4. 显示保存对话框
        String finalRealFileId = realFileId;
        Platform.runLater(() -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存文件");
            fileChooser.setInitialFileName(cleanFileName);
            File saveFile = fileChooser.showSaveDialog(window);

            if (saveFile != null) {
                // 5. 开始下载
                downloadFileWithFileId(groupId, userId, cleanFileName, finalRealFileId, saveFile, window);
            }
        });
    }

    /**
     * 使用fileId下载文件
     */
    private void downloadFileWithFileId(Long groupId, Long userId, String fileName,
                                        String fileId, File saveFile, Window window) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    DialogUtil.showInfo(window, "正在获取文件信息..下载准备中");
                });

                // 1. 发送下载请求
                FileDownloadRequest downloadRequest = new FileDownloadRequest();
                downloadRequest.setFileId(fileId);
                downloadRequest.setUserId(userId);
                downloadRequest.setChatType("group");
                downloadRequest.setGroupId(groupId);
                downloadRequest.setFileName(fileName);

                System.out.println("[GroupDetailsService] 发送下载请求:");
                System.out.println("  - type: " + downloadRequest.getType());
                System.out.println("  - fileId: " + downloadRequest.getFileId());
                System.out.println("  - userId: " + downloadRequest.getUserId());
                System.out.println("  - groupId: " + downloadRequest.getGroupId());
                System.out.println("  - fileName: " + downloadRequest.getFileName());

                String responseJson = socketClient.sendFileDownloadRequest(downloadRequest);

                if (responseJson == null) {
                    Platform.runLater(() -> {
                        DialogUtil.showError(window, "下载失败, 服务器无响应");
                    });
                    return;
                }

                System.out.println("[GroupDetailsService] 收到服务器响应: " + responseJson);

                // 2. 解析响应
                JsonObject jsonResponse = jsonParser.parse(responseJson).getAsJsonObject();
                String responseType = jsonResponse.has("type") ?
                        jsonResponse.get("type").getAsString() : "unknown";
                System.out.println("[GroupDetailsService] 响应类型: " + responseType);

                if (!jsonResponse.get("success").getAsBoolean()) {
                    String errorMsg = jsonResponse.has("message") ?
                            jsonResponse.get("message").getAsString() : "下载失败";
                    System.err.println("[GroupDetailsService] 下载失败: " + errorMsg);

                    // 检查特定错误信息
                    if ("无权限下载该文件".equals(errorMsg)) {
                        Platform.runLater(() -> {
                            DialogUtil.showError(window, "权限不足"+
                                    "您没有权限下载此文件。可能的原因：\n" +
                                            "1. 您已退出群聊\n" +
                                            "2. 文件已被删除\n" +
                                            "3. 您不是群成员");
                        });
                    } else {
                        Platform.runLater(() -> DialogUtil.showError(window, "下载失败"));
                    }
                    return;
                }

                if (!jsonResponse.has("downloadUrl")) {
                    Platform.runLater(() -> {
                        DialogUtil.showError(window, "下载失败, 服务器响应缺少下载链接");
                    });
                    return;
                }

                String downloadUrl = jsonResponse.get("downloadUrl").getAsString();
                String actualFileName = jsonResponse.has("fileName") ?
                        jsonResponse.get("fileName").getAsString() : fileName;

                System.out.println("[GroupDetailsService] 开始下载:");
                System.out.println("  - downloadUrl: " + downloadUrl);
                System.out.println("  - fileName: " + actualFileName);

                // 3. 下载文件
                downloadFileFromUrl(downloadUrl, saveFile, window, actualFileName);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    DialogUtil.showError(window, "下载异常");
                });
            }
        }).start();
    }

    /**
     * 从URL下载文件
     */
    private void downloadFileFromUrl(String downloadUrl, File saveFile, Window window, String fileName) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    DialogUtil.showInfo(window,  "正在下载文件: " + fileName);
                });

                // 处理相对URL
                String fullUrl;
                if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                    fullUrl = downloadUrl;
                } else {
                    // 添加服务器地址前缀
                    fullUrl = "http://"+SocketClient.getServerAddress()+":12355/" +
                            (downloadUrl.startsWith("/") ? downloadUrl.substring(1) : downloadUrl);
                }

                System.out.println("[GroupDetailsService] 完整下载URL: " + fullUrl);

                // 下载文件
                java.net.URL url = new java.net.URL(fullUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                System.out.println("[GroupDetailsService] HTTP响应码: " + responseCode);

                if (responseCode != 200) {
                    Platform.runLater(() -> {
                        DialogUtil.showError(window, "下载失败");
                    });
                    connection.disconnect();
                    return;
                }

                try (java.io.InputStream inputStream = connection.getInputStream();
                     java.io.FileOutputStream outputStream = new java.io.FileOutputStream(saveFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    System.out.println("[GroupDetailsService] 下载完成，总字节: " + totalBytesRead);
                }

                connection.disconnect();

                Platform.runLater(() -> {
                    long actualSize = saveFile.length();
                    String message = String.format("文件下载完成！\n\n" +
                                    "文件名: %s\n" +
                                    "保存位置: %s\n" +
                                    "文件大小: %s",
                            fileName,
                            saveFile.getAbsolutePath(),
                            formatFileSize(actualSize));

                    DialogUtil.showInfo(window, "下载完成");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    DialogUtil.showError(window, "下载过程中出现错误: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 提取干净的文件名（去除大小和上传者信息）
     */
    private String extractCleanFileName(String fileDisplayString) {
        if (fileDisplayString == null) {
            return null;
        }

        // 格式："草稿.doc (255.5 KB) - u1"
        // 或："草稿.doc - u1"
        String[] parts = fileDisplayString.split(" - ");
        if (parts.length > 0) {
            String firstPart = parts[0].trim();
            // 去除括号内的内容
            if (firstPart.contains("(") && firstPart.contains(")")) {
                int start = firstPart.indexOf("(");
                return firstPart.substring(0, start).trim();
            }
            return firstPart;
        }

        return fileDisplayString;
    }

    /**
     * 从文件名中提取fileId
     */
    private String extractFileIdFromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        // 服务器通常会在文件名中包含时间戳和fileId
        // 例如："1766910000000_abcd1234_草稿.doc"
        // 我们需要提取中间的fileId部分

        // 尝试按"_"分割
        String[] parts = fileName.split("_");
        if (parts.length >= 2) {
            // 第一个通常是时间戳，第二个可能是fileId
            return parts[1];
        }

        // 如果没有特定格式，使用文件名哈希
        return "file_" + Math.abs(fileName.hashCode());
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // ========== 以下是原有的其他方法，保持不变 ==========

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