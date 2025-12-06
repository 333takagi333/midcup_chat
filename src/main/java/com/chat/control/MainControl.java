package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import com.chat.protocol.*;
import com.chat.service.*;
import com.chat.ui.DialogHelper;
import com.chat.ui.AvatarHelper;
import com.chat.ui.CellFactoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 主界面控制器 - 仅处理UI交互
 */
public class MainControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label usernameLabel;
    @FXML private Button notificationButton;
    @FXML private ListView<ChatItem> messagesListView;
    @FXML private ListView<FriendItem> contactsListView;
    @FXML private ListView<GroupItem> groupsListView;
    @FXML private MenuButton mainMenu;
    @FXML private Button refreshButton;

    private String username;
    private String userId;
    private SocketClient socketClient;
    private Timer messageTimer;
    private final Gson gson = new Gson();
    private boolean dataLoaded = false;
    private int notificationCount = 0;

    // Service层引用
    private final ChatStateService stateService = new ChatStateService();
    private final MainDataService mainDataService = new MainDataService();
    private final NotificationManagementService notificationService = new NotificationManagementService();
    private final WindowManagementService windowService = new WindowManagementService();
    private UserProfileService userProfileService;

    private final Map<String, Consumer<String>> messageHandlers = new HashMap<>();

    // 窗口键常量
    private static final String USER_PROFILE_WINDOW_KEY = "user_profile";
    private static final String ADD_FRIEND_WINDOW_KEY = "add_friend";
    private static final String CREATE_GROUP_WINDOW_KEY = "create_group";
    private static final String SETTINGS_WINDOW_KEY = "settings";
    private static final String NOTIFICATION_WINDOW_KEY = "notifications";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupDataBinding();
        setupEventHandlers();
        registerMessageHandlers();
        loadUserAvatar();

        Platform.runLater(() -> {
            startMessageListener();
            tryLoadInitialDataIfReady();
        });
    }

    private void setupUI() {
        messagesListView.setCellFactory(CellFactoryHelper.chatCellFactory());
        contactsListView.setCellFactory(CellFactoryHelper.friendCellFactory());
        groupsListView.setCellFactory(CellFactoryHelper.groupCellFactory());
        clearAllData();

        updateNotificationButton();

        if (refreshButton != null) {
            refreshButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    private void setupDataBinding() {
        messagesListView.setItems(stateService.getChatItems());
        contactsListView.setItems(stateService.getFriendItems());
        groupsListView.setItems(stateService.getGroupItems());
    }

    private void setupEventHandlers() {
        messagesListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ChatItem selected = messagesListView.getSelectionModel().getSelectedItem();
                if (selected != null) openChatWindow(selected);
            }
        });

        contactsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FriendItem friend = contactsListView.getSelectionModel().getSelectedItem();
                if (friend != null) openPrivateChat(friend);
            }
        });

        groupsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                GroupItem group = groupsListView.getSelectionModel().getSelectedItem();
                if (group != null) openGroupChat(group);
            }
        });

        if (notificationButton != null) {
            notificationButton.setOnAction(event -> showNotifications());
        }

        if (refreshButton != null) {
            refreshButton.setOnAction(event -> handleRefreshClick());
        }
    }

    // ========== 刷新功能 ==========

    @FXML
    private void handleRefreshClick() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        System.out.println("[MainControl] 用户点击刷新按钮");
        showRefreshStatus("刷新");
        refreshAllDataWithFeedback();
    }

    private void refreshAllDataWithFeedback() {
        new Thread(() -> {
            try {
                MainDataService.RefreshResult result = mainDataService.refreshAll(socketClient);

                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        // 更新ObservableList - 使用修复后的方法名
                        mainDataService.updateFriendList(stateService.getFriendItems(), result.getFriends());
                        mainDataService.updateGroupList(stateService.getGroupItems(), result.getGroups());

                        // 强制刷新UI显示
                        contactsListView.refresh();
                        groupsListView.refresh();

                        showRefreshStatus("刷新完成!");
                        System.out.println("[MainControl] 刷新成功");
                    } else {
                        showRefreshStatus("刷新失败");
                        DialogHelper.showError(mainContainer.getScene().getWindow(),
                                result.getMessage() != null ? result.getMessage() : "刷新失败");
                        System.err.println("[MainControl] 刷新失败: " + result.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showRefreshStatus("刷新失败");
                    DialogHelper.showError(mainContainer.getScene().getWindow(), "刷新失败: " + e.getMessage());
                    System.err.println("[MainControl] 刷新数据失败: " + e.getMessage());
                });
            }
        }).start();
    }


    private void showRefreshStatus(String status) {
        if (refreshButton != null) {
            String originalText = refreshButton.getText();
            Platform.runLater(() -> {
                refreshButton.setText(status);
                refreshButton.setDisable(true);
            });

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                Platform.runLater(() -> {
                    refreshButton.setText(originalText);
                    refreshButton.setDisable(false);
                });
            }).start();
        }
    }

    // ========== 通知管理方法 ==========
    private void updateNotificationButton() {
        Platform.runLater(() -> {
            notificationButton.setText(notificationService.getNotificationButtonText(notificationCount));
            notificationButton.setStyle(notificationService.getNotificationButtonStyle(notificationCount));
        });
    }

    private void addNotification() {
        notificationCount++;
        updateNotificationButton();
    }

    private void clearNotificationCount() {
        notificationCount = 0;
        updateNotificationButton();
    }

    // ========== 窗口管理方法 ==========
    private void openChatWindow(ChatItem chat) {
        String windowKey = WindowManagementService.getChatWindowKey(chat);
        if (windowService.isWindowAlreadyOpen(windowKey)) {
            windowService.bringWindowToFront(windowKey);
            return;
        }

        String fxmlPath = chat.isGroup() ? "/com/chat/fxml/ChatGroup.fxml" : "/com/chat/fxml/ChatPrivate.fxml";
        String title = chat.isGroup() ? "群聊: " + chat.getName() : "与 " + chat.getName() + " 聊天";

        windowService.openManagedWindow(getClass(), fxmlPath,
                controller -> setupChatController(controller, chat),
                title, 600, 700, windowKey);
    }

    private void openPrivateChat(FriendItem friend) {
        String windowKey = WindowManagementService.getPrivateChatWindowKey(friend);
        if (windowService.isWindowAlreadyOpen(windowKey)) {
            windowService.bringWindowToFront(windowKey);
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/ChatPrivate.fxml",
                controller -> setupPrivateController(controller, friend),
                "与 " + friend.getUsername() + " 聊天", 600, 700, windowKey);

        stateService.upsertChat(friend.getUserId(), friend.getUsername(),
                friend.getAvatarUrl(), "已打开聊天", false, false);
    }

    private void openGroupChat(GroupItem group) {
        String windowKey = WindowManagementService.getGroupChatWindowKey(group);
        if (windowService.isWindowAlreadyOpen(windowKey)) {
            windowService.bringWindowToFront(windowKey);
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/ChatGroup.fxml",
                controller -> setupGroupController(controller, group),
                "群聊: " + group.getName(), 600, 700, windowKey);

        stateService.upsertChat(group.getGroupId(), group.getName(),
                group.getAvatarUrl(), "已打开聊天", false, true);
    }

    private void setupChatController(Object controller, ChatItem chat) {
        if (controller instanceof ChatPrivateControl) {
            ((ChatPrivateControl) controller).setChatInfo(chat.getId(), chat.getName(),
                    chat.getAvatarUrl(), socketClient, userId);
        } else if (controller instanceof ChatGroupControl) {
            ((ChatGroupControl) controller).setGroupInfo(chat.getId(), chat.getName(),
                    chat.getAvatarUrl(), socketClient, userId);
        }
    }

    private void setupPrivateController(Object controller, FriendItem friend) {
        if (controller instanceof ChatPrivateControl) {
            ((ChatPrivateControl) controller).setChatInfo(friend.getUserId(),
                    friend.getUsername(), friend.getAvatarUrl(), socketClient, userId);
        }
    }

    private void setupGroupController(Object controller, GroupItem group) {
        if (controller instanceof ChatGroupControl) {
            ((ChatGroupControl) controller).setGroupInfo(group.getGroupId(),
                    group.getName(), group.getAvatarUrl(), socketClient, userId);
        }
    }

    // ========== 数据加载 ==========
    private void tryLoadInitialDataIfReady() {
        if (!dataLoaded && username != null && isConnected()) {
            loadInitialData();
        }
    }

    public void loadInitialData() {
        if (dataLoaded || !isConnected()) return;

        new Thread(() -> {
            MainDataService.RefreshResult result = mainDataService.refreshAll(socketClient);
            if (result.isSuccess()) {
                Platform.runLater(() -> {
                    // 使用修复后的方法名
                    mainDataService.updateFriendList(stateService.getFriendItems(), result.getFriends());
                    mainDataService.updateGroupList(stateService.getGroupItems(), result.getGroups());
                    dataLoaded = true;
                });
            }
        }).start();
    }

    // ========== 消息处理 ==========
    private void registerMessageHandlers() {
        messageHandlers.put(MessageType.FRIEND_LIST_RESPONSE, this::handleFriendListResponse);
        messageHandlers.put(MessageType.GROUP_LIST_RESPONSE, this::handleGroupListResponse);
        messageHandlers.put(MessageType.CHAT_PRIVATE_RECEIVE, this::handlePrivateMessage);
        messageHandlers.put(MessageType.CHAT_GROUP_RECEIVE, this::handleGroupMessage);
        messageHandlers.put(MessageType.FRIEND_ADD_RESPONSE, this::handleFriendAddResponse);
        messageHandlers.put(MessageType.GROUP_CREATE_RESPONSE, this::handleGroupCreateResponse);
        messageHandlers.put(MessageType.FRIEND_REQUEST_RECEIVE, this::handleFriendRequestReceive);
    }

    private void handleServerMessage(String message) {
        try {
            JsonObject obj = gson.fromJson(message, JsonObject.class);
            String type = obj.get("type").getAsString();
            Consumer<String> handler = messageHandlers.get(type);
            if (handler != null) {
                handler.accept(message);
            } else {
                System.out.println("未注册的消息类型: " + type);
            }
        } catch (Exception e) {
            System.err.println("处理服务器消息失败: " + e.getMessage());
        }
    }

    private void handleFriendListResponse(String response) {
        FriendListResponse res = gson.fromJson(response, FriendListResponse.class);
        if (res != null) stateService.updateFriendList(res.getFriends());
    }

    private void handleGroupListResponse(String response) {
        GroupListResponse res = gson.fromJson(response, GroupListResponse.class);
        if (res != null) stateService.updateGroupList(res.getGroups());
    }

    private void handlePrivateMessage(String messageJson) {
        ChatPrivateReceive message = gson.fromJson(messageJson, ChatPrivateReceive.class);
        stateService.handlePrivateMessage(message);
    }

    private void handleGroupMessage(String messageJson) {
        ChatGroupReceive message = gson.fromJson(messageJson, ChatGroupReceive.class);
        stateService.handleGroupMessage(message);
    }

    private void handleFriendAddResponse(String messageJson) {
        FriendAddResponse response = gson.fromJson(messageJson, FriendAddResponse.class);
        if (response != null && response.isSuccess()) {
            DialogHelper.showInfo(mainContainer.getScene().getWindow(), "好友添加成功");
            refreshAllDataWithFeedback();
        } else {
            String errorMsg = response != null ? response.getMessage() : "未知错误";
            DialogHelper.showError(mainContainer.getScene().getWindow(), "好友添加失败: " + errorMsg);
        }
    }

    private void handleGroupCreateResponse(String messageJson) {
        GroupCreateResponse response = gson.fromJson(messageJson, GroupCreateResponse.class);
        if (response != null && response.isSuccess()) {
            DialogHelper.showInfo(mainContainer.getScene().getWindow(),
                    "群聊创建成功: " + response.getGroupName());
            refreshAllDataWithFeedback();
        } else {
            String errorMsg = response != null ? response.getMessage() : "未知错误";
            DialogHelper.showError(mainContainer.getScene().getWindow(), "群聊创建失败: " + errorMsg);
        }
    }

    private void handleFriendRequestReceive(String messageJson) {
        try {
            JsonObject obj = gson.fromJson(messageJson, JsonObject.class);
            Long requestId = obj.get("requestId").getAsLong();
            Long fromUserId = obj.get("fromUserId").getAsLong();
            String fromUsername = obj.get("fromUsername").getAsString();
            String requestTime = obj.get("requestTime").getAsString();

            Platform.runLater(() -> {
                addNotification();

                if (windowService.isWindowAlreadyOpen(NOTIFICATION_WINDOW_KEY)) {
                    NotificationControl control = windowService.getWindowController(NOTIFICATION_WINDOW_KEY);
                    if (control != null) {
                        control.addNotificationFromMainControl(
                                requestId,
                                fromUserId,
                                fromUsername,
                                requestTime
                        );
                    }
                }

                notificationService.showDesktopNotification(mainContainer.getScene().getWindow(),
                        "新好友请求", "用户 " + fromUsername + " 请求添加您为好友");
            });
        } catch (Exception e) {
            System.err.println("处理好友请求失败: " + e.getMessage());
        }
    }

    // ========== 工具方法 ==========
    private boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
    }

    private void startMessageListener() {
        if (messageTimer != null) {
            messageTimer.cancel();
        }

        messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected()) {
                    String message = socketClient.receiveMessage();
                    if (message != null && !message.trim().isEmpty()) {
                        Platform.runLater(() -> handleServerMessage(message));
                    }
                }
            }
        }, 0, 100);
    }

    private void stopMessageListener() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
        windowService.closeAllWindows();
    }

    private void clearAllData() {
        stateService.clearAll();
    }

    // ========== UI 操作方法 ==========
    @FXML
    private void showNotifications() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/Notifications.fxml",
                controller -> {
                    NotificationControl notificationControl = (NotificationControl) controller;
                    notificationControl.setSocketClient(socketClient);
                    notificationControl.setUserId(userId);
                },
                notificationService.getNotificationWindowTitle(notificationCount),
                650, 550, NOTIFICATION_WINDOW_KEY);

        clearNotificationCount();
    }

    @FXML
    private void showUserProfile() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/UserProfile.fxml",
                controller -> ((UserProfileControl) controller).setUserInfo(username, userId, socketClient),
                "个人资料", 400, 500, USER_PROFILE_WINDOW_KEY);
    }

    @FXML
    private void showAddFriendDialog() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/AddFriend.fxml",
                controller -> ((AddFriendControl) controller).setSocketClient(socketClient),
                "添加好友", 400, 250, ADD_FRIEND_WINDOW_KEY);
    }

    @FXML
    private void showCreateGroupDialog() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/CreateGroup.fxml",
                controller -> ((CreateGroupControl) controller).setSocketClient(socketClient),
                "创建群聊", 400, 250, CREATE_GROUP_WINDOW_KEY);
    }

    @FXML
    private void showSettings() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        windowService.openManagedWindow(getClass(), "/com/chat/fxml/Settings.fxml",
                controller -> {
                    SettingControl settingControl = (SettingControl) controller;
                    settingControl.setUserInfo(username, userId);
                    settingControl.setSocketClient(socketClient);

                    settingControl.setLogoutCallback(() -> {
                        System.out.println("设置界面触发退出，关闭所有窗口...");
                        stopMessageListener();

                        if (socketClient != null) {
                            socketClient.disconnect();
                        }

                        Platform.runLater(() -> {
                            javafx.stage.Stage mainStage = (javafx.stage.Stage) mainContainer.getScene().getWindow();
                            if (mainStage != null && mainStage.isShowing()) {
                                mainStage.close();
                            }

                            windowService.closeAllWindows();
                            showLoginScreen();
                        });
                    });

                    settingControl.setPasswordResetCallback((success) -> {
                        if (success) {
                            System.out.println("收到密码重置成功通知");
                        }
                    });
                },
                "设置 - " + username, 500, 450, SETTINGS_WINDOW_KEY);
    }

    private void showLoginScreen() {
        try {
            System.out.println("显示登录界面...");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/chat/fxml/Login.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("登录");
            stage.show();

            System.out.println("登录界面已显示");
        } catch (Exception e) {
            System.err.println("显示登录界面失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 头像加载方法 ==========
    private void loadUserAvatar() {
        if (avatarImage == null) return;

        if (userId != null && !userId.isEmpty() && isConnected()) {
            loadUserAvatarFromServer();
        } else {
            AvatarHelper.setDefaultAvatar(avatarImage, false);
        }
    }

    private void loadUserAvatarFromServer() {
        try {
            if (userProfileService == null) {
                userProfileService = new UserProfileService(socketClient);
            }

            Long userIdLong = Long.parseLong(userId);
            UserInfoResponse userInfo = userProfileService.loadUserInfo(userIdLong);

            if (userInfo != null && userInfo.getAvatarUrl() != null) {
                AvatarHelper.loadAvatar(avatarImage, userInfo.getAvatarUrl(), false, 40);
                return;
            }
        } catch (Exception e) {
            System.err.println("加载用户头像信息失败: " + e.getMessage());
        }

        AvatarHelper.setDefaultAvatar(avatarImage, false);
    }

    // ========== Setter 方法 ==========
    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        if (userId != null && isConnected()) {
            AvatarHelper.loadAvatar(avatarImage, null, false);
        }
        Platform.runLater(this::tryLoadInitialDataIfReady);
    }

    public void setUsername(String username) {
        this.username = username;
        if (usernameLabel != null) {
            usernameLabel.setText(username);
        }
        Platform.runLater(this::tryLoadInitialDataIfReady);
    }

    public void setUserId(String userId) {
        this.userId = userId;
        Platform.runLater(this::loadUserAvatar);
    }
}