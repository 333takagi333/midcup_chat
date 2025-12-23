package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.ChatHistoryRequest;
import com.chat.protocol.ChatHistoryResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;

/**
 * 专门的历史消息服务
 */
public class HistoryService {
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    // 时间格式化器
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    // 数据库datetime格式
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryService() {
        System.out.println("[HistoryService] 初始化");
    }

    /**
     * 加载历史消息
     */
    public void loadHistoryMessages(SocketClient client, String chatType, Long targetId,
                                    Integer limit, Long beforeTimestamp,
                                    HistoryCallback callback) {
        new Thread(() -> {
            try {
                System.out.println("[HistoryService] 开始加载历史消息: " + chatType + ", 目标ID: " + targetId);

                ChatHistoryRequest request = new ChatHistoryRequest();
                request.setChatType(chatType);
                request.setLimit(limit != null ? limit : 50);

                // 关键修复：根据聊天类型设置不同的字段
                if ("private".equals(chatType)) {
                    System.out.println("[HistoryService] 设置私聊参数: targetUserId=" + targetId);
                    request.setTargetUserId(targetId);  // 私聊用 targetUserId
                } else if ("group".equals(chatType)) {
                    System.out.println("[HistoryService] 设置群聊参数: groupId=" + targetId);
                    request.setGroupId(targetId);       // 群聊用 groupId
                } else {
                    System.err.println("[HistoryService] 错误的聊天类型: " + chatType);
                    if (callback != null) {
                        callback.onHistoryLoaded(null, "错误的聊天类型: " + chatType);
                    }
                    return;
                }

                if (beforeTimestamp != null) {
                    request.setBeforeTimestamp(beforeTimestamp);
                }

                // 打印请求详情
                System.out.println("[HistoryService] 历史记录请求详情:");
                System.out.println("  - chatType: " + request.getChatType());
                System.out.println("  - targetUserId: " + request.getTargetUserId());
                System.out.println("  - groupId: " + request.getGroupId());
                System.out.println("  - limit: " + request.getLimit());

                // 发送请求
                String responseJson = client.sendRequest(request);

                if (responseJson == null || responseJson.trim().isEmpty()) {
                    System.err.println("[HistoryService] 历史消息请求无响应");
                    if (callback != null) {
                        callback.onHistoryLoaded(null, "服务器无响应");
                    }
                    return;
                }

                System.out.println("[HistoryService] 收到响应: " + responseJson);

                ChatHistoryResponse response = gson.fromJson(responseJson, ChatHistoryResponse.class);

                if (response == null) {
                    System.err.println("[HistoryService] 解析响应失败");
                    if (callback != null) {
                        callback.onHistoryLoaded(null, "解析响应失败");
                    }
                    return;
                }

                if (!response.isSuccess()) {
                    System.err.println("[HistoryService] 请求失败: " + response.getMessage());
                    if (callback != null) {
                        callback.onHistoryLoaded(null, response.getMessage());
                    }
                    return;
                }

                System.out.println("[HistoryService] 成功加载 " +
                        (response.getMessages() != null ? response.getMessages().size() : 0) + " 条历史消息");

                if (callback != null) {
                    callback.onHistoryLoaded(response, null);
                }

            } catch (Exception e) {
                System.err.println("[HistoryService] 加载失败: " + e.getMessage());
                if (callback != null) {
                    callback.onHistoryLoaded(null, "加载失败: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 专门的历史请求方法
     */
    private String sendHistoryRequest(SocketClient client, ChatHistoryRequest request) {
        try {
            // 创建新的SocketClient实例，避免与主连接冲突
            SocketClient tempClient = new SocketClient();

            System.out.println("[HistoryService] 建立临时连接...");
            if (!tempClient.connect()) {
                System.err.println("[HistoryService] 临时连接失败");
                return null;
            }

            // 发送请求
            String json = gson.toJson(request);
            System.out.println("[HistoryService] 发送历史请求: " + json);

            // 使用 sendRequest 方法
            String response = tempClient.sendRequest(request);

            // 关闭临时连接
            tempClient.disconnect();

            System.out.println("[HistoryService] 收到历史响应: " + (response != null ? response.substring(0, Math.min(100, response.length())) + "..." : "null"));

            return response;

        } catch (Exception e) {
            System.err.println("[HistoryService] 历史请求失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析数据库datetime字符串为Date对象
     */
    public java.util.Date parseDbDateTime(String datetimeStr) {
        if (datetimeStr == null || datetimeStr.trim().isEmpty()) {
            return new java.util.Date();
        }

        try {
            return dbDateFormat.parse(datetimeStr);
        } catch (Exception e) {
            System.err.println("[HistoryService] 解析datetime失败: " + datetimeStr);
            return new java.util.Date();
        }
    }

    /**
     * 格式化数据库datetime字符串为显示时间（HH:mm）
     */
    public String formatDbDateTimeForDisplay(String datetimeStr) {
        java.util.Date date = parseDbDateTime(datetimeStr);
        return timeFormat.format(date);
    }

    /**
     * 将数据库datetime字符串转换为Long类型时间戳
     */
    public Long dbDateTimeToTimestamp(String datetimeStr) {
        java.util.Date date = parseDbDateTime(datetimeStr);
        return date.getTime();
    }

    /**
     * 历史消息加载回调接口
     */
    public interface HistoryCallback {
        void onHistoryLoaded(ChatHistoryResponse response, String error);
    }
}