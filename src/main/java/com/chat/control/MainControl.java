package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import com.chat.service.*;
import  com.chat.protocol.*;
import com.chat.ui.DialogHelper;
import com.chat.ui.AvatarHelper;
import com.chat.ui.CellFactoryHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

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
    private boolean dataLoaded = false;
    private int notificationCount = 0;

    // Service层引用
    private final ChatStateService stateService = new ChatStateService();
    private final MainDataService mainDataService = new MainDataService();
    private final NotificationManagementService notificationService = new NotificationManagementService();
    private final WindowManagementService windowService = new WindowManagementService();
    private final ChatService chatService = new ChatService();
    private UserProfileService userProfileService;

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

        Platform.runLater(() -> {
            // 设置当前用户ID到MessageBroadcaster
            if (userId != null && !userId.isEmpty()) {
                try {
                    Long userIdLong = Long.parseLong(userId);
                    MessageBroadcaster.getInstance().setCurrentUserId(userIdLong);
                    System.out.println("[MainControl] 已设置当前用户ID到MessageBroadcaster: " + userIdLong);
                } catch (NumberFormatException e) {
                    System.err.println("用户ID格式错误: " + e.getMessage());
                }
            }

            // 先设置默认头像
            AvatarHelper.setDefaultAvatar(avatarImage, false, 40);

            // 设置窗口关闭事件处理器
            setupWindowCloseHandler();

            // 延迟一会儿再尝试从服务器加载
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        startMessageListener();
                        tryLoadInitialDataIfReady();

                        // 尝试加载头像
                        if (userId != null && !userId.isEmpty()) {
                            loadUserAvatarWithRetry();
                        }
                    });
                }
            }, 500); // 延迟500ms
        });
    }

    /**
     * 设置窗口关闭事件处理器
     */
    private void setupWindowCloseHandler() {
        if (mainContainer != null && mainContainer.getScene() != null && mainContainer.getScene().getWindow() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) mainContainer.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("[MainControl] 检测到窗口关闭请求，执行清理...");

                // 1. 先执行清理操作
                cleanup();

                // 2. 强制退出程序
                System.out.println("[MainControl] 强制退出程序...");

                // 先关闭JavaFX平台
                Platform.exit();

                // 再退出JVM
                System.exit(0);
            });
        }
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
                        // 更新ObservableList
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

    // ========== 消息监听 ==========
    private void startMessageListener() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }

        messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected()) {
                    try {
                        String message = socketClient.receiveMessage();
                        if (message != null && !message.trim().isEmpty()) {
                            // 使用ChatService处理并广播消息
                            chatService.processMessage(message);
                        }
                    } catch (Exception e) {
                        System.err.println("消息监听错误: " + e.getMessage());
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
                    mainDataService.updateFriendList(stateService.getFriendItems(), result.getFriends());
                    mainDataService.updateGroupList(stateService.getGroupItems(), result.getGroups());
                    dataLoaded = true;
                });
            }
        }).start();
    }

    // ========== 头像加载方法 ==========
    // 添加重试计数
    private int avatarLoadRetryCount = 0;
    private static final int MAX_AVATAR_RETRY = 3;

    /**
     * 带重试机制的头像加载
     */
    private void loadUserAvatarWithRetry() {
        if (avatarImage == null || userId == null || userId.isEmpty()) {
            return;
        }

        if (!isConnected()) {
            AvatarHelper.setDefaultAvatar(avatarImage, false, 40);
            return;
        }

        // 重置重试计数
        avatarLoadRetryCount = 0;

        // 开始加载
        loadUserAvatarRetryImpl();
    }

    private void loadUserAvatarRetryImpl() {
        new Thread(() -> {
            try {
                avatarLoadRetryCount++;
                System.out.println("[MainControl] 头像加载尝试 " + avatarLoadRetryCount + "/" + MAX_AVATAR_RETRY);

                if (userProfileService == null) {
                    userProfileService = new UserProfileService(socketClient);
                }

                Long userIdLong = Long.parseLong(userId);
                UserInfoResponse userInfo = userProfileService.loadUserInfo(userIdLong);

                Platform.runLater(() -> {
                    if (userInfo != null && userInfo.isSuccess() &&
                            userInfo.getAvatarUrl() != null && !userInfo.getAvatarUrl().trim().isEmpty()) {

                        // 成功获取头像
                        String avatarUrl = userInfo.getAvatarUrl();
                        System.out.println("[MainControl] 头像加载成功: " + avatarUrl);
                        AvatarHelper.loadAvatar(avatarImage, avatarUrl, false, 40);

                    } else if (avatarLoadRetryCount < MAX_AVATAR_RETRY) {
                        // 失败但还可以重试
                        System.err.println("[MainControl] 头像加载失败，准备重试...");

                        // 延迟后重试
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                loadUserAvatarRetryImpl();
                            }
                        }, 1000 * avatarLoadRetryCount); // 延迟时间递增

                    } else {
                        // 达到最大重试次数，使用默认头像
                        System.err.println("[MainControl] 头像加载失败，已达到最大重试次数");
                        AvatarHelper.setDefaultAvatar(avatarImage, false, 40);
                    }
                });

            } catch (Exception e) {
                System.err.println("[MainControl] 头像加载异常: " + e.getMessage());

                Platform.runLater(() -> {
                    if (avatarLoadRetryCount < MAX_AVATAR_RETRY) {
                        // 延迟后重试
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                loadUserAvatarRetryImpl();
                            }
                        }, 1000 * avatarLoadRetryCount);
                    } else {
                        AvatarHelper.setDefaultAvatar(avatarImage, false, 40);
                    }
                });
            }
        }).start();
    }

    // ========== 工具方法 ==========
    private boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
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
                        cleanup();

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

    // ========== 清理方法 ==========
    public void cleanup() {
        System.out.println("[MainControl] 开始清理所有数据...");

        stopMessageListener();

        if (socketClient != null) {
            socketClient.disconnect();
        }

        windowService.closeAllWindows();

        // 清理聊天会话管理器（退出登录时清空所有记录）
        ChatSessionManager.getInstance().clearAllSessions();

        // 清理状态服务
        stateService.cleanup();

        // 清理消息广播器
        MessageBroadcaster.getInstance().cleanup();

        // 重置数据加载标志
        dataLoaded = false;

        System.out.println("[MainControl] 清理完成，所有聊天记录已清空");
    }

    // ========== Setter 方法 ==========
    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        if (userId != null && isConnected()) {
            AvatarHelper.setDefaultAvatar(avatarImage, false, 40);
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

        // 设置当前用户ID到MessageBroadcaster
        if (userId != null && !userId.isEmpty()) {
            try {
                Long userIdLong = Long.parseLong(userId);
                MessageBroadcaster.getInstance().setCurrentUserId(userIdLong);
                System.out.println("[MainControl] 已设置当前用户ID: " + userIdLong);
            } catch (NumberFormatException e) {
                System.err.println("用户ID格式错误: " + e.getMessage());
            }
        }

        Platform.runLater(() -> {
            // 立即显示默认头像
            AvatarHelper.setDefaultAvatar(avatarImage, false, 40);

            // 异步加载真实头像
            if (isConnected()) {
                loadUserAvatarWithRetry();
            }
        });
    }
}