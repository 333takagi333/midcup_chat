package com.chat.service;

import javafx.application.Platform;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息广播中心，用于将消息同时发送给多个监听器
 */
public class MessageBroadcaster {

    private static MessageBroadcaster instance;

    // 存储私聊消息监听器：key = "private_userId_contactId"
    private final Map<String, List<PrivateMessageListener>> privateListeners = new ConcurrentHashMap<>();

    // 存储群聊消息监听器：key = "group_groupId"
    private final Map<String, List<GroupMessageListener>> groupListeners = new ConcurrentHashMap<>();

    // 存储全局聊天列表更新监听器
    private final List<ChatListUpdateListener> chatListListeners = new ArrayList<>();

    // 用于防止重复处理消息
    private final Map<String, Long> lastProcessedMessages = new ConcurrentHashMap<>();

    // 当前用户ID - 用于过滤自己发送的消息
    private Long currentUserId = null;

    private MessageBroadcaster() {}

    public static synchronized MessageBroadcaster getInstance() {
        if (instance == null) {
            instance = new MessageBroadcaster();
        }
        return instance;
    }

    // 私聊消息监听器接口
    public interface PrivateMessageListener {
        void onPrivateMessageReceived(Long fromUserId, Long toUserId, String content, long timestamp, Long messageId);
    }

    // 群聊消息监听器接口
    public interface GroupMessageListener {
        void onGroupMessageReceived(Long groupId, Long fromUserId, String content, long timestamp, Long messageId);
    }

    // 聊天列表更新监听器接口
    public interface ChatListUpdateListener {
        void onNewPrivateMessage(Long contactId, String contactName, String content, long timestamp, Long messageId);
        void onNewGroupMessage(Long groupId, String groupName, String content, long timestamp, Long messageId);
    }

    // ========== 配置方法 ==========

