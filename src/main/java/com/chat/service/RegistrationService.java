package com.chat.service;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.chat.control.PasswordEncryptor;

/**
 * 注册、登录、密码管理业务逻辑服务
 */
public class RegistrationService {

    private final Gson gson = new Gson();
    public LoginResult loginWithConnection(Long uid, String password) {
        SocketClient client = null;
        try {
            // 1. 创建 SocketClient
            client = new SocketClient();

            // 2. 显式连接
            if (!client.connect()) {
                System.err.println("[RegistrationService] 连接服务器失败");
                return null;
            }

            System.out.println("[RegistrationService] 连接服务器成功，开始登录...");

            // 3. 加密密码
            String encrypted = PasswordEncryptor.encrypt(password);

            // 4. 创建请求
            LoginRequest request = new LoginRequest(uid, encrypted);

            // 5. 发送请求
            String response = client.sendLoginRequest(request);

            if (response == null) {
                System.err.println("[RegistrationService] 服务器无响应");
                return null;
            }

            System.out.println("[RegistrationService] 收到登录响应: " + response);

            // 6. 解析响应
            LoginResponse loginResponse = gson.fromJson(response, LoginResponse.class);

            if (loginResponse != null && loginResponse.isSuccess()) {
                // 登录成功，返回连接和响应
                return new LoginResult(loginResponse, client);
            } else {
                // 登录失败，断开连接
                client.disconnect();
                return new LoginResult(loginResponse, null);
            }

        } catch (Exception e) {
            System.err.println("[RegistrationService] 登录请求异常: " + e.getMessage());
            e.printStackTrace();
            if (client != null) {
                client.disconnect();
            }
            return null;
        }
    }

    /**
     * 登录结果包装类
     */
    public static class LoginResult {
        private LoginResponse response;
        private SocketClient socketClient;

        public LoginResult(LoginResponse response, SocketClient socketClient) {
            this.response = response;
            this.socketClient = socketClient;
        }

        public LoginResponse getResponse() { return response; }
        public SocketClient getSocketClient() { return socketClient; }
    }
    /**
     * 注册用户
     */
    public RegisterResponse register(SocketClient client, String username, String password) {
        try {
            String encryptedPassword = PasswordEncryptor.encrypt(password);
            RegisterRequest request = new RegisterRequest(username, encryptedPassword);
            String response = client.sendRegisterRequest(request);

            if (response != null) {
                RegisterResponse registerResponse = gson.fromJson(response, RegisterResponse.class);
                if (registerResponse != null && MessageType.REGISTER_RESPONSE.equals(registerResponse.getType())) {
                    return registerResponse;
                }
            }
        } catch (Exception e) {
            System.err.println("注册请求异常: " + e.getMessage());
        }
        return null;
    }

    /**
     * 用户登录
     */
    public LoginResponse login(SocketClient client, Long uid, String password) {
        try {
            String encrypted = PasswordEncryptor.encrypt(password);
            LoginRequest request = new LoginRequest(uid, encrypted);
            String response = client.sendLoginRequest(request);

            if (response != null) {
                return gson.fromJson(response, LoginResponse.class);
            }
        } catch (Exception e) {
            System.err.println("登录请求异常: " + e.getMessage());
        }
        return null;
    }

    /**
     * 重置密码
     */
    public ResetPasswordResponse resetPassword(SocketClient client, String key, String newPassword) {
        try {
            String encryptedPassword = PasswordEncryptor.encrypt(newPassword);
            ResetPasswordRequest request = new ResetPasswordRequest(key, encryptedPassword);
            String response = client.sendResetPasswordRequest(request);

            if (response != null) {
                ResetPasswordResponse resetResponse = gson.fromJson(response, ResetPasswordResponse.class);
                if (resetResponse != null && MessageType.RESET_PASSWORD_RESPONSE.equals(resetResponse.getType())) {
                    return resetResponse;
                }
            }
        } catch (Exception e) {
            System.err.println("重置密码请求异常: " + e.getMessage());
        }
        return null;
    }

    /**
     * 修改密码
     */
    public boolean changePassword(SocketClient client, String userId, String oldPassword, String newPassword) {
        try {
            ChangePasswordRequest request = new ChangePasswordRequest(userId, oldPassword, newPassword);
            String response = client.sendRequest(request);

            if (response != null) {
                JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                return responseObj.has("success") && responseObj.get("success").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("修改密码请求异常: " + e.getMessage());
        }
        return false;
    }

    /**
     * 验证注册输入
     */
    public static String validateRegistrationInput(String username, String password, String confirmPassword) {
        if (username.isEmpty()) {
            return "请输入用户名";
        }
        if (password.isEmpty()) {
            return "请输入密码";
        }
        if (confirmPassword.isEmpty()) {
            return "请确认密码";
        }
        if (!password.equals(confirmPassword)) {
            return "两次输入的密码不一致";
        }
        return null;
    }

    /**
     * 验证重置密码输入
     */
    public static String validateResetPasswordInput(String key, String newPassword, String confirmPassword) {
        if (key.isEmpty()) {
            return "请输入安全密钥";
        }
        if (newPassword.isEmpty()) {
            return "请输入新密码";
        }
        if (confirmPassword.isEmpty()) {
            return "请确认新密码";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "两次输入的密码不一致";
        }
        return null;
    }

    /**
     * 验证登录输入
     */
    public static String validateLoginInput(String uidText, String password) {
        if (uidText.isEmpty()) {
            return "请输入用户ID";
        }
        if (password.isEmpty()) {
            return "请输入密码";
        }
        try {
            Long.parseLong(uidText);
        } catch (NumberFormatException e) {
            return "用户ID必须是数字";
        }
        return null;
    }
}