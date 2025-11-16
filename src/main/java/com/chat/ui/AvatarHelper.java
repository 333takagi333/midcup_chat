package com.chat.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import java.util.Objects;

/**
 * AvatarHelper - 头像处理工具
 */
public final class AvatarHelper {

    private AvatarHelper() {}

    public static void loadAvatar(ImageView imageView, String avatarUrl, boolean isGroup) {
        int size = isGroup ? 50 : 40;
        loadAvatar(imageView, avatarUrl, isGroup, size);
    }

    public static void loadAvatar(ImageView imageView, String avatarUrl, boolean isGroup, int size) {
        if (imageView == null) return;
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            // 加载本地资源图片
            loadLocalAvatar(avatarUrl, imageView, isGroup, size);
        } else {
            setDefaultAvatar(imageView, isGroup, size);
        }
    }

    /**
     * 加载本地资源图片
     */
    public static void loadLocalAvatar(String avatarFileName, ImageView imageView, boolean isGroup, int size) {
        try {
            // 构建资源路径
            String resourcePath = "/com/chat/images/" + avatarFileName;
            System.out.println("[AvatarHelper] 加载本地头像: " + resourcePath);

            // 从资源文件夹加载图片
            Image image = new Image(Objects.requireNonNull(
                    AvatarHelper.class.getResourceAsStream(resourcePath)
            ), size, size, true, true);

            imageView.setImage(image);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);

        } catch (Exception e) {
            System.err.println("[AvatarHelper] 加载本地头像失败: " + avatarFileName + ", 错误: " + e.getMessage());
            // 回退到默认头像
            setDefaultAvatar(imageView, isGroup, size);
        }
    }

    /**
     * 加载远程头像（占位实现）
     */
    public static void loadRemoteAvatar(String avatarUrl, ImageView imageView, boolean isGroup, int size) {
        // 如果是简单的文件名，尝试作为本地资源加载
        if (avatarUrl != null && !avatarUrl.contains("/") && !avatarUrl.contains("http")) {
            loadLocalAvatar(avatarUrl, imageView, isGroup, size);
        } else {
            System.out.println("[AvatarHelper] (placeholder) load remote avatar: " + avatarUrl);
            setDefaultAvatar(imageView, isGroup, size);
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
            imageView.setImage(img);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setVisible(true);
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
}