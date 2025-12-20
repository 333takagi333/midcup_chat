package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 聊天业务逻辑服务
 */
public class ChatService {

    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();
    private final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
    private final ChatSessionManager sessionManager = ChatSessionManager.getInstance();

    // 时间格式化器
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    // 数据库datetime格式
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送私聊消息
     */
    public boolean sendPrivateMessage(SocketClient client, Long contactId, Long userId, String content) {
        try {
            ChatPrivateSend message = new ChatPrivateSend();
            message.setToUserId(contactId);
            message.setFromUserId(userId);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());

            System.out.println("[ChatService] 发送私聊消息: " + userId + " -> " + contactId + ", 内容: " + content);

            boolean sent = client.sendMessage(message);

            if (sent) {
                System.out.println("[ChatService] 消息发送成功，等待服务器回传");
            } else {
                System.err.println("[ChatService] 消息发送失败");
            }

            return sent;
        } catch (Exception e) {
            System.err.println("发送私聊消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送群聊消息
     */
    public boolean sendGroupMessage(SocketClient client, Long groupId, Long userId, String content) {
        try {
            ChatGroupSend message = new ChatGroupSend();
            message.setGroupId(groupId);
            message.setFromUserId(userId);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());

            System.out.println("[ChatService] 发送群聊消息: 群组" + groupId + ", 发送者" + userId + ", 内容: " + content);

            boolean sent = client.sendMessage(message);

            if (sent) {
                System.out.println("[ChatService] 群聊消息发送成功，等待服务器回传");
            }

            return sent;
        } catch (Exception e) {
            System.err.println("发送群聊消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理接收到的消息并广播（不处理历史消息响应）
     */
    public void processMessage(String messageJson) {
        try {
            System.out.println("[ChatService] 处理消息: " + messageJson);

            JsonObject jsonObject = jsonParser.parse(messageJson).getAsJsonObject();

            if (jsonObject.has("type")) {
                String type = jsonObject.get("type").getAsString();

                // ========== 重要：跳过历史消息响应 ==========
                if (MessageType.CHAT_HISTORY_RESPONSE.equals(type)) {
                    System.out.println("[ChatService] 收到历史消息响应，跳过处理（由 HistoryService 处理）");
                    return;
                }

                // 私聊消息
                if (MessageType.CHAT_PRIVATE_RECEIVE.equals(type)) {
                    Long messageId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : null;
                    Long fromUserId = jsonObject.get("fromUserId").getAsLong();
                    Long toUserId = jsonObject.get("toUserId").getAsLong();
                    String content = jsonObject.get("content").getAsString();
                    long timestamp = jsonObject.has("timestamp") ?
                            jsonObject.get("timestamp").getAsLong() : System.currentTimeMillis();

                    // 获取发送方用户名
                    String senderName = "用户" + fromUserId;

                    broadcaster.broadcastPrivateMessage(
                            fromUserId,
                            toUserId,
                            content,
                            timestamp,
                            senderName,
                            messageId
                    );

                    System.out.println("[ChatService] 已处理并广播私聊消息: " + fromUserId + " -> " + toUserId);
                }
                // 群聊消息
                else if (MessageType.CHAT_GROUP_RECEIVE.equals(type)) {
                    Long messageId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : null;
                    Long groupId = jsonObject.get("groupId").getAsLong();
                    Long fromUserId = jsonObject.get("fromUserId").getAsLong();
                    String content = jsonObject.get("content").getAsString();
                    long timestamp = jsonObject.has("timestamp") ?
                            jsonObject.get("timestamp").getAsLong() : System.currentTimeMillis();

                    String groupName = "群聊" + groupId;

                    broadcaster.broadcastGroupMessage(
                            groupId,
                            fromUserId,
                            content,
                            timestamp,
                            groupName,
                            messageId
                    );

                    System.out.println("[ChatService] 已处理并广播群聊消息: 群组" + groupId);
                }
                // 其他类型的消息（好友请求、系统通知等）
                else {
                    System.out.println("[ChatService] 收到其他类型消息: " + type);
                }
            }
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 解析数据库datetime字符串为Date对象
     */
    public Date parseDbDateTime(String datetimeStr) {
        if (datetimeStr == null || datetimeStr.trim().isEmpty()) {
            return new Date();
        }

        try {
            // 尝试解析为数据库datetime格式
            return dbDateFormat.parse(datetimeStr);
        } catch (ParseException e) {
            System.err.println("[ChatService] 解析datetime失败: " + datetimeStr + ", 错误: " + e.getMessage());
            return new Date();
        }
    }

    /**
     * 格式化数据库datetime字符串为显示时间（HH:mm）
     */
    public String formatDbDateTimeForDisplay(String datetimeStr) {
        Date date = parseDbDateTime(datetimeStr);
        return timeFormat.format(date);
    }

    /**
     * 将数据库datetime字符串转换为Long类型时间戳（用于比较和分页）
     */
    public Long dbDateTimeToTimestamp(String datetimeStr) {
        Date date = parseDbDateTime(datetimeStr);
        return date.getTime();
    }

    /**
     * 获取 Gson 实例（用于解析JSON）
     */
    public Gson getGson() {
        return gson;
    }
}