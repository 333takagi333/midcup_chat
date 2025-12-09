package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.FriendDetailResponse;
import com.chat.service.FriendProfileService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 好友资料控制器 - 只处理UI显示和事件绑定
 */
public class FriendProfileControl implements Initializable {

    @FXML private ImageView avatarImage;
    @FXML private Label usernameLabel, userIdLabel, genderLabel, birthdayLabel, phoneLabel;
    @FXML private VBox mainContainer;
    @FXML private Button deleteFriendButton;

    // 业务服务
    private FriendProfileService friendProfileService;

    // 数据
    private Long friendId;
    private Long currentUserId;
    private String friendName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AvatarHelper.setDefaultAvatar(avatarImage, false, 100);

        // 设置按钮事件
        if (deleteFriendButton != null) {
            deleteFriendButton.setOnAction(event -> handleDeleteFriend());
            deleteFriendButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
        }
    }

    /**
     * 设置好友信息（UI初始化）
     */
    public void setFriendInfo(String friendId, String friendName, String avatarUrl,
                              SocketClient socketClient, String currentUserId) {
        try {
            this.friendId = Long.parseLong(friendId);
            this.currentUserId = Long.parseLong(currentUserId);
            this.friendName = friendName;

            // 初始化服务
            this.friendProfileService = new FriendProfileService(socketClient);

            // 设置初始UI
            Platform.runLater(() -> {
                usernameLabel.setText(friendName != null ? friendName : "未知好友");
                userIdLabel.setText("ID: " + friendId);

                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    AvatarHelper.loadAvatar(avatarImage, avatarUrl, false, 100);
                }
            });

            // 加载详细信息
            loadFriendDetails();

        } catch (NumberFormatException e) {
            System.err.println("[FriendProfileControl] ID格式错误: " + e.getMessage());
            Platform.runLater(() -> DialogUtil.showError(getCurrentWindow(), "ID格式错误"));
        }
    }

    /**
     * 加载好友详细信息（调用服务）
     */
    private void loadFriendDetails() {
        friendProfileService.loadAndDisplayFriendInfo(currentUserId, friendId,
                new FriendProfileService.FriendProfileUICallback() {
                    @Override
                    public void onSuccess(FriendDetailResponse response) {
                        Platform.runLater(() -> updateFriendProfileUI(response));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Platform.runLater(() -> {
                            setDefaultInfo();
                            DialogUtil.showWarning(getCurrentWindow(), errorMessage);
                        });
                    }
                });
    }

    /**
     * 更新好友资料UI
     */
    private void updateFriendProfileUI(FriendDetailResponse response) {
        if (response.getUsername() != null) {
            usernameLabel.setText(response.getUsername());
        }

        if (response.getFriendId() != null) {
            userIdLabel.setText("ID: " + response.getFriendId());
        }

        if (response.getGender() != null) {
            String genderText = friendProfileService.getGenderText(response.getGender());
            genderLabel.setText("性别: " + genderText);
        }

        if (response.getBirthday() != null) {
            String birthdayText = friendProfileService.formatBirthday(response.getBirthday());
            birthdayLabel.setText("生日: " + birthdayText);
        }

        if (response.getTele() != null) {
            String phoneText = friendProfileService.formatPhone(response.getTele());
            phoneLabel.setText("电话: " + phoneText);
        }

        if (response.getAvatarUrl() != null && !response.getAvatarUrl().isEmpty()) {
            AvatarHelper.loadAvatar(avatarImage, response.getAvatarUrl(), false, 100);
        }
    }

    /**
     * 设置默认信息
     */
    private void setDefaultInfo() {
        genderLabel.setText("性别: 未知");
        birthdayLabel.setText("生日: 未设置");
        phoneLabel.setText("电话: 未设置");
    }

    /**
     * 处理删除好友（调用服务处理）
     */
    private void handleDeleteFriend() {
        if (friendId == null || currentUserId == null || friendProfileService == null) {
            DialogUtil.showError(getCurrentWindow(), "无法删除好友：信息不完整");
            return;
        }

        friendProfileService.deleteFriend(currentUserId, friendId, friendName, getCurrentWindow(),
                () -> closeWindow());
    }

    /**
     * 关闭窗口
     */
    private void closeWindow() {
        javafx.stage.Window window = getCurrentWindow();
        if (window instanceof javafx.stage.Stage) {
            ((javafx.stage.Stage) window).close();
        }
    }

    /**
     * 获取当前窗口
     */
    private javafx.stage.Window getCurrentWindow() {
        return mainContainer.getScene().getWindow();
    }
}