package com.chat.control;

import com.chat.control.MainControl.GroupItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 群聊详情控制器
 */
public class GroupDetailsControl implements Initializable {

    @FXML private ImageView groupAvatar;
    @FXML private Label groupNameLabel;
    @FXML private Label memberCountLabel;
    @FXML private ListView<String> memberListView;

    private GroupItem group;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupMemberList();
    }

    private void setupMemberList() {
        // 设置成员列表的样式等
    }

    /**
     * 设置群聊信息
     */
    public void setGroupInfo(GroupItem group) {
        this.group = group;
        updateUI();
    }

    private void updateUI() {
        if (group != null) {
            groupNameLabel.setText(group.getName());
            memberCountLabel.setText("成员数量: " + group.getMemberCount());

            // 设置群头像
            if (group.getAvatar() != null && !group.getAvatar().isEmpty()) {
                try {
                    groupAvatar.setImage(new Image(getClass().getResourceAsStream(group.getAvatar())));
                } catch (Exception e) {
                    groupAvatar.setImage(new Image(getClass().getResourceAsStream("/com/chat/images/default_group.png")));
                }
            }

            // 加载示例成员
            loadSampleMembers();
        }
    }

    private void loadSampleMembers() {
        memberListView.getItems().clear();
        memberListView.getItems().addAll(
                "张三 (群主)",
                "李四",
                "王五",
                "赵六"
        );
    }

    /**
     * 进入群聊按钮点击
     */
    @FXML
    private void enterGroup() {
        System.out.println("进入群聊: " + group.getName());
        // TODO: 打开群聊窗口
    }

    /**
     * 退出群聊按钮点击
     */
    @FXML
    private void exitGroup() {
        System.out.println("退出群聊: " + group.getName());
        // TODO: 退出群聊功能
    }
}