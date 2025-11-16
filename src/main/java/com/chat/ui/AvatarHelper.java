package com.chat.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * AvatarHelper - 头像处理工具
 * - 将头像相关的默认图和加载逻辑集中在此，便于复用与测试
 * - 目前远程加载为占位实现（后续可扩展为 HTTP 下载并缓存）
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
            // placeholder for remote loading
            loadRemoteAvatar(avatarUrl, imageView, isGroup, size);
        } else {
            setDefaultAvatar(imageView, isGroup, size);
        }
    }

    public static void loadRemoteAvatar(String avatarUrl, ImageView imageView, boolean isGroup, int size) {
        // TODO: 实现真正的远程下载 + 缓存。当前占位实现使用默认头像。
        System.out.println("[AvatarHelper] (placeholder) load remote avatar: " + avatarUrl);
        setDefaultAvatar(imageView, isGroup, size);
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

