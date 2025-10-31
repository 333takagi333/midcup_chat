package com.chat.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 简易本地登录测试服务器：监听 12345 端口，接收一行 JSON 并回显一个响应。
 * 仅用于本地调试。可在 IDE 中以普通 Java 应用运行。
 */
public class MockLoginServer {
    public static void main(String[] args) throws IOException {
        int port = 12345;
        System.out.println("MockLoginServer listening on port " + port + " ...");
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                try (Socket client = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {

                    String line = in.readLine();
                    System.out.println("Client says: " + line);

                    String response = "{\"status\":\"OK\",\"msg\":\"login received\",\"echo\":" + (line == null ? "null" : ("\"" + line.replace("\"", "\\\"") + "\"")) + "}";
                    out.println(response);
                } catch (Exception e) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }
        }
    }
}

