package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
     * 处理接收到的消息并广播
     */
    public void processMessage(String messageJson) {
        try {
            System.out.println("[ChatService] 处理消息: " + messageJson);

            JsonObject jsonObject = jsonParser.parse(messageJson).getAsJsonObject();

            if (jsonObject.has("type")) {
                String type = jsonObject.get("type").getAsString();

                // 私聊消息
                if ("chat_private_receive".equals(type)) {
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
                else if ("chat_group_receive".equals(type)) {
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
            }
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载历史消息 - 从服务器获取更早的聊天记录
     */
    public void loadHistoryMessages(SocketClient client, String chatType, Long targetId,
                                    String chatKey, Integer limit, Long beforeTimestamp,
                                    ChatHistoryCallback callback) {
        new Thread(() -> {
            try {
                System.out.println("[ChatService] 开始加载历史消息: " + chatType + ", 目标ID: " + targetId);

                ChatHistoryRequest request = new ChatHistoryRequest();
                request.setChatType(chatType);

                if ("private".equals(chatType)) {
                    request.setTargetUserId(targetId);
                } else if ("group".equals(chatType)) {
                    request.setGroupId(targetId);
                }

                if (limit != null) {
                    request.setLimit(limit);
                } else {
                    request.setLimit(50); // 默认50条
                }

                if (beforeTimestamp != null) {
                    request.setBeforeTimestamp(beforeTimestamp);
                    System.out.println("[ChatService] 加载早于 " + beforeTimestamp + " 的消息");
                }

                // 发送历史消息请求
                String responseJson = client.sendAndReceive(request, 5000); // 5秒超时

                if (responseJson == null || responseJson.trim().isEmpty()) {
                    System.err.println("[ChatService] 历史消息请求无响应");
                    if (callback != null) {
                        callback.onHistoryLoaded(null, "服务器无响应");
                    }
                    return;
                }

                System.out.println("[ChatService] 收到历史消息响应: " + responseJson);

                ChatHistoryResponse response = gson.fromJson(responseJson, ChatHistoryResponse.class);

                if (response == null) {
                    System.err.println("[ChatService] 解析历史消息响应失败");
                    if (callback != null) {
                        callback.onHistoryLoaded(null, "解析响应失败");
                    }
                    return;
                }

                if (!response.isSuccess()) {
                    System.err.println("[ChatService] 历史消息请求失败: " + response.getMessage());
                    if (callback != null) {
                        callback.onHistoryLoaded(null, response.getMessage());
                    }
                    return;
                }

                if (response.getMessages() == null || response.getMessages().isEmpty()) {
                    System.out.println("[ChatService] 没有更多历史消息");
                    if (callback != null) {
                        callback.onHistoryLoaded(response, "没有更多历史消息");
                    }
                    return;
                }

                System.out.println("[ChatService] 成功加载 " + response.getMessages().size() + " 条历史消息");

                // 处理历史消息并保存到会话管理器
                for (ChatHistoryResponse.HistoryMessageItem item : response.getMessages()) {
                    String time = timeFormat.format(new Date(item.getTimestamp()));

                    if ("private".equals(chatType)) {
                        String senderName = "用户" + item.getSenderId();
                        String displayMessage = "[" + time + "] " + senderName + ": " + item.getContent();

                        // 保存到会话管理器（需要知道当前用户ID和联系人ID）
                        // 这里简化处理，由广播器处理时保存
                        System.out.println("[ChatService] 加载私聊历史消息: " + displayMessage);

                    } else if ("group".equals(chatType)) {
                        String senderName = "用户" + item.getSenderId();
                        String displayMessage = "[" + time + "] " + senderName + ": " + item.getContent();

                        // 保存到会话管理器
                        sessionManager.addGroupMessage(item.getGroupId(), displayMessage);
                        System.out.println("[ChatService] 保存群聊历史消息到会话: " + displayMessage);
                    }
                }

                if (callback != null) {
                    callback.onHistoryLoaded(response, null);
                }

            } catch (Exception e) {
                System.err.println("[ChatService] 加载历史消息失败: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onHistoryLoaded(null, "加载失败: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 历史消息加载回调接口
     */
    public interface ChatHistoryCallback {
        void onHistoryLoaded(ChatHistoryResponse response, String error);
    }

    /**
     * 获取 Gson 实例（用于解析JSON）
     */
    public Gson getGson() {
        return gson;
    }
}