package com.chat.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

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

    /**
     * 发送登录请求
     * @param jsonRequest JSON 字符串
     * @return 服务端返回的首行响应；如果超时或失败返回 null
     */
    public String sendLoginRequest(String jsonRequest) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            socket.setSoTimeout(TIMEOUT_MS);

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // 发送请求
            out.println(jsonRequest);

            // 读取响应
            String response = in.readLine();
            connected = true;
            return response;

        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 发送消息（保持连接）
     * @param jsonMessage JSON格式的消息
     * @return 发送是否成功
     */
    public boolean sendMessage(String jsonMessage) {
        if (!connected || out == null) {
            System.err.println("未连接到服务器，无法发送消息");
            return false;
        }

        try {
            out.println(jsonMessage);
            return true;
        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 接收消息
     */
    public String receiveMessage() {
        try {
            if (in != null && in.ready()) {
                return in.readLine();
            }
        } catch (IOException e) {
            System.err.println("接收消息错误: " + e.getMessage());
        }
        return null;
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
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
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }

    public String getServerAddress() {
        return SERVER_ADDRESS;
    }

    public int getServerPort() {
        return SERVER_PORT;
    }
}