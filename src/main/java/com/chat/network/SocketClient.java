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
 * 简单的基于 TCP 的客户端，用于向服务器发送登录请求并获取响应。
 */
public class SocketClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int TIMEOUT_MS = 5000; // 5 seconds

    /**
     * 发送登录请求并等待服务端响应。
     * @param jsonRequest JSON 字符串
     * @return 服务端返回的首行响应；如果超时或失败返回 null
     */
    public String sendLoginRequest(String jsonRequest) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            socket.setSoTimeout(TIMEOUT_MS);

            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                // 发送请求
                out.println(jsonRequest);

                // 读取响应（按行协议）
                return in.readLine();
            }
        } catch (SocketTimeoutException e) {
            // 超时返回 null，调用方统一处理
            return null;
        } catch (IOException e) {
            // 连接或通讯失败，返回 null，调用方统一处理
            return null;
        }
    }
}
