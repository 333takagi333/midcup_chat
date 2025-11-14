package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.UserInfoRequest;
import com.chat.protocol.UserInfoResponse;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 用户资料控制器
 */
public class UserProfileControl implements Initializable {

    @FXML private ImageView avatarImage;
    @FXML private Label usernameLabel;
    @FXML private Label userIdLabel;
    @FXML private Label statusLabel;
    @FXML private Label signatureLabel;
    @FXML private Label genderLabel;
    @FXML private Label birthdayLabel;
    @FXML private Label phoneLabel;

    private String username;
    private String userId;
    private SocketClient socketClient;
    private Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化时可以设置一些默认值
        setDefaultAvatar();
    }

    /**
     * 设置用户信息
     */
    public void setUserInfo(String username, String userId, SocketClient socketClient) {
        this.username = username;
        this.userId = userId;
        this.socketClient = socketClient;

        updateUI();
        loadUserInfoFromServer();
    }

    /**
     * 更新UI显示
     */
    private void updateUI() {
        usernameLabel.setText(username != null ? username : "未知用户");
        userIdLabel.setText(userId != null ? "ID: " + userId : "ID: 未知");
        statusLabel.setText("在线");
        signatureLabel.setText("这个人很懒，什么都没有写~");
        genderLabel.setText("未知");
        birthdayLabel.setText("未设置");
        phoneLabel.setText("未设置");

        // 使用默认头像，不再尝试加载本地文件
        setDefaultAvatar();
    }

    /**
     * 设置默认头像
     */
    private void setDefaultAvatar() {
        avatarImage.setImage(createDefaultAvatar());
        avatarImage.setFitWidth(100);
        avatarImage.setFitHeight(100);
    }

    /**
     * 创建默认头像（程序生成）
     */
    private Image createDefaultAvatar() {
        int width = 100;
        int height = 100;
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        // 根据用户名生成不同的颜色
        Color baseColor = generateColorFromUsername();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double dx = x - width / 2.0;
                double dy = y - height / 2.0;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= width / 2.0) {
                    pixelWriter.setColor(x, y, baseColor);
                } else {
                    pixelWriter.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        return image;
    }

    /**
     * 根据用户名生成颜色
     */
    private Color generateColorFromUsername() {
        if (username == null || username.isEmpty()) {
            return Color.BLUE;
        }

        // 使用用户名哈希值生成颜色
        int hash = username.hashCode();
        double hue = (hash & 0xFF) / 255.0 * 360;
        double saturation = 0.7 + ((hash >> 8) & 0xFF) / 255.0 * 0.3;
        double brightness = 0.6 + ((hash >> 16) & 0xFF) / 255.0 * 0.4;

        return Color.hsb(hue, saturation, brightness);
    }

    /**
     * 从服务端加载用户信息
     */
    private void loadUserInfoFromServer() {
        if (socketClient != null && socketClient.isConnected() && userId != null) {
            try {
                // 发送用户信息请求
                UserInfoRequest request = new UserInfoRequest(Long.parseLong(userId));
                String response = socketClient.sendUserInfoRequest(request);

                if (response != null) {
                    // 处理服务端响应
                    handleUserInfoResponseJson(response);
                } else {
                    System.out.println("[UserProfile] 用户信息请求超时，使用本地数据");
                    // 请求失败时使用本地数据，不显示错误
                }
            } catch (NumberFormatException e) {
                System.err.println("[UserProfile] 用户ID格式错误: " + userId);
            } catch (Exception e) {
                System.err.println("[UserProfile] 加载用户信息失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理服务端返回的用户信息（JSON字符串）
     */
    private void handleUserInfoResponseJson(String responseJson) {
        try {
            UserInfoResponse response = gson.fromJson(responseJson, UserInfoResponse.class);
            handleUserInfoResponse(response);
        } catch (Exception e) {
            System.err.println("[UserProfile] 解析用户信息响应失败: " + e.getMessage());
        }
    }

    /**
     * 处理服务端返回的用户信息
     */
    public void handleUserInfoResponse(UserInfoResponse response) {
        if (response != null && response.isSuccess()) {
            // 更新UI显示服务端返回的数据
            Platform.runLater(() -> {
                // 更新用户名
                if (response.getUsername() != null) {
                    usernameLabel.setText(response.getUsername());
                }

                // 更新用户ID
                if (response.getUid() != null) {
                    userIdLabel.setText("ID: " + response.getUid());
                }

                // 更新性别
                if (response.getGender() != null) {
                    String genderText = getGenderText(response.getGender());
                    genderLabel.setText(genderText);
                }

                // 更新生日
                if (response.getBirthday() != null && !response.getBirthday().isEmpty()) {
                    birthdayLabel.setText(response.getBirthday());
                }

                // 更新电话
                if (response.getTele() != null && !response.getTele().isEmpty()) {
                    phoneLabel.setText(response.getTele());
                }

                // 更新头像
                if (response.getAvatarUrl() != null && !response.getAvatarUrl().isEmpty()) {
                    loadAvatarFromServer(response.getAvatarUrl());
                }
            });
        }
    }

    /**
     * 将性别代码转换为文本
     */
    private String getGenderText(Integer genderCode) {
        if (genderCode == null) {
            return "未知";
        }
        switch (genderCode) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 从服务端加载头像
     */
    private void loadAvatarFromServer(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            // 这里应该实现从服务端下载头像的逻辑
            // 暂时使用默认头像，实际项目中需要实现HTTP请求下载
            System.out.println("[UserProfile] 需要加载远程头像: " + avatarUrl);
            // setDefaultAvatar(); // 保持默认头像
        }
    }

    /**
     * 保存用户资料（如果需要编辑功能）
     */
    @FXML
    private void saveProfile() {
        // 实现保存逻辑
        if (avatarImage.getScene() != null && avatarImage.getScene().getWindow() != null) {
            DialogUtil.showInfo(avatarImage.getScene().getWindow(), "保存成功");
        }
    }

    /**
     * 编辑头像
     */
    @FXML
    private void editAvatar() {
        // 实现头像编辑逻辑
        if (avatarImage.getScene() != null && avatarImage.getScene().getWindow() != null) {
            DialogUtil.showInfo(avatarImage.getScene().getWindow(), "头像编辑功能开发中...");
        }
    }

    @FXML
    private void editProfile() {
        // 实现编辑资料逻辑
        if (avatarImage.getScene() != null && avatarImage.getScene().getWindow() != null) {
            DialogUtil.showInfo(avatarImage.getScene().getWindow(), "编辑资料功能开发中...");
        }
    }
}