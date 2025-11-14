package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.ui.DialogUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 主界面控制器 - 只负责UI显示，数据由服务端提供
 */
public class MainControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private HBox topBar;
    @FXML private ImageView avatarImage;
    @FXML private Label usernameLabel;
    @FXML private Button notificationButton;
    @FXML private MenuButton settingsMenu;

    @FXML private TabPane mainTabPane;
    @FXML private Tab messagesTab;
    @FXML private Tab contactsTab;
    @FXML private Tab groupsTab;

    @FXML private ListView<ChatItem> messagesListView;
    @FXML private ListView<FriendItem> contactsListView;
    @FXML private ListView<GroupItem> groupsListView;

    private String username;
    private String userId;
    private SocketClient socketClient;
    private Timer messageTimer;
    private Gson gson = new Gson();
    private boolean dataLoaded = false;

    // 数据模型类 - 只包含显示所需字段
    public static class ChatItem {
        private String id;
        private String name;
        private String lastMessage;
        private String time;
        private String avatar;
        private boolean unread;
        private boolean isGroup;

        public ChatItem(String id, String name, String lastMessage, String time, String avatar, boolean unread, boolean isGroup) {
            this.id = id;
            this.name = name;
            this.lastMessage = lastMessage;
            this.time = time;
            this.avatar = avatar;
            this.unread = unread;
            this.isGroup = isGroup;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getLastMessage() { return lastMessage; }
        public String getTime() { return time; }
        public String getAvatar() { return avatar; }
        public boolean isUnread() { return unread; }
        public boolean isGroup() { return isGroup; }
    }

    public static class FriendItem {
        private String userId;
        private String username;
        private String status;
        private String avatar;
        private String signature;

        public FriendItem(String userId, String username, String status, String avatar, String signature) {
            this.userId = userId;
            this.username = username;
            this.status = status;
            this.avatar = avatar;
            this.signature = signature;
        }

        // Getters
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getStatus() { return status; }
        public String getAvatar() { return avatar; }
        public String getSignature() { return signature; }
    }

    public static class GroupItem {
        private String groupId;
        private String name;
        private String lastMessage;
        private String memberCount;
        private String avatar;

        public GroupItem(String groupId, String name, String lastMessage, String memberCount, String avatar) {
            this.groupId = groupId;
            this.name = name;
            this.lastMessage = lastMessage;
            this.memberCount = memberCount;
            this.avatar = avatar;
        }

        // Getters
        public String getGroupId() { return groupId; }
        public String getName() { return name; }
        public String getLastMessage() { return lastMessage; }
        public String getMemberCount() { return memberCount; }
        public String getAvatar() { return avatar; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
        // 延迟启动消息监听，等待窗口完全显示
        Platform.runLater(() -> {
            startMessageListener();
            // 延迟加载数据，确保Scene已经附加
            if (!dataLoaded && username != null) {
                loadInitialData();
            }
        });
    }

    /**
     * 初始化UI组件
     */
    private void setupUI() {
        try {
            // 移除本地头像设置，等待从服务端获取
            avatarImage.setVisible(false);

            // 设置用户名样式
            usernameLabel.setFont(Font.font(16));

            // 设置通知按钮
            notificationButton.setText("通知");

            // 设置菜单按钮
            MenuItem profileItem = new MenuItem("个人资料");
            MenuItem settingsItem = new MenuItem("设置");
            MenuItem logoutItem = new MenuItem("退出登录");

            settingsMenu.getItems().addAll(profileItem, settingsItem, new SeparatorMenuItem(), logoutItem);

            // 设置列表视图
            setupListViews();

            // 初始化空列表
            clearAllData();

        } catch (Exception e) {
            System.err.println("[MainControl] UI设置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置列表视图的单元格工厂
     */
    private void setupListViews() {
        // 消息列表
        messagesListView.setCellFactory(param -> new ListCell<ChatItem>() {
            @Override
            protected void updateItem(ChatItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createChatCell(item);
                    setGraphic(cell);
                }
            }
        });

        // 联系人列表
        contactsListView.setCellFactory(param -> new ListCell<FriendItem>() {
            @Override
            protected void updateItem(FriendItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createFriendCell(item);
                    setGraphic(cell);
                }
            }
        });

        // 群聊列表
        groupsListView.setCellFactory(param -> new ListCell<GroupItem>() {
            @Override
            protected void updateItem(GroupItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createGroupCell(item);
                    setGraphic(cell);
                }
            }
        });
    }

    /**
     * 创建消息列表单元格 - 使用服务端提供的头像URL
     */
    private HBox createChatCell(ChatItem item) {
        HBox cell = new HBox(10);
        cell.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

        // 使用服务端提供的头像URL或默认占位符
        ImageView avatar = new ImageView();
        loadAvatarFromServer(item.getAvatar(), avatar, item.isGroup());
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);

        VBox content = new VBox(5);
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label messageLabel = new Label(item.getLastMessage());
        messageLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        content.getChildren().addAll(nameLabel, messageLabel);

        VBox rightPanel = new VBox(5);
        Label timeLabel = new Label(item.getTime());
        timeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

        if (item.isUnread()) {
            Label unreadLabel = new Label("●");
            unreadLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16;");
            rightPanel.getChildren().addAll(timeLabel, unreadLabel);
        } else {
            rightPanel.getChildren().add(timeLabel);
        }

        cell.getChildren().addAll(avatar, content, rightPanel);
        return cell;
    }

    /**
     * 创建联系人列表单元格 - 使用服务端提供的头像URL
     */
    private HBox createFriendCell(FriendItem item) {
        HBox cell = new HBox(10);
        cell.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

        // 使用服务端提供的头像URL
        ImageView avatar = new ImageView();
        loadAvatarFromServer(item.getAvatar(), avatar, false);
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);

        VBox content = new VBox(5);
        Label nameLabel = new Label(item.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label statusLabel = new Label(item.getStatus());
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        content.getChildren().addAll(nameLabel, statusLabel);
        cell.getChildren().addAll(avatar, content);
        return cell;
    }

    /**
     * 创建群聊列表单元格 - 使用服务端提供的头像URL
     */
    private HBox createGroupCell(GroupItem item) {
        HBox cell = new HBox(10);
        cell.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

        // 使用服务端提供的头像URL
        ImageView avatar = new ImageView();
        loadAvatarFromServer(item.getAvatar(), avatar, true);
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);

        VBox content = new VBox(5);
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        HBox info = new HBox(10);
        Label messageLabel = new Label(item.getLastMessage());
        messageLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        Label countLabel = new Label(item.getMemberCount());
        countLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

        info.getChildren().addAll(messageLabel, countLabel);
        content.getChildren().addAll(nameLabel, info);
        cell.getChildren().addAll(avatar, content);
        return cell;
    }

    /**
     * 从服务端加载头像
     */
    private void loadAvatarFromServer(String avatarUrl, ImageView imageView, boolean isGroup) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            // 如果服务端提供了头像URL，异步加载
            loadRemoteAvatar(avatarUrl, imageView);
        } else {
            // 使用默认占位符
            setDefaultAvatar(imageView, isGroup);
        }
    }

    /**
     * 加载远程头像
     */
    private void loadRemoteAvatar(String avatarUrl, ImageView imageView) {
        // 这里应该实现从服务端下载头像的逻辑
        // 暂时使用默认头像，实际项目中需要实现HTTP请求下载
        setDefaultAvatar(imageView, false);
    }

    /**
     * 设置默认头像（占位符）
     */
    private void setDefaultAvatar(ImageView imageView, boolean isGroup) {
        imageView.setImage(createDefaultAvatar(isGroup));
        imageView.setVisible(true);
    }

    /**
     * 创建默认头像（程序生成）
     */
    private Image createDefaultAvatar(boolean isGroup) {
        int width = isGroup ? 50 : 40;
        int height = isGroup ? 50 : 40;
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        // 根据类型选择颜色
        Color baseColor = isGroup ? Color.BLUE : Color.GREEN;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double dx = x - width / 2.0;
                double dy = y - height / 2.0;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= width / 2.0) {
                    pixelWriter.setColor(x, y, baseColor);
                } else {
                    pixelWriter.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        return image;
    }

    /**
     * 从服务端加载当前用户的头像
     */
    private void loadUserAvatar() {
        if (userId != null && socketClient != null) {
            // 向服务端请求用户头像
            // 这里需要根据您的协议实现具体的请求
            try {
                // 暂时使用默认头像
                setDefaultAvatar(avatarImage, false);
            } catch (Exception e) {
                System.err.println("[MainControl] 请求用户头像失败: " + e.getMessage());
                setDefaultAvatar(avatarImage, false);
            }
        } else {
            setDefaultAvatar(avatarImage, false);
        }
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 消息列表点击事件
        messagesListView.setOnMouseClicked(event -> {
            ChatItem selected = messagesListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                if (selected.isGroup()) {
                    openGroupChat(selected);
                } else {
                    openPrivateChat(selected);
                }
            }
        });

        // 联系人列表点击事件
        contactsListView.setOnMouseClicked(event -> {
            FriendItem selected = contactsListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                openFriendProfile(selected);
            }
        });

        // 群聊列表点击事件
        groupsListView.setOnMouseClicked(event -> {
            GroupItem selected = groupsListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                openGroupDetails(selected);
            }
        });

        // 通知按钮点击事件
        notificationButton.setOnAction(event -> showNotifications());

        // 菜单项事件
        settingsMenu.getItems().get(0).setOnAction(event -> showUserProfile());
        settingsMenu.getItems().get(1).setOnAction(event -> showSettings());
        settingsMenu.getItems().get(3).setOnAction(event -> logout());
    }

    /**
     * 打开私聊窗口
     */
    private void openPrivateChat(ChatItem chat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/ChatPrivate.fxml"));
            Parent root = loader.load();

            ChatPrivateControl controller = loader.getController();
            controller.setChatInfo(chat.getId(), chat.getName(), chat.getAvatar(), socketClient, userId);

            Stage stage = new Stage();
            stage.setTitle("与 " + chat.getName() + " 聊天");
            stage.setScene(new Scene(root, 600, 700));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("打开聊天窗口失败: " + e.getMessage());
        }
    }

    /**
     * 打开群聊窗口
     */
    private void openGroupChat(ChatItem chat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/ChatGroup.fxml"));
            Parent root = loader.load();

            ChatGroupControl controller = loader.getController();
            controller.setGroupInfo(chat.getId(), chat.getName(), chat.getAvatar(), socketClient, userId);

            Stage stage = new Stage();
            stage.setTitle("群聊: " + chat.getName());
            stage.setScene(new Scene(root, 600, 700));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("打开群聊窗口失败: " + e.getMessage());
        }
    }

    /**
     * 打开好友资料
     */
    private void openFriendProfile(FriendItem friend) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/FriendProfile.fxml"));
            Parent root = loader.load();

            FriendProfileControl controller = loader.getController();
            controller.setFriendInfo(friend);

            Stage stage = new Stage();
            stage.setTitle(friend.getUsername() + " 的资料");
            stage.setScene(new Scene(root, 400, 500));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("打开好友资料失败: " + e.getMessage());
        }
    }

    /**
     * 打开群聊详情
     */
    private void openGroupDetails(GroupItem group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/GroupDetails.fxml"));
            Parent root = loader.load();

            GroupDetailsControl controller = loader.getController();
            controller.setGroupInfo(group);

            Stage stage = new Stage();
            stage.setTitle(group.getName() + " 的详情");
            stage.setScene(new Scene(root, 500, 600));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("打开群聊详情失败: " + e.getMessage());
        }
    }

    /**
     * 显示通知
     */
    private void showNotifications() {
        showInfoDialog("暂无新通知");
    }

    /**
     * 显示个人资料
     */
    private void showUserProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/UserProfile.fxml"));
            Parent root = loader.load();

            UserProfileControl controller = loader.getController();
            controller.setUserInfo(username, userId, socketClient);

            Stage stage = new Stage();
            stage.setTitle("个人资料");
            stage.setScene(new Scene(root, 400, 500));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("打开个人资料失败: " + e.getMessage());
        }
    }

    /**
     * 显示设置
     */
    private void showSettings() {
        showInfoDialog("设置功能开发中...");
    }

    /**
     * 退出登录
     */
    private void logout() {
        if (showConfirmationDialog("确认退出登录？")) {
            stopMessageListener();
            if (socketClient != null) {
                socketClient.disconnect();
            }
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * 开始监听服务器消息
     */
    private void startMessageListener() {
        messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (socketClient != null && socketClient.isConnected()) {
                    String message = socketClient.receiveMessage();
                    if (message != null) {
                        Platform.runLater(() -> handleServerMessage(message));
                    }
                }
            }
        }, 0, 100);
    }

    /**
     * 停止消息监听
     */
    private void stopMessageListener() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
    }

    /**
     * 处理服务器消息
     */
    private void handleServerMessage(String message) {
        try {
            System.out.println("[MAIN] 收到服务器消息: " + message);

            // 根据消息类型路由到不同的处理方法
            if (message.contains("\"type\":\"friend_list_response\"")) {
                handleFriendListResponse(message);
            } else if (message.contains("\"type\":\"group_list_response\"")) {
                handleGroupListResponse(message);
            } else if (message.contains("\"type\":\"user_profile_response\"")) {
                handleUserProfileResponse(message);
            } else if (message.contains("\"type\":\"chat_private_receive\"")) {
                handlePrivateMessage(message);
            } else if (message.contains("\"type\":\"chat_group_receive\"")) {
                handleGroupMessage(message);
            } else if (message.contains("\"type\":\"friend_add_response\"")) {
                handleFriendAddResponse(message);
            }

        } catch (Exception e) {
            System.err.println("处理服务器消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理好友列表响应 - 从服务端获取数据
     */
    private void handleFriendListResponse(String response) {
        try {
            FriendListResponse friendResponse = gson.fromJson(response, FriendListResponse.class);
            if (friendResponse != null && friendResponse.getFriends() != null) {
                updateFriendList(friendResponse.getFriends());
                System.out.println("[MAIN] 好友列表更新，数量: " + friendResponse.getFriends().size());
            } else {
                System.err.println("[MAIN] 好友列表响应为空");
            }
        } catch (Exception e) {
            System.err.println("解析好友列表响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理群组列表响应 - 从服务端获取数据
     */
    private void handleGroupListResponse(String response) {
        try {
            GroupListResponse groupResponse = gson.fromJson(response, GroupListResponse.class);
            if (groupResponse != null && groupResponse.getGroups() != null) {
                updateGroupList(groupResponse.getGroups());
                System.out.println("[MAIN] 群组列表更新，数量: " + groupResponse.getGroups().size());
            } else {
                System.err.println("[MAIN] 群组列表响应为空");
            }
        } catch (Exception e) {
            System.err.println("解析群组列表响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理用户资料响应 - 包含头像信息
     */
    private void handleUserProfileResponse(String response) {
        try {
            // 解析用户资料响应，获取头像URL
            // UserProfileResponse profileResponse = gson.fromJson(response, UserProfileResponse.class);
            // if (profileResponse != null && profileResponse.getAvatarUrl() != null) {
            //     loadRemoteAvatar(profileResponse.getAvatarUrl(), avatarImage);
            // }
        } catch (Exception e) {
            System.err.println("解析用户资料响应失败: " + e.getMessage());
        }
    }

    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(String messageJson) {
        try {
            ChatPrivateReceive message = gson.fromJson(messageJson, ChatPrivateReceive.class);
            if (message != null) {
                updateChatListWithNewMessage(message.getFromUserId().toString(),
                        message.getContent(), false);
            }
        } catch (Exception e) {
            System.err.println("解析私聊消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(String messageJson) {
        try {
            ChatGroupReceive message = gson.fromJson(messageJson, ChatGroupReceive.class);
            if (message != null) {
                updateChatListWithNewMessage(message.getGroupId().toString(),
                        message.getContent(), true);
            }
        } catch (Exception e) {
            System.err.println("解析群聊消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理好友添加响应
     */
    private void handleFriendAddResponse(String messageJson) {
        try {
            FriendAddResponse response = gson.fromJson(messageJson, FriendAddResponse.class);
            if (response != null && response.isSuccess()) {
                showInfoDialog("好友添加成功");
                // 重新加载好友列表
                loadFriendsFromServer();
            } else {
                showErrorDialog(response != null ? response.getMessage() : "好友添加失败");
            }
        } catch (Exception e) {
            System.err.println("解析好友添加响应失败: " + e.getMessage());
        }
    }

    /**
     * 更新好友列表显示 - 适配新的协议字段
     */
    private void updateFriendList(java.util.List<FriendListResponse.FriendItem> friends) {
        ObservableList<FriendItem> items = FXCollections.observableArrayList();
        if (friends != null) {
            for (FriendListResponse.FriendItem friend : friends) {
                // 使用服务端提供的字段
                items.add(new FriendItem(
                        friend.getUid() != null ? friend.getUid().toString() : "0",
                        friend.getUsername() != null ? friend.getUsername() : "未知用户",
                        "在线", // 状态信息需要服务端提供
                        friend.getAvatarUrl(), // 直接使用服务端提供的头像URL
                        "" // 签名信息需要服务端提供
                ));
            }
        }
        contactsListView.setItems(items);
    }

    /**
     * 更新群组列表显示 - 适配新的协议字段
     */
    private void updateGroupList(java.util.List<GroupListResponse.GroupItem> groups) {
        ObservableList<GroupItem> items = FXCollections.observableArrayList();
        if (groups != null) {
            for (GroupListResponse.GroupItem group : groups) {
                // 使用服务端提供的字段
                items.add(new GroupItem(
                        group.getId() != null ? group.getId().toString() : "0",
                        group.getName() != null ? group.getName() : "未知群组",
                        "暂无消息", // 最后消息需要服务端提供
                        "0", // 成员数量需要服务端提供
                        group.getAvatar() // 直接使用服务端提供的头像URL
                ));
            }
        }
        groupsListView.setItems(items);
    }

    /**
     * 更新聊天列表显示新消息
     */
    private void updateChatListWithNewMessage(String id, String content, boolean isGroup) {
        ObservableList<ChatItem> items = messagesListView.getItems();

        // 查找是否已存在该聊天
        for (int i = 0; i < items.size(); i++) {
            ChatItem item = items.get(i);
            if (item.getId().equals(id) && item.isGroup() == isGroup) {
                // 更新现有聊天项
                items.set(i, new ChatItem(item.getId(), item.getName(), content, "刚刚",
                        item.getAvatar(), true, isGroup));
                return;
            }
        }

        // 如果是新聊天，添加到列表
        String name = isGroup ? "群聊" + id : "用户" + id;
        String avatar = ""; // 空字符串，等待从服务端获取
        items.add(0, new ChatItem(id, name, content, "刚刚", avatar, true, isGroup));
    }

    /**
     * 从服务端加载好友列表
     */
    public void loadFriendsFromServer() {
        if (socketClient != null && socketClient.isConnected()) {
            FriendListRequest request = new FriendListRequest();
            socketClient.sendFriendListRequest(request);
            System.out.println("[MAIN] 发送好友列表请求");
        } else {
            showErrorDialog("未连接到服务器");
        }
    }

    /**
     * 从服务端加载群组列表
     */
    public void loadGroupsFromServer() {
        if (socketClient != null && socketClient.isConnected()) {
            GroupListRequest request = new GroupListRequest();
            socketClient.sendGroupListRequest(request);
            System.out.println("[MAIN] 发送群组列表请求");
        } else {
            showErrorDialog("未连接到服务器");
        }
    }

    /**
     * 清空所有数据
     */
    private void clearAllData() {
        messagesListView.setItems(FXCollections.observableArrayList());
        contactsListView.setItems(FXCollections.observableArrayList());
        groupsListView.setItems(FXCollections.observableArrayList());
    }

    /**
     * 加载初始数据
     */
    public void loadInitialData() {
        if (dataLoaded) return;

        clearAllData();
        loadFriendsFromServer();
        loadGroupsFromServer();
        dataLoaded = true;
        System.out.println("[MAIN] 初始数据加载完成");
    }

    // 安全的对话框显示方法
    private void showErrorDialog(String message) {
        Platform.runLater(() -> {
            if (mainContainer.getScene() != null && mainContainer.getScene().getWindow() != null) {
                DialogUtil.showError(mainContainer.getScene().getWindow(), message);
            } else {
                System.err.println("[MainControl] 无法显示错误对话框，Scene未就绪: " + message);
            }
        });
    }

    private void showInfoDialog(String message) {
        Platform.runLater(() -> {
            if (mainContainer.getScene() != null && mainContainer.getScene().getWindow() != null) {
                DialogUtil.showInfo(mainContainer.getScene().getWindow(), message);
            } else {
                System.err.println("[MainControl] 无法显示信息对话框，Scene未就绪: " + message);
            }
        });
    }

    private boolean showConfirmationDialog(String message) {
        if (mainContainer.getScene() != null && mainContainer.getScene().getWindow() != null) {
            return DialogUtil.showConfirmation(mainContainer.getScene().getWindow(), message);
        } else {
            System.err.println("[MainControl] 无法显示确认对话框，Scene未就绪: " + message);
            return false;
        }
    }

    // Setter 方法
    public void setUsername(String username) {
        this.username = username;
        usernameLabel.setText(username);
        loadUserAvatar(); // 加载用户头像
        // 延迟加载数据，确保Scene已经就绪
        Platform.runLater(this::loadInitialData);
    }

    public void setUserId(String userId) {
        this.userId = userId;
        loadUserAvatar(); // 加载用户头像
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
    }
}