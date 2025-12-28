package com.chat.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * 文件服务类
 */
public class FileService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public interface UploadCallback {
        void onFileSelected(File file);
    }

    /**
     * 选择并上传文件
     */
    public static void chooseAndUploadFile(Window window, UploadCallback callback) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要上传的文件");

        // 设置文件过滤器
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("所有文件", "*.*"),
                new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                new FileChooser.ExtensionFilter("文档", "*.txt", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.ppt", "*.pptx"),
                new FileChooser.ExtensionFilter("视频", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.mkv", "*.flv"),
                new FileChooser.ExtensionFilter("音频", "*.mp3", "*.wav", "*.flac", "*.aac"),
                new FileChooser.ExtensionFilter("压缩文件", "*.zip", "*.rar", "*.7z", "*.tar", "*.gz")
        );

        // 设置上次使用的目录
        Preferences prefs = Preferences.userNodeForPackage(FileService.class);
        String lastDir = prefs.get("lastFileDirectory", System.getProperty("user.home"));
        File lastDirectory = new File(lastDir);
        if (lastDirectory.exists() && lastDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(lastDirectory);
        }

        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            // 保存目录
            prefs.put("lastFileDirectory", selectedFile.getParent());

            // 检查文件大小
            if (selectedFile.length() > MAX_FILE_SIZE) {
                showError(window, "文件太大",
                        String.format("文件大小不能超过 %d MB", MAX_FILE_SIZE / (1024 * 1024)));
                return;
            }

            callback.onFileSelected(selectedFile);
        }
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取文件类型分类
     */
    public static String getFileTypeCategory(File file) {
        String extension = getFileExtension(file);
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return "image";
            case "mp4":
            case "avi":
            case "mov":
            case "wmv":
            case "mkv":
            case "flv":
                return "video";
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
                return "audio";
            case "pdf":
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
                return "document";
            case "txt":
                return "text";
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "archive";
            default:
                return "other";
        }
    }

    /**
     * 获取文件类型描述
     */
    public static String getFileTypeDescription(String fileType) {
        switch (fileType) {
            case "image": return "图片";
            case "video": return "视频";
            case "audio": return "音频";
            case "document": return "文档";
            case "text": return "文本";
            case "archive": return "压缩包";
            default: return "文件";
        }
    }

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 显示错误对话框
     */
    private static void showError(Window window, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(window);
            alert.show();
        });
    }

    /**
     * 获取推荐的文件名（避免特殊字符）
     */
    public static String getSafeFileName(String originalName) {
        // 替换特殊字符
        return originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 检查文件名是否合法
     */
    public static boolean isValidFileName(String fileName) {
        return fileName != null && !fileName.isEmpty() &&
                !fileName.contains("\\") && !fileName.contains("/") &&
                !fileName.contains(":") && !fileName.contains("*") &&
                !fileName.contains("?") && !fileName.contains("\"") &&
                !fileName.contains("<") && !fileName.contains(">") &&
                !fileName.contains("|");
    }
    public static File chooseAndUploadFileSimplified(Window window) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("所有文件", "*.*"),
                new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("文档", "*.txt", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx"),
                new FileChooser.ExtensionFilter("视频", "*.mp4", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("音频", "*.mp3", "*.wav"),
                new FileChooser.ExtensionFilter("压缩包", "*.zip", "*.rar", "*.7z")
        );

        // 设置上次使用的目录
        Preferences prefs = Preferences.userNodeForPackage(FileService.class);
        String lastDir = prefs.get("lastFileDirectory", System.getProperty("user.home"));
        File lastDirectory = new File(lastDir);
        if (lastDirectory.exists() && lastDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(lastDirectory);
        }

        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            // 保存目录
            prefs.put("lastFileDirectory", selectedFile.getParent());
        }

        return selectedFile;
    }
}