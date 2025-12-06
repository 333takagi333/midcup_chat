package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.ChatPrivateSend;
import com.chat.protocol.ChatGroupSend;
import com.google.gson.Gson;

/**
 * 聊天业务逻辑服务
 */
public class ChatService {

    private final Gson gson = new Gson();

    /**
     * 发送私聊消息
     */
    public boolean sendPrivateMessage(SocketClient client, Long contactId, Long userId, String content) {
        try {
            ChatPrivateSend message = new ChatPrivateSend();
            message.setToUserId(contactId);
            message.setFromUserId(userId);
            message.setContent(content);
            return client.sendPrivateMessage(message);
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
            return client.sendGroupMessage(message);
        } catch (Exception e) {
            System.err.println("发送群聊消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 接收消息
     */
    public String receiveMessage(SocketClient client) {
        return client.receiveMessage();
    }
}