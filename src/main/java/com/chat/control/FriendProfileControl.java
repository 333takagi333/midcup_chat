package com.chat.control;

import com.chat.control.MainControl.FriendItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 好友资料控制器
 */
public class FriendProfileControl implements Initializable {

    @FXML private ImageView avatarImage;
    @FXML private Label nameLabel;
    @FXML private Label statusLabel;
    @FXML private Label userIdLabel;
    @FXML private Label signatureLabel;

    private FriendItem friend;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化界面
    }

    /**
     * 设置好友信息
     */
    public void setFriendInfo(FriendItem friend) {
        this.friend = friend;
        updateUI();
    }

    private void updateUI() {
        if (friend != null) {
            nameLabel.setText(friend.getUsername());
            statusLabel.setText("状态: " + friend.getStatus());
            userIdLabel.setText("用户ID: " + friend.getUserId());
            signatureLabel.setText("个性签名: " +
                    (friend.getSignature() != null ? friend.getSignature() : "这个用户很懒，什么都没有写"));

            // 设置头像
            if (friend.getAvatar() != null && !friend.getAvatar().isEmpty()) {
                try {
                    avatarImage.setImage(new Image(getClass().getResourceAsStream(friend.getAvatar())));
                } catch (Exception e) {
                    avatarImage.setImage(new Image(getClass().getResourceAsStream("/com/chat/images/default_avatar.png")));
                }
            }
        }
    }

    /**
     * 发送消息按钮点击
     */
    @FXML
    private void sendMessage() {
        System.out.println("打开与 " + friend.getUsername() + " 的聊天窗口");
        // TODO: 打开聊天窗口
    }

    /**
     * 删除好友按钮点击
     */
    @FXML
    private void deleteFriend() {
        System.out.println("删除好友: " + friend.getUsername());
        // TODO: 删除好友功能
    }
}