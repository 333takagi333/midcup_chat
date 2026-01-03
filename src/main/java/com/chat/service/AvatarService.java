package com.chat.service;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

/**
 * 头像服务类 - 处理头像相关的文件操作
 */
public class AvatarService {

    /**
     * 选择头像图片文件
     */
    public static File selectAvatarFile(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择头像图片");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png", "*.gif"));
        return fileChooser.showOpenDialog(ownerWindow);
    }

    /**
     * 从文件加载图片到ImageView
     */
    public static boolean loadImageFromFile(File file, ImageView imageView, int size) {
        if (file == null || imageView == null) return false;

        try {
            Image image = new Image(file.toURI().toString(), size, size, true, true);
            if (!image.isError()) {
                imageView.setImage(image);
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[AvatarService] 图片加载失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 将图片文件转换为Base64字符串
     */
    public static String imageFileToBase64(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] imageData = new byte[(int) file.length()];
            fis.read(imageData);
            return Base64.getEncoder().encodeToString(imageData);
        } catch (Exception e) {
            System.err.println("[AvatarService] Base64编码失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成头像文件名
     */
    public static String generateAvatarFileName(String userId) {
        return "avatar_" + userId + "_" + System.currentTimeMillis() + ".png";
    }

    /**
     * 验证图片文件大小
     */
    public static boolean validateImageFile(File file, long maxSizeMB) {
        if (file == null) return false;
        long maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.length() <= maxSizeBytes;
    }
}