    /**
     * 设置当前用户ID
     */
    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
        System.out.println("[MessageBroadcaster] 设置当前用户ID: " + userId);
    }

    /**
     * 获取当前用户ID
     */
    public Long getCurrentUserId() {
        return currentUserId;
    }

    // ========== 注册/注销方法 ==========

    /**
     * 注册私聊监听器
     */
    public void registerPrivateListener(String key, PrivateMessageListener listener) {
        privateListeners.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
        System.out.println("[MessageBroadcaster] 注册私聊监听器: " + key);
    }

    /**
     * 移除私聊监听器
     */
    public void unregisterPrivateListener(String key, PrivateMessageListener listener) {
        List<PrivateMessageListener> listeners = privateListeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
            System.out.println("[MessageBroadcaster] 移除私聊监听器: " + key);
            if (listeners.isEmpty()) {
                privateListeners.remove(key);
            }
        }
    }

    /**
     * 注册群聊监听器
     */
    public void registerGroupListener(String groupId, GroupMessageListener listener) {
        groupListeners.computeIfAbsent(groupId, k -> new ArrayList<>()).add(listener);
        System.out.println("[MessageBroadcaster] 注册群聊监听器: " + groupId);
    }

    /**
     * 移除群聊监听器
     */
    public void unregisterGroupListener(String groupId, GroupMessageListener listener) {
        List<GroupMessageListener> listeners = groupListeners.get(groupId);
        if (listeners != null) {
            listeners.remove(listener);
            System.out.println("[MessageBroadcaster] 移除群聊监听器: " + groupId);
            if (listeners.isEmpty()) {
                groupListeners.remove(groupId);
            }
        }
    }

    /**
     * 注册聊天列表监听器
     */
    public void registerChatListListener(ChatListUpdateListener listener) {
        if (!chatListListeners.contains(listener)) {
            chatListListeners.add(listener);
            System.out.println("[MessageBroadcaster] 注册聊天列表监听器，总数: " + chatListListeners.size());
        }
    }

    /**
     * 移除聊天列表监听器
     */
    public void unregisterChatListListener(ChatListUpdateListener listener) {
        chatListListeners.remove(listener);
        System.out.println("[MessageBroadcaster] 移除聊天列表监听器，剩余: " + chatListListeners.size());
    }

    // ========== 消息处理核心方法 ==========

    /**
     * 检查是否为重复消息
     */
    private boolean isDuplicateMessage(String messageKey, Long messageId) {
        if (messageId == null) {
            return false;
        }

        Long lastId = lastProcessedMessages.get(messageKey);
        if (lastId != null && lastId.equals(messageId)) {
            return true;
        }

        lastProcessedMessages.put(messageKey, messageId);

        // 保持缓存大小，避免内存泄漏
        if (lastProcessedMessages.size() > 1000) {
            // 删除最老的100个条目
            int count = 0;
            List<String> toRemove = new ArrayList<>();
            for (String key : lastProcessedMessages.keySet()) {
                toRemove.add(key);
                if (++count >= 100) break;
            }
            for (String key : toRemove) {
                lastProcessedMessages.remove(key);
            }
        }

        return false;
    }

    /**
     * 广播私聊消息（核心方法）- 显示最后一人发的消息
     */
    public void broadcastPrivateMessage(Long fromUserId, Long toUserId, String content,
                                        long timestamp, String contactName, Long messageId) {
        // 生成消息唯一标识
        String messageKey = "private_" + fromUserId + "_" + toUserId + "_" + timestamp;

        // 检查是否为重复消息
        if (isDuplicateMessage(messageKey, messageId)) {
            System.out.println("[MessageBroadcaster] 跳过重复的私聊消息: " + messageKey);
            return;
        }

        System.out.println("[MessageBroadcaster] 广播私聊消息: " + fromUserId + " -> " + toUserId +
                ", 当前用户: " + currentUserId + ", 消息ID: " + messageId + ", 内容: " +
                (content.length() > 20 ? content.substring(0, 20) + "..." : content));

        // 判断当前用户是发送方还是接收方
        boolean isCurrentUserSender = currentUserId != null && currentUserId.equals(fromUserId);
        boolean isCurrentUserReceiver = currentUserId != null && currentUserId.equals(toUserId);
        boolean isFromCurrentUser = isCurrentUserSender;

        // ========== 1. 保存到会话管理器 ==========
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String time = timeFormat.format(new Date(timestamp));

        // 生成显示消息
        String senderDisplayName = isFromCurrentUser ? "我" : contactName;
        String displayMessage = String.format("[%s] %s: %s", time, senderDisplayName, content);

        // 保存到会话管理器
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();

        if (isCurrentUserSender) {
            // 当前用户是发送方，保存到发送方会话
            sessionManager.addPrivateMessage(fromUserId, toUserId, displayMessage);
            System.out.println("[MessageBroadcaster] 保存到发送方会话: " + fromUserId + " -> " + toUserId);
        } else if (isCurrentUserReceiver) {
            // 当前用户是接收方，保存到接收方会话
            sessionManager.addPrivateMessage(toUserId, fromUserId, displayMessage);
            System.out.println("[MessageBroadcaster] 保存到接收方会话: " + toUserId + " <- " + fromUserId);
        }

        // ========== 2. 更新最近消息服务（显示最后一人发的消息） ==========
        RecentMessageService recentService = RecentMessageService.getInstance();

        // 确定聊天ID和显示名称
        String chatId;
        String chatDisplayName;

        if (isCurrentUserReceiver) {
            // 当前用户是接收方，聊天ID为对方ID，显示对方名称
            chatId = fromUserId.toString();
            chatDisplayName = contactName;
        } else if (isCurrentUserSender) {
            // 当前用户是发送方，聊天ID为对方ID，显示"我"（表示这是与对方的对话）
            chatId = toUserId.toString();
            chatDisplayName = "我";
        } else {
            // 当前用户不是参与者，不更新最近消息
            chatId = null;
            chatDisplayName = null;
        }

        if (chatId != null) {
            recentService.updateRecentMessage(
                    chatId,
                    chatDisplayName,
                    senderDisplayName, // 发送者名称：我 或 对方名称
                    content,
                    "", // 头像URL
                    false, // 不是群聊
                    isFromCurrentUser // 是否当前用户发送
            );

            System.out.println("[MessageBroadcaster] 更新最近消息: " + chatDisplayName +
                    " (" + senderDisplayName + "发送)");
        }

        // ========== 3. 通知聊天列表（只通知接收方） ==========
        Platform.runLater(() -> {
            for (ChatListUpdateListener listener : chatListListeners) {
                try {
                    // 只有当当前用户是接收方时，才更新聊天列表
                    // 发送方不应该在自己的聊天列表中看到自己发送的消息
                    if (isCurrentUserReceiver) {
                        listener.onNewPrivateMessage(fromUserId, contactName, content, timestamp, messageId);
                        System.out.println("[MessageBroadcaster] 通知接收方聊天列表: " + fromUserId + " -> " + toUserId);
                    }
                } catch (Exception e) {
                    System.err.println("[MessageBroadcaster] 通知聊天列表监听器失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        // ========== 4. 通知接收方相关的私聊窗口 ==========
        // 接收方视角：private_接收方ID_发送方ID
        String receiverKey = "private_" + toUserId + "_" + fromUserId;
        List<PrivateMessageListener> receiverListeners = privateListeners.get(receiverKey);
        if (receiverListeners != null && !receiverListeners.isEmpty()) {
            Platform.runLater(() -> {
                for (PrivateMessageListener listener : receiverListeners) {
                    try {
                        listener.onPrivateMessageReceived(fromUserId, toUserId, content, timestamp, messageId);
                        System.out.println("[MessageBroadcaster] 通知接收方窗口: " + receiverKey);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 通知接收方私聊监听器失败: " + e.getMessage());
                    }
                }
            });
        }

        // ========== 5. 通知发送方相关的私聊窗口 ==========
        // 发送方视角：private_发送方ID_接收方ID
        String senderKey = "private_" + fromUserId + "_" + toUserId;
        List<PrivateMessageListener> senderListeners = privateListeners.get(senderKey);
        if (senderListeners != null && !senderListeners.isEmpty()) {
            Platform.runLater(() -> {
                for (PrivateMessageListener listener : senderListeners) {
                    try {
                        listener.onPrivateMessageReceived(fromUserId, toUserId, content, timestamp, messageId);
                        System.out.println("[MessageBroadcaster] 通知发送方窗口: " + senderKey);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 通知发送方私聊监听器失败: " + e.getMessage());
                    }
                }
            });
        }

        // ========== 6. 特殊处理：发送方自我反馈 ==========
        if (isCurrentUserSender) {
            System.out.println("[MessageBroadcaster] 发送方消息已处理: " + fromUserId + " -> " + toUserId);

            // 如果发送方没有打开聊天窗口，这里可以做一些额外处理
            if ((receiverListeners == null || receiverListeners.isEmpty()) &&
                    (senderListeners == null || senderListeners.isEmpty())) {
                System.out.println("[MessageBroadcaster] 双方都未打开聊天窗口，消息已保存到会话");
            }
        }
    }

    /**
     * 广播群聊消息 - 显示最后一人发的消息
     */
    public void broadcastGroupMessage(Long groupId, Long fromUserId, String content,
                                      long timestamp, String groupName, Long messageId) {
        // 生成消息唯一标识
        String messageKey = "group_" + groupId + "_" + fromUserId + "_" + timestamp;

        // 检查是否为重复消息
        if (isDuplicateMessage(messageKey, messageId)) {
            System.out.println("[MessageBroadcaster] 跳过重复的群聊消息: " + messageKey);
            return;
        }

        System.out.println("[MessageBroadcaster] 广播群聊消息: 群组" + groupId + ", 发送者" + fromUserId +
                ", 当前用户: " + currentUserId + ", 内容: " +
                (content.length() > 20 ? content.substring(0, 20) + "..." : content));

        // 判断是否来自当前用户
        boolean isFromCurrentUser = currentUserId != null && currentUserId.equals(fromUserId);

        // ========== 1. 保存到会话管理器 ==========
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String time = timeFormat.format(new Date(timestamp));

        // 生成显示消息
        String senderDisplayName = isFromCurrentUser ? "我" : "用户" + fromUserId;
        String displayMessage = String.format("[%s] %s: %s", time, senderDisplayName, content);

        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        sessionManager.addGroupMessage(groupId, displayMessage);

        System.out.println("[MessageBroadcaster] 保存到群聊会话: " + groupName);

        // ========== 2. 更新最近消息服务（群聊） ==========
        RecentMessageService recentService = RecentMessageService.getInstance();

        // 更新群聊最近消息（显示最后一人发的消息）
        recentService.updateRecentMessage(
                groupId.toString(),
                groupName,
                senderDisplayName, // 发送者名称：我 或 用户X
                content,
                "", // 群聊头像
                true, // 是群聊
                isFromCurrentUser // 是否当前用户发送
        );

        System.out.println("[MessageBroadcaster] 更新群聊最近消息: " + groupName +
                " (" + senderDisplayName + "发送)");

        // ========== 3. 通知聊天列表（群聊消息总是通知） ==========
        Platform.runLater(() -> {
            for (ChatListUpdateListener listener : chatListListeners) {
                try {
                    listener.onNewGroupMessage(groupId, groupName, content, timestamp, messageId);
                    System.out.println("[MessageBroadcaster] 通知群聊列表: " + groupName);
                } catch (Exception e) {
                    System.err.println("[MessageBroadcaster] 通知聊天列表监听器失败: " + e.getMessage());
                }
            }
        });

        // ========== 4. 通知所有打开该群聊的窗口 ==========
        List<GroupMessageListener> listeners = groupListeners.get(groupId.toString());
        if (listeners != null && !listeners.isEmpty()) {
            Platform.runLater(() -> {
                for (GroupMessageListener listener : listeners) {
                    try {
                        listener.onGroupMessageReceived(groupId, fromUserId, content, timestamp, messageId);
                        System.out.println("[MessageBroadcaster] 通知群聊窗口: " + groupId);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 通知群聊监听器失败: " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 广播发送方立即看到自己的消息（用于消息发送后的即时反馈）
     */
    public void broadcastSelfMessageForImmediateFeedback(Long fromUserId, Long toUserId, String content,
                                                         long timestamp, String contactName) {
        System.out.println("[MessageBroadcaster] 广播发送方即时反馈: " + fromUserId + " -> " + toUserId);

        // 只通知发送方的聊天窗口
        String senderKey = "private_" + fromUserId + "_" + toUserId;
        List<PrivateMessageListener> senderListeners = privateListeners.get(senderKey);

        if (senderListeners != null && !senderListeners.isEmpty()) {
            Platform.runLater(() -> {
                for (PrivateMessageListener listener : senderListeners) {
                    try {
                        listener.onPrivateMessageReceived(fromUserId, toUserId, content, timestamp, null);
                        System.out.println("[MessageBroadcaster] 发送方即时反馈: " + senderKey);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 发送方即时反馈失败: " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 广播历史消息（用于从服务器加载的历史消息）
     */
    public void broadcastHistoricalPrivateMessage(Long fromUserId, Long toUserId, String content,
                                                  long timestamp, String contactName, Long messageId,
                                                  boolean isFromCurrentUser) {

        System.out.println("[MessageBroadcaster] 广播历史私聊消息: " + fromUserId + " -> " + toUserId +
                ", 消息ID: " + messageId + ", 来自当前用户: " + isFromCurrentUser);

        // 生成消息唯一标识
        String messageKey = "history_private_" + fromUserId + "_" + toUserId + "_" + messageId;

        // 检查是否为重复历史消息
        if (isDuplicateMessage(messageKey, messageId)) {
            System.out.println("[MessageBroadcaster] 跳过重复的历史消息: " + messageKey);
            return;
        }

        // 只通知窗口显示历史消息，不更新最近消息服务

        // 通知接收方相关的私聊窗口
        String receiverKey = "private_" + toUserId + "_" + fromUserId;
        List<PrivateMessageListener> receiverListeners = privateListeners.get(receiverKey);
        if (receiverListeners != null && !receiverListeners.isEmpty()) {
            Platform.runLater(() -> {
                for (PrivateMessageListener listener : receiverListeners) {
                    try {
                        listener.onPrivateMessageReceived(fromUserId, toUserId, content, timestamp, messageId);
                        System.out.println("[MessageBroadcaster] 通知接收方历史消息: " + receiverKey);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 通知接收方历史消息失败: " + e.getMessage());
                    }
                }
            });
        }

        // 通知发送方相关的私聊窗口
        String senderKey = "private_" + fromUserId + "_" + toUserId;
        List<PrivateMessageListener> senderListeners = privateListeners.get(senderKey);
        if (senderListeners != null && !senderListeners.isEmpty()) {
            Platform.runLater(() -> {
                for (PrivateMessageListener listener : senderListeners) {
                    try {
                        listener.onPrivateMessageReceived(fromUserId, toUserId, content, timestamp, messageId);
                        System.out.println("[MessageBroadcaster] 通知发送方历史消息: " + senderKey);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 通知发送方历史消息失败: " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 广播文件消息
     */
    public void broadcastFileMessage(String chatType, Long senderId, Long targetId,
                                     String fileName, long fileSize, String fileType,
                                     String downloadUrl, long timestamp) {

        String messageKey = String.format("file_%s_%d_%d_%s_%d",
                chatType, senderId, targetId, fileName, timestamp);

        if (isDuplicateMessage(messageKey, null)) {
            return;
        }

        System.out.println("[MessageBroadcaster] 广播文件消息: " + fileName + " (" + chatType + ")");

        // 根据聊天类型处理
        if ("private".equals(chatType)) {
            // 私聊文件消息
            String senderKey = "private_" + senderId + "_" + targetId;
            String receiverKey = "private_" + targetId + "_" + senderId;

            // 更新最近消息服务
            RecentMessageService recentService = RecentMessageService.getInstance();
            boolean isFromCurrentUser = currentUserId != null && currentUserId.equals(senderId);

            // 确定聊天ID和名称
            String chatId;
            String chatName;
            if (currentUserId != null && currentUserId.equals(targetId)) {
                // 当前用户是接收方
                chatId = senderId.toString();
                chatName = "用户" + senderId;
            } else if (isFromCurrentUser) {
                // 当前用户是发送方
                chatId = targetId.toString();
                chatName = "我";
            } else {
                chatId = null;
                chatName = null;
            }

            if (chatId != null) {
                recentService.updateRecentMessage(
                        chatId,
                        chatName,
                        isFromCurrentUser ? "我" : "用户" + senderId,
                        "[" + fileType + "] " + fileName,
                        "",
                        false,
                        isFromCurrentUser
                );
            }

        } else if ("group".equals(chatType)) {
            // 群聊文件消息
            RecentMessageService recentService = RecentMessageService.getInstance();
            boolean isFromCurrentUser = currentUserId != null && currentUserId.equals(senderId);

            // 这里需要获取群聊名称，简化处理
            String groupName = "群聊" + targetId;

            recentService.updateRecentMessage(
                    targetId.toString(),
                    groupName,
                    isFromCurrentUser ? "我" : "用户" + senderId,
                    "[" + fileType + "] " + fileName,
                    "",
                    true,
                    isFromCurrentUser
            );
        }
    }

    /**
     * 清理缓存和监听器
     */
    public void cleanup() {
        lastProcessedMessages.clear();
        privateListeners.clear();
        groupListeners.clear();
        chatListListeners.clear();
        currentUserId = null;
        System.out.println("[MessageBroadcaster] 已清理所有缓存和监听器");
    }

    /**
     * 获取监听器统计信息（用于调试）
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("MessageBroadcaster 统计:\n");
        stats.append("当前用户ID: ").append(currentUserId).append("\n");
        stats.append("私聊监听器: ").append(privateListeners.size()).append(" 个键\n");
        stats.append("群聊监听器: ").append(groupListeners.size()).append(" 个键\n");
        stats.append("聊天列表监听器: ").append(chatListListeners.size()).append(" 个\n");
        stats.append("消息缓存: ").append(lastProcessedMessages.size()).append(" 条\n");

        // 添加最近消息统计
        try {
            RecentMessageService recentService = RecentMessageService.getInstance();
            stats.append("最近消息: ").append(recentService.getAllRecentMessages().size()).append(" 条\n");
        } catch (Exception e) {
            stats.append("最近消息: 无法获取\n");
        }

        return stats.toString();
    }
}