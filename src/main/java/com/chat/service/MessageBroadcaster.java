package com.chat.service;

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // 添加文件消息监听器接口
    public interface FileMessageListener {
        void onFileMessageReceived(String chatType, Long senderId, Long targetId,
                                   String fileName, long fileSize, String fileType,
                                   String downloadUrl, long timestamp);
    }

    // 添加文件消息广播方法
    public void broadcastFileMessage(String chatType, Long senderId, Long targetId,
                                     String fileName, long fileSize, String fileType,
                                     String downloadUrl, long timestamp) {

        String messageKey = String.format("file_%s_%d_%d_%s_%d",
                chatType, senderId, targetId, fileName, timestamp);

        if (isDuplicateMessage(messageKey, null)) {
            return;
        }

        // 根据聊天类型广播
        if ("private".equals(chatType)) {
            // 广播给私聊窗口
            String senderKey = "private_" + senderId + "_" + targetId;
            String receiverKey = "private_" + targetId + "_" + senderId;

            // 类似文本消息的广播逻辑
        } else if ("group".equals(chatType)) {
            // 广播给群聊窗口
            List<GroupMessageListener> listeners = groupListeners.get(targetId.toString());
            if (listeners != null) {
                Platform.runLater(() -> {
                    for (GroupMessageListener listener : listeners) {
                        // 可以调用新的方法或使用现有方法
                    }
                });
            }
        }
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
     * 广播私聊消息（核心方法）
     * 关键：接收方会在聊天列表中看到消息，发送方不会
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
                ", 当前用户: " + currentUserId + ", 内容长度: " + (content != null ? content.length() : 0));

        // ========== 1. 通知聊天列表 ==========
        Platform.runLater(() -> {
            for (ChatListUpdateListener listener : chatListListeners) {
                try {
                    // 关键逻辑：判断当前用户是发送方还是接收方
                    if (currentUserId != null) {
                        if (currentUserId.equals(toUserId)) {
                            // 当前用户是接收方 - 应该在聊天列表中看到发送方的消息
                            listener.onNewPrivateMessage(fromUserId, contactName, content, timestamp, messageId);
                            System.out.println("[MessageBroadcaster] 通知接收方聊天列表: " + fromUserId + " -> " + toUserId);
                        }
                        // 注意：发送方不应该在自己的聊天列表中看到自己
                        // 所以当 currentUserId.equals(fromUserId) 时，不通知聊天列表
                    } else {
                        // 如果没有设置当前用户ID，默认通知所有监听器（兼容模式）
                        listener.onNewPrivateMessage(fromUserId, contactName, content, timestamp, messageId);
                    }
                } catch (Exception e) {
                    System.err.println("[MessageBroadcaster] 通知聊天列表监听器失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        // ========== 2. 通知接收方相关的私聊窗口 ==========
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

        // ========== 3. 通知发送方相关的私聊窗口 ==========
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

        // ========== 4. 特殊处理：如果接收方没有打开窗口，确保发送方能看到自己的消息 ==========
        // 发送方可能在窗口中看到自己发送的消息，即使接收方没有打开窗口
        if (currentUserId != null && currentUserId.equals(fromUserId)) {
            // 确保发送方能在自己的聊天窗口中看到消息
            if ((receiverListeners == null || receiverListeners.isEmpty()) &&
                    (senderListeners == null || senderListeners.isEmpty())) {
                // 如果双方都没有打开窗口，但发送方需要立即看到反馈
                System.out.println("[MessageBroadcaster] 消息已发送，但双方都没有打开聊天窗口");
            }
        }
    }

    /**
     * 广播群聊消息
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
                ", 当前用户: " + currentUserId + ", 内容长度: " + (content != null ? content.length() : 0));

        // ========== 1. 通知聊天列表 ==========
        // 群聊消息总是通知聊天列表（无论谁发送的）
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

        // ========== 2. 通知所有打开该群聊的窗口 ==========
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
     * 专门用于发送方立即看到自己的消息（不触发聊天列表更新）
     */
    public void broadcastSelfMessage(Long fromUserId, Long toUserId, String content,
                                     long timestamp, String contactName) {
        System.out.println("[MessageBroadcaster] 广播发送方自己的消息: " + fromUserId + " -> " + toUserId);

        // 只通知发送方的聊天窗口，不通知聊天列表
        String senderKey = "private_" + fromUserId + "_" + toUserId;
        List<PrivateMessageListener> senderListeners = privateListeners.get(senderKey);
        if (senderListeners != null && !senderListeners.isEmpty()) {
            Platform.runLater(() -> {
                for (PrivateMessageListener listener : senderListeners) {
                    try {
                        listener.onPrivateMessageReceived(fromUserId, toUserId, content, timestamp, null);
                        System.out.println("[MessageBroadcaster] 通知发送方自己看到消息: " + senderKey);
                    } catch (Exception e) {
                        System.err.println("[MessageBroadcaster] 通知发送方自己失败: " + e.getMessage());
                    }
                }
            });
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
        return stats.toString();
    }
}