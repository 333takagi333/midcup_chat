package com.chat.ui;

import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * CellFactoryHelper - 提供 ListView 的 cellFactory 方法，减少 controller 中的 UI 代码。
 */
public final class CellFactoryHelper {
    private CellFactoryHelper() {}

    public static Callback<ListView<ChatItem>, ListCell<ChatItem>> chatCellFactory() {
        return param -> new ListCell<>() {
            @Override
            protected void updateItem(ChatItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

                    // 左侧：头像
                    ImageView avatar = new ImageView();
                    AvatarHelper.loadAvatar(avatar, item.getAvatarUrl(), item.isGroup());
                    avatar.setFitWidth(50);
                    avatar.setFitHeight(50);

                    // 中间：消息内容
                    VBox content = new VBox(5);
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

                    // 消息预览（已包含发送者信息）
                    Label messageLabel = new Label(item.getLastMessage());
                    messageLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

                    content.getChildren().addAll(nameLabel, messageLabel);

                    // 右侧：时间和未读标记
                    VBox rightPanel = new VBox(5);
                    Label timeLabel = new Label(item.getTime());
                    timeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

                    if (item.isUnread()) {
                        // 显示红点
                        Label unreadDot = new Label("●");
                        unreadDot.setStyle("-fx-text-fill: #ff3b30; -fx-font-size: 16;");
                        rightPanel.getChildren().addAll(timeLabel, unreadDot);
                    } else {
                        rightPanel.getChildren().add(timeLabel);
                    }

                    cell.getChildren().addAll(avatar, content, rightPanel);
                    setGraphic(cell);
                }
            }
        };
    }

    public static Callback<ListView<FriendItem>, ListCell<FriendItem>> friendCellFactory() {
        return param -> new ListCell<>() {
            @Override
            protected void updateItem(FriendItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

                    ImageView avatar = new ImageView();
                    AvatarHelper.loadAvatar(avatar, item.getAvatarUrl(), false);
                    avatar.setFitWidth(50);
                    avatar.setFitHeight(50);

                    VBox content = new VBox(5);
                    Label nameLabel = new Label(item.getUsername());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
                    content.getChildren().addAll(nameLabel);
                    cell.getChildren().addAll(avatar, content);
                    setGraphic(cell);
                }
            }
        };
    }

    public static Callback<ListView<GroupItem>, ListCell<GroupItem>> groupCellFactory() {
        return param -> new ListCell<>() {
            @Override
            protected void updateItem(GroupItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

                    ImageView avatar = new ImageView();
                    AvatarHelper.loadAvatar(avatar, item.getAvatarUrl(), true);
                    avatar.setFitWidth(50);
                    avatar.setFitHeight(50);

                    VBox content = new VBox(5);
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

                    HBox info = new HBox(10);
                    info.getChildren().addAll();
                    content.getChildren().addAll(nameLabel, info);
                    cell.getChildren().addAll(avatar, content);
                    setGraphic(cell);
                }
            }
        };
    }
}

