package com.chat.service;

import com.chat.model.ChatItem;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 最近消息服务 - 管理消息栏显示逻辑
 */
public class RecentMessageService {
    private static RecentMessageService instance;

    // 存储每个聊天的最新一条消息（用于消息栏显示）
    private final Map<String, ChatItem> recentMessages = new ConcurrentHashMap<>();

    // 存储每个聊天的未读消息数量
    private final Map<String, Integer> unreadCounts = new ConcurrentHashMap<>();

    // 消息栏最大显示长度
    private static final int MAX_PREVIEW_LENGTH = 30;

    private RecentMessageService() {
        System.out.println("[RecentMessageService] 初始化");
    }

    public static synchronized RecentMessageService getInstance() {
        if (instance == null) {
            instance = new RecentMessageService();
        }
        return instance;
    }

    /**
     * 更新聊天的最新消息（显示最后一人发的消息）
     * @param chatId 聊天ID（私聊：对方ID，群聊：群组ID）
     * @param chatName 聊天名称
     * @param senderName 发送者名称
     * @param content 消息内容
     * @param avatarUrl 头像URL
     * @param isGroup 是否群聊
     * @param isFromCurrentUser 是否当前用户发送
     */
    public void updateRecentMessage(String chatId, String chatName, String senderName,
                                    String content, String avatarUrl, boolean isGroup,
                                    boolean isFromCurrentUser) {

        // 格式化预览消息：显示"发送者: 内容"或"我: 内容"
        String previewContent;
        if (isFromCurrentUser) {
            previewContent = "我: " + content;
        } else {
            previewContent = senderName + ": " + content;
        }

        // 截取消息预览
        String preview;
        if (previewContent.length() > MAX_PREVIEW_LENGTH) {
            preview = previewContent.substring(0, MAX_PREVIEW_LENGTH) + "...";
        } else {
            preview = previewContent;
        }

        // 当前时间
        String currentTime = new SimpleDateFormat("HH:mm").format(new Date());

        // 获取未读数量（如果不是当前用户发送的，增加未读数）
        int unreadCount = unreadCounts.getOrDefault(chatId, 0);
        if (!isFromCurrentUser) {
            unreadCount++;
            unreadCounts.put(chatId, unreadCount);
        }

        // 创建 ChatItem
        ChatItem chatItem = new ChatItem(
                chatId,
                chatName,
                preview,
                currentTime,
                avatarUrl,
                unreadCount > 0,  // 有未读消息时显示红点
                isGroup
        );

        recentMessages.put(chatId, chatItem);
        System.out.println("[RecentMessageService] 更新消息栏: " + chatName +
                " - " + preview + " (未读: " + unreadCount + ")");
    }

    /**
     * 标记为已读（当用户打开聊天窗口时调用）
     */
    public void markAsRead(String chatId) {
        // 清除未读计数
        unreadCounts.put(chatId, 0);

        // 更新对应的 ChatItem，去掉红点
        ChatItem oldItem = recentMessages.get(chatId);
        if (oldItem != null) {
            ChatItem readItem = new ChatItem(
                    oldItem.getId(),
                    oldItem.getName(),
                    oldItem.getLastMessage(),
                    oldItem.getTime(),
                    oldItem.getAvatarUrl(),
                    false,  // 标记为已读，无红点
                    oldItem.isGroup()
            );
            recentMessages.put(chatId, readItem);
            System.out.println("[RecentMessageService] 标记为已读: " + oldItem.getName());
        }
    }

    /**
     * 获取指定聊天的最新消息预览
     */
    public ChatItem getRecentMessage(String chatId) {
        return recentMessages.get(chatId);
    }

    /**
     * 获取所有最近消息（用于初始化消息栏）
     */
    public List<ChatItem> getAllRecentMessages() {
        return new ArrayList<>(recentMessages.values());
    }

    /**
     * 获取未读消息数量
     */
    public int getUnreadCount(String chatId) {
        return unreadCounts.getOrDefault(chatId, 0);
    }

    /**
     * 清除所有最近消息
     */
    public void clearAll() {
        recentMessages.clear();
        unreadCounts.clear();
        System.out.println("[RecentMessageService] 已清空所有最近消息");
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        int total = recentMessages.size();
        int totalUnread = 0;

        for (Integer count : unreadCounts.values()) {
            totalUnread += count;
        }

        return String.format("最近消息: %d个聊天，%d条未读消息", total, totalUnread);
    }
}