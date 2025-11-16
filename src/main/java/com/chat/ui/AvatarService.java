package com.chat.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * 头像加载与占位图生成服务，负责从 URL 加载头像或生成默认头像。
 * 后续如需接入 HTTP 下载，只需在此类中扩展逻辑。
 */
public class AvatarService {

    /**
     * 根据 URL 加载头像，如果为空则使用默认占位图。
     */
    public void loadAvatar(String avatarUrl, ImageView imageView, boolean isGroup) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            loadRemoteAvatar(avatarUrl, imageView, isGroup);
        } else {
            setDefaultAvatar(imageView, isGroup);
        }
    }

    /**
     * 实际的远程头像加载逻辑，目前使用默认头像占位，后续可扩展 HTTP 请求。
     */
    private void loadRemoteAvatar(String avatarUrl, ImageView imageView, boolean isGroup) {
        // TODO: 使用 HTTP 客户端从服务器下载图片并设置到 imageView
        setDefaultAvatar(imageView, isGroup);
    }

    /**
     * 设置默认头像（占位符）。
     */
    public void setDefaultAvatar(ImageView imageView, boolean isGroup) {
        imageView.setImage(createDefaultAvatar(isGroup));
        imageView.setVisible(true);
    }

    /**
     * 创建默认头像（程序生成的圆形色块）。
     */
    public Image createDefaultAvatar(boolean isGroup) {
        int width = isGroup ? 50 : 40;
        int height = isGroup ? 50 : 40;
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        Color baseColor = isGroup ? Color.BLUE : Color.GREEN;

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

