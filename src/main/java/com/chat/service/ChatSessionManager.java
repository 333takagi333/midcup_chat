package com.chat.service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天会话管理器 - 单例模式，管理登录期间的所有聊天会话
 */
public class ChatSessionManager {

    private static ChatSessionManager instance;

    // 登录时间戳
    private final long loginTimestamp;

    // 存储私聊会话记录：key = "private_userId_contactId"
    private final Map<String, List<String>> privateSessions = new ConcurrentHashMap<>();

    // 存储群聊会话记录：key = "group_groupId"
    private final Map<String, List<String>> groupSessions = new ConcurrentHashMap<>();

    private ChatSessionManager() {
        this.loginTimestamp = System.currentTimeMillis();
        System.out.println("[ChatSessionManager] 初始化，登录时间: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(loginTimestamp)));
    }

    public static synchronized ChatSessionManager getInstance() {
        if (instance == null) {
            instance = new ChatSessionManager();
        }
        return instance;
    }

    /**
     * 添加私聊消息到会话
     */
    public void addPrivateMessage(Long userId, Long contactId, String message) {
        String key = buildPrivateKey(userId, contactId);
        List<String> messages = privateSessions.computeIfAbsent(key, k -> new ArrayList<>());
        messages.add(message);

        // 限制会话大小，避免内存泄漏
        if (messages.size() > 500) {
            messages.remove(0);
        }

        System.out.println("[ChatSessionManager] 添加到本次登录会话 " + key + ", 当前消息数: " + messages.size());
    }

    /**
     * 添加群聊消息到会话
     */
    public void addGroupMessage(Long groupId, String message) {
        String key = buildGroupKey(groupId);
        List<String> messages = groupSessions.computeIfAbsent(key, k -> new ArrayList<>());
        messages.add(message);

        // 限制会话大小
        if (messages.size() > 1000) {
            messages.remove(0);
        }

        System.out.println("[ChatSessionManager] 添加到本次登录会话 " + key + ", 当前消息数: " + messages.size());
    }

    /**
     * 获取私聊会话记录
     */
    public List<String> getPrivateSession(Long userId, Long contactId) {
        String key = buildPrivateKey(userId, contactId);
        return privateSessions.getOrDefault(key, new ArrayList<>());
    }

    /**
     * 获取群聊会话记录
     */
    public List<String> getGroupSession(Long groupId) {
        String key = buildGroupKey(groupId);
        return groupSessions.getOrDefault(key, new ArrayList<>());
    }

    /**
     * 清空所有会话记录（退出登录时调用）
     */
    public void clearAllSessions() {
        privateSessions.clear();
        groupSessions.clear();
        System.out.println("[ChatSessionManager] 已清空所有聊天会话记录");
    }

    /**
     * 清除指定用户的旧会话记录，保留本次登录的
     */
    public void clearOldSessions(Long userId) {
        // 这里可以添加逻辑来清除除当前用户外的其他会话
        // 目前实现中，所有会话都会在clearAllSessions()时清空
        System.out.println("[ChatSessionManager] 保留用户" + userId + "的会话记录");
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        int privateCount = 0;
        int groupCount = 0;

        for (List<String> session : privateSessions.values()) {
            privateCount += session.size();
        }

        for (List<String> session : groupSessions.values()) {
            groupCount += session.size();
        }

        return String.format("本次登录聊天会话: %d个私聊会话(%d条消息), %d个群聊会话(%d条消息)",
                privateSessions.size(), privateCount,
                groupSessions.size(), groupCount);
    }

    /**
     * 获取登录时间戳
     */
    public long getLoginTimestamp() {
        return loginTimestamp;
    }

    private String buildPrivateKey(Long userId, Long contactId) {
        // 生成对称的key，确保A和B的对话在同一个会话中
        Long smaller = Math.min(userId, contactId);
        Long larger = Math.max(userId, contactId);
        return "private_" + smaller + "_" + larger;
    }

    private String buildGroupKey(Long groupId) {
        return "group_" + groupId;
    }
}