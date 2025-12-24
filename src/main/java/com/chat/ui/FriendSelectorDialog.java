package com.chat.ui;

import com.chat.service.GroupDetailsService;
import com.chat.ui.AvatarHelper;
import com.chat.ui.DialogUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Map;

/**
 * 好友选择对话框
 */
public class FriendSelectorDialog {

    private final Window owner;
    private final GroupDetailsService service;
    private final GroupDetailsService.FriendSelectCallback callback;
    private final Long currentUserId;
    private final Long groupId;

    public FriendSelectorDialog(Window owner, GroupDetailsService service,
                                Long currentUserId, Long groupId,
                                GroupDetailsService.FriendSelectCallback callback) {
        this.owner = owner;
        this.service = service;
        this.currentUserId = currentUserId;
        this.groupId = groupId;
        this.callback = callback;
    }

    public void show() {
        // 创建对话框
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setTitle("选择好友添加");

        // 主容器
        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setPrefSize(450, 550);

        // 标题
        Label titleLabel = new Label("请选择要添加的好友:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // 加载提示
        Label loadingLabel = new Label("正在加载好友列表...");
        loadingLabel.setStyle("-fx-text-fill: #7f8c8d;");

        // 好友列表
        ListView<HBox> friendListView = new ListView<>();
        friendListView.setPrefHeight(350);
        friendListView.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd;");

        // 按钮区域
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-padding: 8 20;");
        cancelButton.setOnAction(e -> dialogStage.close());

        Button selectButton = new Button("选择");
        selectButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 25;");
        selectButton.setDisable(true);

        buttonBox.getChildren().addAll(cancelButton, selectButton);

        // 初始添加组件
        mainContainer.getChildren().addAll(titleLabel, loadingLabel, friendListView, buttonBox);

        // 设置场景
        dialogStage.setScene(new javafx.scene.Scene(mainContainer));

        // 异步加载好友列表
        new Thread(() -> {
            List<Map<String, Object>> friends = service.getFriendsForAddMember(currentUserId, groupId);

            javafx.application.Platform.runLater(() -> {
                mainContainer.getChildren().remove(loadingLabel);

                if (friends == null || friends.isEmpty()) {
                    Label errorLabel = new Label("暂无可以添加的好友");
                    errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-padding: 10 0;");
                    mainContainer.getChildren().add(1, errorLabel);
                    return;
                }

                // 创建单选模式
                ToggleGroup toggleGroup = new ToggleGroup();

                for (Map<String, Object> friend : friends) {
                    try {
                        HBox friendItem = createFriendItem(friend, toggleGroup);
                        friendListView.getItems().add(friendItem);
                    } catch (Exception e) {
                        // 跳过无法处理的好友数据
                        continue;
                    }
                }

                // 监听选择变化
                toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                    selectButton.setDisable(newToggle == null);
                });

                // 选择按钮事件
                selectButton.setOnAction(e -> {
                    RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
                    if (selected != null) {
                        Long friendId = (Long) selected.getUserData();
                        String friendUsername = (String) selected.getText();

                        if (callback != null) {
                            callback.onFriendSelected(friendId, friendUsername);
                        }

                        dialogStage.close();
                    }
                });
            });
        }).start();

        dialogStage.show();
    }

    private HBox createFriendItem(Map<String, Object> friend, ToggleGroup toggleGroup) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-border-color: #eee; -fx-border-radius: 5; -fx-background-radius: 5; " +
                "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        item.setPrefHeight(60);

        // 从Map中获取数据
        Object uidObj = friend.get("uid");
        String username = (String) friend.get("username");
        String nickname = (String) friend.get("nickname");
        String avatarUrl = (String) friend.get("avatarUrl");

        Long friendId = null;
        if (uidObj instanceof Number) {
            friendId = ((Number) uidObj).longValue();
        }

        // 单选按钮
        RadioButton radioButton = new RadioButton();
        radioButton.setToggleGroup(toggleGroup);
        if (friendId != null) {
            radioButton.setUserData(friendId);
        }
        radioButton.setStyle("-fx-padding: 0 10 0 0;");

        // 设置显示文本
        String displayName = nickname != null && !nickname.isEmpty() ? nickname : username;
        radioButton.setText(displayName);

        // 头像
        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(40);
        avatarView.setFitHeight(40);
        AvatarHelper.loadAvatar(avatarView, avatarUrl, false, 40);
        avatarView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");

        // 用户信息
        VBox infoBox = new VBox(3);

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label usernameLabel = new Label("账号: " + username);
        usernameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        infoBox.getChildren().addAll(nameLabel, usernameLabel);

        // 布局
        item.getChildren().addAll(radioButton, avatarView, infoBox);

        return item;
    }
}