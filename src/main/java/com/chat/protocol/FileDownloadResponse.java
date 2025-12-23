package com.chat.protocol;

/**
 * 文件下载响应（简化版）
 */
public class FileDownloadResponse {
    private String type = MessageType.FILE_DOWNLOAD_RESPONSE;
    private boolean success;
    private String message;
    private String fileId;
    private String fileName;
    private String downloadUrl;

    // 构造方法
    public FileDownloadResponse() {
    }

    public FileDownloadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public FileDownloadResponse(boolean success, String message, String fileId, String fileName, String downloadUrl) {
        this.success = success;
        this.message = message;
        this.fileId = fileId;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
    }

    // 成功响应的静态工厂方法
    public static FileDownloadResponse success(String fileId, String fileName, String downloadUrl) {
        return new FileDownloadResponse(true, "文件下载链接生成成功", fileId, fileName, downloadUrl);
    }

    // 失败响应的静态工厂方法
    public static FileDownloadResponse error(String message) {
        return new FileDownloadResponse(false, message, null, null, null);
    }

    // getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public String toString() {
        return "FileDownloadResponse{" +
                "type='" + type + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", downloadUrl='" + (downloadUrl != null ? "***" : "null") + '\'' +
                '}';
    }
}