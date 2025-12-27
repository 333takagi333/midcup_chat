package com.chat.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.chat.protocol.*;

/**
 * 简单的基于 TCP 的客户端，用于向服务器发送请求并获取响应。
 */
public class SocketClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int TIMEOUT_MS = 5000; // 5 seconds

    private boolean connected = false;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();

    /**
     * 建立与服务器的连接
     * @return 连接是否成功
     */
    public boolean connect() {
        try {
            if (connected) {
                disconnect(); // 先断开现有连接
            }

            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            socket.setSoTimeout(TIMEOUT_MS);

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            connected = true;
            return true;

        } catch (IOException e) {
            System.err.println("[SOCKET] Connection failed: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * 发送请求到服务器并获取响应（自动管理连接）
     * @param data 请求数据对象
     * @return 服务器响应字符串，失败返回 null
     */
    public String sendRequest(Object data) {
        try {
            // 确保连接建立
            if (!connected && !connect()) {
                System.err.println("[SOCKET] 连接失败，无法发送请求");
                return null;
            }

            // 发送请求
            String json = gson.toJson(data);
            System.out.println("[SOCKET] 发送请求: " + json);

            // 确保输出流可用
            if (out == null) {
                System.err.println("[SOCKET] 输出流为空");
                return null;
            }

            out.println(json);
            out.flush(); // 确保数据发送

            // 读取响应
            if (in == null) {
                System.err.println("[SOCKET] 输入流为空");
                return null;
            }

            String response = in.readLine();
            System.out.println("[SOCKET] 收到响应: " + (response != null ? response : "null"));
            return response;

        } catch (SocketTimeoutException e) {
            System.err.println("[SOCKET] 请求超时: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("[SOCKET] 请求失败: " + e.getMessage());
            connected = false; // 标记连接断开
            return null;
        } catch (Exception e) {
            System.err.println("[SOCKET] 请求异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 发送请求并接收响应（带超时的专用方法）
     * @param data 请求数据对象
     * @return 服务器响应字符串，失败返回 null
     */
    public String sendAndReceive(Object data) {
        return sendAndReceive(data, 5000); // 默认5秒超时
    }

    /**
     * 发送请求并接收响应（带自定义超时）
     * @param data 请求数据对象
     * @param timeoutMs 超时时间（毫秒）
     * @return 服务器响应字符串，失败返回 null
     */
    public String sendAndReceive(Object data, long timeoutMs) {
        try {
            // 确保连接建立
            if (!connected && !connect()) {
                return null;
            }

            // 发送请求
            String json = gson.toJson(data);
            System.out.println("[SOCKET] Sending request: " + json);
            out.println(json);

            // 等待响应
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    // 检查是否有数据可读
                    if (in.ready()) {
                        String response = in.readLine();
                        if (response != null) {
                            System.out.println("[SOCKET] Received response: " + response);
                            return response;
                        }
                    }

                    // 短暂休眠避免CPU占用过高
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            System.out.println("[SOCKET] Request timeout after " + timeoutMs + "ms");
            return null;

        } catch (IOException e) {
            System.err.println("[SOCKET] Request failed: " + e.getMessage());
            connected = false;
            return null;
        }
    }

    // ==================== 用户相关请求 ====================

    /**
     * 发送登录请求
     */
    public String sendLoginRequest(LoginRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送注册请求
     */
    public String sendRegisterRequest(RegisterRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送重置密码请求
     */
    public String sendResetPasswordRequest(ResetPasswordRequest request) {
        return sendRequest(request);
    }

    public String sendChangePasswordRequest(ChangePasswordRequest request) {
        return sendRequest(request);
    }
    /**
     * 发送用户信息请求
     */
    public String sendUserInfoRequest(UserInfoRequest request) {
        return sendRequest(request);
    }
    /**
     * 发送更新资料请求
     */
    public String sendUpdateProfileRequest(UpdateProfileRequest request) {
        return sendRequest(request);
    }

    // ==================== 聊天相关请求 ====================

    /**
     * 发送私聊消息
     */
    public boolean sendPrivateMessage(ChatPrivateSend message) {
        return sendMessage(message);
    }

    /**
     * 发送群聊消息
     */
    public boolean sendGroupMessage(ChatGroupSend message) {
        return sendMessage(message);
    }

    /**
     * 请求聊天记录
     */
    public String sendChatHistoryRequest(ChatHistoryRequest request) {
        return sendRequest(request);
    }

    // ==================== 好友系统请求 ====================

    /**
     * 发送添加好友请求
     */
    public String sendFriendAddRequest(FriendAddRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送好友列表请求
     */
    public String sendFriendListRequest(FriendListRequest request) {
        return sendRequest(request);
    }

    // ==================== 群组系统请求 ====================

    /**
     * 发送群组列表请求
     */
    public String sendGroupListRequest(GroupListRequest request) {
        return sendRequest(request);
    }
    // ==================== 好友详情相关请求 ====================

    /**
     * 发送好友详情请求
     */
    public String sendFriendDetailRequest(FriendDetailRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送删除好友请求
     */
    public String sendDeleteFriendRequest(DeleteFriendRequest request) {
        return sendRequest(request);
    }



    // ==================== 群聊详情相关请求 ====================

    /**
     * 发送群聊详情请求
     */
    public String sendGroupDetailRequest(GroupDetailRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送退出群聊请求
     */
    public String sendExitGroupRequest(ExitGroupRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送文件上传请求
     */
    public String sendFileUploadRequest(FileUploadRequest request) {
        return sendRequest(request);
    }

    /**
     * 发送文件下载请求
     */
    public String sendFileDownloadRequest(FileDownloadRequest request) {
        return sendRequest(request);
    }
    /**
     * 发送私聊文件消息
     */
    public boolean sendPrivateFileMessage(FilePrivateSend message) {
        return sendMessage(message);
    }

    /**
     * 发送群聊文件消息
     */
    public boolean sendGroupFileMessage(FileGroupSend message) {
        return sendMessage(message);
    }
    /**
     * 发送添加群成员请求
     */
    public String sendGroupAddMemberRequest(GroupAddMemberRequest request) {
        return sendRequest(request);
    }
    // ==================== 通用消息发送 ====================

    /**
     * 发送一条消息（要求已建立持久连接）
     */
    public boolean sendMessage(Object data) {
        if (!connected || out == null) {
            System.err.println("[SOCKET] Not connected, cannot send message");
            return false;
        }
        try {
            String json = gson.toJson(data);
            System.out.println("[SOCKET] Sending message: " + json);
            out.println(json);
            return true;
        } catch (Exception e) {
            System.err.println("[SOCKET] Send failed: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * 从服务器读取一行消息（非阻塞尝试）
     */
    public String receiveMessage() {
        try {
            if (in != null && in.ready()) {
                String message = in.readLine();
                if (message != null) {
                    System.out.println("[SOCKET] Received message: " + message);
                }
                return message;
            }
        } catch (IOException e) {
            System.err.println("[SOCKET] Receive failed: " + e.getMessage());
            connected = false;
        }
        return null;
    }

    /**
     * 阻塞等待接收消息（带超时）
     */
    public String receiveMessageBlocking() {
        try {
            if (in != null) {
                String message = in.readLine();
                if (message != null) {
                    System.out.println("[SOCKET] Received message (blocking): " + message);
                }
                return message;
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[SOCKET] Receive timeout");
        } catch (IOException e) {
            System.err.println("[SOCKET] Receive failed: " + e.getMessage());
            connected = false;
        }
        return null;
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // 静默关闭
        } finally {
            out = null;
            in = null;
            socket = null;
            System.out.println("[SOCKET] Disconnected");
        }
    }

    /**
     * 获取服务器地址（常量）
     */
    public String getServerAddress() {
        return SERVER_ADDRESS;
    }

    /**
     * 获取服务器端口（常量）
     */
    public int getServerPort() {
        return SERVER_PORT;
    }
}