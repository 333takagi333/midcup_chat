package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.GroupDetailResponse;
import com.chat.service.GroupDetailsService;
import com.chat.ui.AvatarHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 群聊详情控制器 - 只处理UI显示和事件绑定
 */
public class GroupDetailsControl implements Initializable {

    @FXML private ImageView groupAvatar;
    @FXML private Label groupNameLabel, groupIdLabel, memberCountLabel;
    @FXML private TextField myNicknameField;
    @FXML private TextArea groupNoticeArea;
    @FXML private ListView<String> fileListView;
    @FXML private ListView<String> memberListView;
    @FXML private Button saveNicknameButton, exitGroupButton, downloadButton, updateNoticeButton;
    @FXML private VBox mainContainer;

    // 业务服务
    private GroupDetailsService groupDetailsService;

    // 数据
    private Long groupId;
    private Long currentUserId;
    private String groupName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AvatarHelper.setDefaultAvatar(groupAvatar, true, 100);

        // 设置按钮事件
        if (saveNicknameButton != null) {
            saveNicknameButton.setOnAction(event -> handleSaveNickname());
        }

        if (exitGroupButton != null) {
            exitGroupButton.setOnAction(event -> handleExitGroup());
            exitGroupButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
        }

        if (downloadButton != null) {
            downloadButton.setOnAction(event -> handleDownloadFile());
        }

        if (updateNoticeButton != null) {
            updateNoticeButton.setOnAction(event -> handleUpdateNotice());
        }

        // 初始化列表视图
        setupListView();
    }

    /**
     * 设置群聊信息（UI初始化）
     */
    public void setGroupInfo(String groupId, String groupName, String avatarUrl,
                             SocketClient socketClient, String currentUserId) {
        try {
            this.groupId = Long.parseLong(groupId);
            this.currentUserId = Long.parseLong(currentUserId);
            this.groupName = groupName;

            // 初始化服务
            this.groupDetailsService = new GroupDetailsService(socketClient);

            // 设置初始UI
            Platform.runLater(() -> {
                groupNameLabel.setText(groupName != null ? groupName : "未知群聊");
                groupIdLabel.setText("群号: " + groupId);

                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    AvatarHelper.loadAvatar(groupAvatar, avatarUrl, true, 100);
                }
            });

            // 加载详细信息
            loadGroupDetails();

        } catch (NumberFormatException e) {
            System.err.println("[GroupDetailsControl] ID格式错误: " + e.getMessage());
            Platform.runLater(() -> com.chat.ui.DialogUtil.showError(getCurrentWindow(), "ID格式错误"));
        }
    }

    /**
     * 设置列表视图
     */
    private void setupListView() {
        fileListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        });

        memberListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        });
    }

    /**
     * 加载群聊详细信息（调用服务）
     */
    private void loadGroupDetails() {
        groupDetailsService.loadAndDisplayGroupInfo(groupId, currentUserId,
                new GroupDetailsService.GroupDetailsUICallback() {
                    @Override
                    public void onSuccess(GroupDetailResponse response) {
                        Platform.runLater(() -> updateGroupDetailsUI(response));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Platform.runLater(() -> {
                            setDefaultInfo();
                            com.chat.ui.DialogUtil.showWarning(getCurrentWindow(), errorMessage);
                        });
                    }
                });
    }

    /**
     * 更新群聊详情UI
     */
    private void updateGroupDetailsUI(GroupDetailResponse response) {
        if (response.getGroupName() != null) {
            groupNameLabel.setText(response.getGroupName());
        }

        if (response.getGroupId() != null) {
            groupIdLabel.setText("群号: " + response.getGroupId());
        }

        if (response.getMemberCount() != null) {
            memberCountLabel.setText("成员: " + response.getMemberCount() + "人");
        }

        if (response.getNotice() != null) {
            groupNoticeArea.setText(response.getNotice());
        }

        if (response.getMyNickname() != null) {
            myNicknameField.setText(response.getMyNickname());
        }

        if (response.getAvatarUrl() != null && !response.getAvatarUrl().isEmpty()) {
            AvatarHelper.loadAvatar(groupAvatar, response.getAvatarUrl(), true, 100);
        }

        // 更新文件列表
        updateFileList(response.getFiles());

        // 更新成员列表
        updateMemberList(response.getMembers());
    }

    /**
     * 更新文件列表
     */
    private void updateFileList(List<GroupDetailResponse.GroupFile> files) {
        fileListView.getItems().clear();
        if (files != null && !files.isEmpty()) {
            for (GroupDetailResponse.GroupFile file : files) {
                String fileInfo = groupDetailsService.formatFileInfo(file);
                fileListView.getItems().add(fileInfo);
            }
        } else {
            fileListView.getItems().add("暂无文件");
        }
    }

    /**
     * 更新成员列表
     */
    private void updateMemberList(List<GroupDetailResponse.GroupMember> members) {
        memberListView.getItems().clear();
        if (members != null && !members.isEmpty()) {
            for (GroupDetailResponse.GroupMember member : members) {
                String memberInfo = groupDetailsService.formatMemberInfo(member);
                memberListView.getItems().add(memberInfo);
            }
        } else {
            memberListView.getItems().add("暂无成员信息");
        }
    }

    /**
     * 设置默认信息
     */
    private void setDefaultInfo() {
        memberCountLabel.setText("成员: 加载中...");
        groupNoticeArea.setText("暂无群公告");
        myNicknameField.setText("我在本群的昵称");
    }

    /**
     * 处理保存昵称（调用服务处理）
     */
    private void handleSaveNickname() {
        String nickname = myNicknameField.getText().trim();
        groupDetailsService.updateGroupNickname(groupId, currentUserId, nickname, getCurrentWindow());
    }

    /**
     * 处理下载文件（调用服务处理）
     */
    private void handleDownloadFile() {
        String selectedFile = fileListView.getSelectionModel().getSelectedItem();
        groupDetailsService.downloadGroupFile(groupId, "", selectedFile, getCurrentWindow());
    }

    /**
     * 处理更新群公告（调用服务处理）
     */
    private void handleUpdateNotice() {
        String notice = groupNoticeArea.getText().trim();
        groupDetailsService.updateGroupNotice(groupId, notice, currentUserId, getCurrentWindow());
    }

    /**
     * 处理退出群聊（调用服务处理）
     */
    private void handleExitGroup() {
        groupDetailsService.exitGroup(groupId, currentUserId, groupName, getCurrentWindow(),
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