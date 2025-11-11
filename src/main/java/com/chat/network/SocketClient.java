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
     * 发送登录请求到服务器，并读取首行响应。
     * @param data 请求数据对象
     * @return 首行响应字符串，失败或超时返回 null
     */
    public String sendLoginRequest(Object data) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            socket.setSoTimeout(TIMEOUT_MS);

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // 发送请求
            String json = new Gson().toJson(data);
            out.println(json);

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
     * 发送一条消息（要求已建立连接）。
     * @param data 数据对象
     * @return true 表示发送成功；否则返回 false
     */
    public boolean sendMessage(Object data) {
        if (!connected || out == null) {
            return false;
        }
        try {
            String json = new Gson().toJson(data);
            // 调试输出：查看实际发送的 JSON
            System.out.println("[SOCKET] Sending: " + json);
            out.println(json);
            return true;
        } catch (Exception e) {
            System.out.println("[SOCKET] Send failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从服务器读取一行消息（非阻塞尝试）。
     * @return 有可读数据时返回读取到的一行；否则返回 null
     */
    public String receiveMessage() {
        try {
            if (in != null && in.ready()) {
                return in.readLine();
            }
        } catch (IOException e) {
            // 静默失败，返回 null
        }
        return null;
    }

    /**
     * 检查当前是否仍然连接到服务器。
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * 主动断开与服务器的连接并释放资源。
     */
    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // 静默关闭
        }
    }

    /**
     * 获取服务器地址（常量）。
     */
    public String getServerAddress() {
        return SERVER_ADDRESS;
    }

    /**
     * 获取服务器端口（常量）。
     */
    public int getServerPort() {
        return SERVER_PORT;
    }
}