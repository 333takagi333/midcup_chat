package com.chat.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * AvatarHelper - 头像处理工具
 */
public final class AvatarHelper {

    // 服务器基础URL配置
    private static final String SERVER_BASE_URL = "http://112.124.108.184:12355/";

    private AvatarHelper() {}

    public static void loadAvatar(ImageView imageView, String avatarUrl, boolean isGroup) {
        int size = isGroup ? 50 : 40;
        loadAvatar(imageView, avatarUrl, isGroup, size);
    }

    public static void loadAvatar(ImageView imageView, String avatarUrl, boolean isGroup, int size) {
        if (imageView == null) return;

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            loadRemoteAvatar(avatarUrl, imageView, isGroup, size);
        } else {
            setDefaultAvatar(imageView, isGroup, size);
        }
    }

    /**
     * 构建完整的头像URL
     */
    private static String buildFullAvatarUrl(String avatarPath) {
        if (avatarPath == null || avatarPath.trim().isEmpty()) {
            return null;
        }

        // 如果已经是完整的URL，直接返回
        if (avatarPath.startsWith("http://") || avatarPath.startsWith("https://")) {
            return avatarPath;
        }

        // 如果路径以斜杠开头，去掉斜杠
        if (avatarPath.startsWith("/")) {
            avatarPath = avatarPath.substring(1);
        }

        // 拼接完整的URL
        return SERVER_BASE_URL + avatarPath;
    }

    /**
     * 加载远程头像 - 修复版本
     */
    public static void loadRemoteAvatar(String avatarUrl, ImageView imageView, boolean isGroup, int size) {
        try {
            // 构建完整的头像URL
            String fullAvatarUrl = buildFullAvatarUrl(avatarUrl);
            System.out.println("[AvatarHelper] 开始加载远程头像: " + fullAvatarUrl);

            // 创建Image对象，启用背景加载
            Image image = new Image(fullAvatarUrl, size, size, true, true, true);

            // 先设置默认头像，避免显示空白
            setDefaultAvatar(imageView, isGroup, size);

            // 使用单一监听器来处理加载完成和错误
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                double progress = newValue.doubleValue();

                if (progress == 1.0) {
                    // 加载完成
                    if (!image.isError()) {
                        System.out.println("[AvatarHelper] 远程头像加载成功: " + fullAvatarUrl);
                        // 在主线程中更新UI
                        javafx.application.Platform.runLater(() -> {
                            setImageToView(imageView, image, size);
                        });
                    } else {
                        System.err.println("[AvatarHelper] 远程头像加载失败（进度完成但有错误）: " + fullAvatarUrl);
                        // 保持默认头像
                    }
                }
            });

            // 立即检查状态（对于缓存的图片可能已经加载完成）
            if (image.getProgress() == 1.0 && !image.isError()) {
                System.out.println("[AvatarHelper] 使用缓存的远程头像: " + fullAvatarUrl);
                setImageToView(imageView, image, size);
            }

        } catch (Exception e) {
            System.err.println("[AvatarHelper] 加载远程头像异常: " + avatarUrl + ", 错误: " + e.getMessage());
            setDefaultAvatar(imageView, isGroup, size);
        }
    }

    /**
     * 设置图片到ImageView
     */
    private static void setImageToView(ImageView imageView, Image image, int size) {
        if (image != null && !image.isError()) {
            imageView.setImage(image);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);
            imageView.setVisible(true);
        }
    }

    public static void setDefaultAvatar(ImageView imageView, boolean isGroup) {
        int size = isGroup ? 50 : 40;
        setDefaultAvatar(imageView, isGroup, size);
    }

    public static void setDefaultAvatar(ImageView imageView, boolean isGroup, int size) {
        if (imageView == null) return;
        Image img = createDefaultAvatar(isGroup, size);
        if (img != null) {
            setImageToView(imageView, img, size);
        }
    }

    public static Image createDefaultAvatar(boolean isGroup, int size) {
        if (size <= 0) size = isGroup ? 50 : 40;
        WritableImage image = new WritableImage(size, size);
        PixelWriter pixelWriter = image.getPixelWriter();

        Color baseColor = isGroup ? Color.BLUE : Color.GREEN;
        int width = size;
        int height = size;

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
     * 测试方法：直接验证URL是否可访问
     */
    public static void testAvatarUrl(String avatarPath) {
        String fullUrl = buildFullAvatarUrl(avatarPath);
        System.out.println("[AvatarHelper] 测试头像URL: " + fullUrl);

        // 可以在这里添加网络连通性测试
        try {
            java.net.URL url = new java.net.URL(fullUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            System.out.println("[AvatarHelper] HTTP响应码: " + responseCode);
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("[AvatarHelper] 测试URL失败: " + e.getMessage());
        }
    }
}