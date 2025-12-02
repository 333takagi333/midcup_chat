package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.UpdateProfileRequest;
import com.chat.protocol.UserInfoResponse;
import com.chat.service.AvatarService;
import com.chat.service.ProfileValidationService;
import com.chat.service.UserProfileService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 用户资料控制器 - 管理用户资料界面的显示和交互
 */
public class UserProfileControl implements Initializable {

    // FXML控件
    @FXML private ImageView avatarImage;
    @FXML private Label usernameLabel, userIdLabel, statusLabel, genderLabel, birthdayLabel, phoneLabel;
    @FXML private TextField usernameField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private TextField birthdayField, phoneField;
    @FXML private Button saveButton, editButton, cancelButton;

    // 业务数据
    private String username, userId;
    private SocketClient socketClient;
    private UserProfileService profileService;
    private boolean isEditing = false;
    private File selectedAvatarFile; // 新选择的头像文件

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AvatarHelper.setDefaultAvatar(avatarImage, false, 100);
        genderComboBox.getItems().addAll("未知", "男", "女");
        setEditMode(false);
    }

    /**
     * 设置用户信息
     */
    public void setUserInfo(String username, String userId, SocketClient socketClient) {
        this.username = username;
        this.userId = userId;
        this.socketClient = socketClient;
        this.profileService = new UserProfileService(socketClient);
        updateUI();
        loadUserInfoFromServer();
    }

    /**
     * 更新UI显示
     */
    private void updateUI() {
        Platform.runLater(() -> {
            usernameLabel.setText(username != null ? username : "未知用户");
            userIdLabel.setText(userId != null ? userId : "未知");
            statusLabel.setText("在线");
            genderLabel.setText("未知");
            birthdayLabel.setText("未设置");
            phoneLabel.setText("未设置");
            AvatarHelper.setDefaultAvatar(avatarImage, false, 100);
        });
    }

    /**
     * 从服务端加载用户信息
     */
    private void loadUserInfoFromServer() {
        if (profileService != null && userId != null) {
            try {
                UserInfoResponse response = profileService.loadUserInfo(Long.parseLong(userId));
                if (response != null && response.isSuccess()) {
                    Platform.runLater(() -> updateProfileUI(response));
                }
            } catch (NumberFormatException e) {
                System.err.println("[UserProfile] 用户ID格式错误: " + userId);
            }
        }
    }

    /**
     * 更新资料UI
     */
    private void updateProfileUI(UserInfoResponse response) {
        if (response.getUsername() != null) {
            usernameLabel.setText(response.getUsername());
            this.username = response.getUsername();
        }
        if (response.getUid() != null) {
            userIdLabel.setText(String.valueOf(response.getUid()));
            this.userId = response.getUid().toString();
        }
        if (response.getGender() != null) {
            String genderText = ProfileValidationService.getGenderText(response.getGender());
            genderLabel.setText(genderText);
            genderComboBox.setValue(genderText);
        }
        if (response.getBirthday() != null && !response.getBirthday().isEmpty()) {
            birthdayLabel.setText(response.getBirthday());
            birthdayField.setText(response.getBirthday());
        }
        if (response.getTele() != null && !response.getTele().isEmpty()) {
            phoneLabel.setText(response.getTele());
            phoneField.setText(response.getTele());
        }
        if (response.getAvatarUrl() != null && !response.getAvatarUrl().isEmpty()) {
            AvatarHelper.loadAvatar(avatarImage, response.getAvatarUrl(), false, 100);
        }
    }

    /**
     * 设置编辑模式
     */
    private void setEditMode(boolean editing) {
        this.isEditing = editing;

        // 切换标签和输入控件的显示状态
        usernameLabel.setVisible(!editing);
        genderLabel.setVisible(!editing);
        birthdayLabel.setVisible(!editing);
        phoneLabel.setVisible(!editing);

        usernameField.setVisible(editing);
        genderComboBox.setVisible(editing);
        birthdayField.setVisible(editing);
        phoneField.setVisible(editing);

        // 切换按钮的显示状态
        saveButton.setVisible(editing);
        cancelButton.setVisible(editing);
        editButton.setVisible(!editing);

        editButton.setText(editing ? "编辑中..." : "编辑资料");
    }

    /**
     * 编辑资料 - 进入编辑模式并填充当前数据
     */
    @FXML
    private void editProfile() {
        setEditMode(true);
        usernameField.setText(usernameLabel.getText());
        genderComboBox.setValue(genderLabel.getText());
        birthdayField.setText("未设置".equals(birthdayLabel.getText()) ? "" : birthdayLabel.getText());
        phoneField.setText("未设置".equals(phoneLabel.getText()) ? "" : phoneLabel.getText());
    }

    /**
     * 取消编辑 - 退出编辑模式并恢复原始数据
     */
    @FXML
    private void cancelEdit() {
        setEditMode(false);
        DialogUtil.showInfo(getCurrentWindow(), "已取消编辑");
    }

    /**
     * 保存资料 - 验证并提交所有修改到服务器
     */
    @FXML
    private void saveProfile() {
        if (!validateInput()) return;

        // 构建更新请求（不包含头像）
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername(usernameField.getText().trim());
        request.setGender(ProfileValidationService.getGenderCode(genderComboBox.getValue()));
        request.setBirthday(birthdayField.getText().trim());
        request.setTele(phoneField.getText().trim());

        // 提交到服务器
        if (profileService != null) {
            boolean success = profileService.updateUserProfile(request);
            if (success) {
                updateLocalDisplay();
                setEditMode(false);
                DialogUtil.showInfo(getCurrentWindow(), "资料更新成功");
                // 不重新加载用户信息，避免头像消失
            } else {
                DialogUtil.showError(getCurrentWindow(), "资料更新失败");
            }
        } else {
            DialogUtil.showError(getCurrentWindow(), "网络服务不可用");
        }
    }

    /**
     * 验证输入 - 检查用户名、电话号码和生日格式
     */
    private boolean validateInput() {
        // 验证用户名
        String newUsername = usernameField.getText().trim();
        if (newUsername.isEmpty()) {
            DialogUtil.showError(getCurrentWindow(), "用户名不能为空");
            return false;
        }
        if (newUsername.length() < 2 || newUsername.length() > 20) {
            DialogUtil.showError(getCurrentWindow(), "用户名长度应在2-20个字符之间");
            return false;
        }

        // 验证电话号码
        if (!ProfileValidationService.validatePhone(phoneField.getText().trim())) {
            DialogUtil.showError(getCurrentWindow(), "电话号码格式不正确");
            return false;
        }

        // 验证生日格式
        if (!ProfileValidationService.validateBirthday(birthdayField.getText().trim())) {
            DialogUtil.showError(getCurrentWindow(), "生日格式不正确");
            return false;
        }

        return true;
    }

    /**
     * 更新本地显示 - 在服务器响应前先更新UI
     */
    private void updateLocalDisplay() {
        usernameLabel.setText(usernameField.getText().trim());
        genderLabel.setText(genderComboBox.getValue());
        birthdayLabel.setText(birthdayField.getText().trim().isEmpty() ? "未设置" : birthdayField.getText().trim());
        phoneLabel.setText(phoneField.getText().trim().isEmpty() ? "未设置" : phoneField.getText().trim());
    }

    /**
     * 编辑头像 - 独立功能，不依赖编辑模式
     */
    @FXML
    private void editAvatar() {
        selectAvatarImage();
    }

    /**
     * 选择头像图片 - 独立功能，随时可以修改头像
     */
    private void selectAvatarImage() {
        File selectedFile = AvatarService.selectAvatarFile(getCurrentWindow());
        if (selectedFile != null) {
            // 验证文件大小
            if (!AvatarService.validateImageFile(selectedFile, 2)) {
                DialogUtil.showError(getCurrentWindow(), "图片文件太大，请选择小于2MB的图片");
                return;
            }

            // 加载并预览图片
            if (AvatarService.loadImageFromFile(selectedFile, avatarImage, 100)) {
                // 立即上传头像
                uploadAvatar(selectedFile);
            } else {
                DialogUtil.showError(getCurrentWindow(), "图片加载失败，请选择其他图片");
            }
        } else {
            // 用户取消选择，保持原头像不变
            System.out.println("[UserProfile] 用户取消选择头像");
            // 不需要做任何操作，头像保持原样
        }
    }

    /**
     * 上传头像 - 独立上传头像功能
     */
    private void uploadAvatar(File avatarFile) {
        try {
            String avatarData = AvatarService.imageFileToBase64(avatarFile);
            if (avatarData != null) {
                UpdateProfileRequest request = new UpdateProfileRequest();
                request.setAvatarData(avatarData);
                request.setAvatarFileName(AvatarService.generateAvatarFileName(userId));

                if (profileService != null) {
                    boolean success = profileService.updateUserProfile(request);
                    if (success) {
                        DialogUtil.showInfo(getCurrentWindow(), "头像更新成功");
                        loadUserInfoFromServer(); // 重新加载确保数据一致性
                    } else {
                        DialogUtil.showError(getCurrentWindow(), "头像更新失败");
                    }
                }
            } else {
                DialogUtil.showError(getCurrentWindow(), "头像文件处理失败");
            }
        } catch (Exception e) {
            System.err.println("[UserProfile] 头像上传失败: " + e.getMessage());
            DialogUtil.showError(getCurrentWindow(), "头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前窗口
     */
    private javafx.stage.Window getCurrentWindow() {
        return avatarImage.getScene().getWindow();
    }
}