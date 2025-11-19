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
    @FXML private TextField usernameField; // 新增：用户名输入框
    @FXML private ComboBox<String> genderComboBox;
    @FXML private TextField birthdayField, phoneField;
    @FXML private Button saveButton, editButton, cancelButton, changeAvatarButton;

    // 业务数据
    private String username, userId;
    private SocketClient socketClient;
    private UserProfileService profileService;
    private boolean isEditing = false;
    private File selectedAvatarFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AvatarHelper.setDefaultAvatar(avatarImage, false, 100);
        genderComboBox.getItems().addAll("未知", "男", "女");
        setEditMode(false);
        setupAvatarClickEvent();
    }

    /**
     * 设置头像点击事件
     */
    private void setupAvatarClickEvent() {
        avatarImage.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && isEditing) {
                selectAvatarImage();
            }
        });
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
            userIdLabel.setText(userId != null ? "ID: " + userId : "未知");
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
            userIdLabel.setText(response.getUid().toString());
            this.userId = String.valueOf(response.getUid());
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
        changeAvatarButton.setVisible(editing);

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

        // 构建更新请求
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername(usernameField.getText().trim()); // 新增：用户名
        request.setGender(ProfileValidationService.getGenderCode(genderComboBox.getValue()));
        request.setBirthday(birthdayField.getText().trim());
        request.setTele(phoneField.getText().trim());

        // 处理头像更新
        if (selectedAvatarFile != null) {
            String avatarData = AvatarService.imageFileToBase64(selectedAvatarFile);
            if (avatarData != null) {
                request.setAvatarData(avatarData);
                request.setAvatarFileName(AvatarService.generateAvatarFileName(userId));
            } else {
                DialogUtil.showError(getCurrentWindow(), "头像文件处理失败");
                return;
            }
        }

        // 提交到服务器
        if (profileService != null) {
            boolean success = profileService.updateUserProfile(request);
            if (success) {
                updateLocalDisplay();
                setEditMode(false);
                DialogUtil.showInfo(getCurrentWindow(), "资料更新成功");
                loadUserInfoFromServer();
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
        selectedAvatarFile = null;
    }

    /**
     * 编辑头像 - 选择新头像图片
     */
    @FXML
    private void editAvatar() {
        selectAvatarImage();
    }

    /**
     * 选择头像图片 - 打开文件选择器并加载图片
     */
    private void selectAvatarImage() {
        if (!isEditing) {
            DialogUtil.showInfo(getCurrentWindow(), "请先进入编辑模式");
            return;
        }

        File selectedFile = AvatarService.selectAvatarFile(getCurrentWindow());
        if (selectedFile != null) {
            if (!AvatarService.validateImageFile(selectedFile, 2)) {
                DialogUtil.showError(getCurrentWindow(), "图片文件太大");
                return;
            }

            if (AvatarService.loadImageFromFile(selectedFile, avatarImage, 100)) {
                selectedAvatarFile = selectedFile;
                DialogUtil.showInfo(getCurrentWindow(), "头像已更新");
            } else {
                DialogUtil.showError(getCurrentWindow(), "图片加载失败");
            }
        }
    }

    /**
     * 获取当前窗口
     */
    private javafx.stage.Window getCurrentWindow() {
        return avatarImage.getScene().getWindow();
    }
}