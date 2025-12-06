package com.chat.control;

import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.service.ChatStateService;
import com.chat.service.FriendService;
import com.chat.service.GroupService;
import com.chat.ui.DialogHelper;
import com.chat.ui.AvatarHelper;
import com.chat.ui.CellFactoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 主界面控制器
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
    @FXML private Button refreshButton; // 刷新按钮

    private String username;
    private String userId;
    private SocketClient socketClient;
    private Timer messageTimer;
    private final Gson gson = new Gson();
    private boolean dataLoaded = false;

    // 通知管理
    private int notificationCount = 0;

    // Service 层引用
    private final ChatStateService stateService = new ChatStateService();
    private final FriendService friendService = new FriendService();
    private final GroupService groupService = new GroupService();

    private final Map<String, Consumer<String>> messageHandlers = new HashMap<>();

    // 窗口管理
    private final Map<String, Stage> activeWindows = new HashMap<>();

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

        // 初始化通知按钮
        updateNotificationButton();

        // 设置刷新按钮样式
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
        // 双击打开聊天
        messagesListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ChatItem selected = messagesListView.getSelectionModel().getSelectedItem();
                if (selected != null) openChatWindow(selected);
            }
        });

        // 双击联系人打开私聊
        contactsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FriendItem friend = contactsListView.getSelectionModel().getSelectedItem();
                if (friend != null) openPrivateChat(friend);
            }
        });

        // 双击群组打开群聊
        groupsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                GroupItem group = groupsListView.getSelectionModel().getSelectedItem();
                if (group != null) openGroupChat(group);
            }
        });

        if (notificationButton != null) {
            notificationButton.setOnAction(event -> showNotifications());
        }

        // 刷新按钮事件处理
        if (refreshButton != null) {
            refreshButton.setOnAction(event -> handleRefreshClick());
        }
    }
    // ========== 刷新功能 ==========

    /**
     * 刷新按钮点击处理
     */
    @FXML
    private void handleRefreshClick() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        System.out.println("[MainControl] 用户点击刷新按钮");

        // 显示刷新状态
        showRefreshStatus("刷新");

        // 刷新所有数据
        refreshAllDataWithFeedback();
    }

    /**
     * 带反馈的刷新所有数据
     */
    private void refreshAllDataWithFeedback() {
        Platform.runLater(() -> {
            try {
                // 保存刷新前的状态
                int oldFriendCount = stateService.getFriendItems().size();
                int oldGroupCount = stateService.getGroupItems().size();

                System.out.println("[MainControl] 刷新前好友数: " + oldFriendCount + ", 群组数: " + oldGroupCount);

                // 刷新好友列表
                stateService.loadFriendsFromServer(socketClient, friendService);

                // 刷新群组列表
                stateService.loadGroupsFromServer(socketClient, groupService);

                // 获取刷新后的状态
                int newFriendCount = stateService.getFriendItems().size();
                int newGroupCount = stateService.getGroupItems().size();

                System.out.println("[MainControl] 刷新后好友数: " + newFriendCount + ", 群组数: " + newGroupCount);

                // 计算变化
                int friendDelta = newFriendCount - oldFriendCount;
                int groupDelta = newGroupCount - oldGroupCount;

                // 强制刷新UI显示
                contactsListView.refresh();
                groupsListView.refresh();

                // 显示成功提示
                showRefreshStatus("刷新完成!");

            } catch (Exception e) {
                System.err.println("[MainControl] 刷新数据失败: " + e.getMessage());
                showRefreshStatus("刷新失败");
                DialogHelper.showError(mainContainer.getScene().getWindow(), "刷新失败: " + e.getMessage());
            }
        });
    }

    /**
     * 显示刷新状态
     */
    private void showRefreshStatus(String status) {
        if (refreshButton != null) {
            String originalText = refreshButton.getText();
            refreshButton.setText(status);
            refreshButton.setDisable(true);

            // 1.5秒后恢复按钮状态
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
    /**
     * 刷新好友列表（供外部调用）
     */
    public void refreshFriends() {
        if (!isConnected()) {
            System.out.println("[MainControl] 无法刷新好友列表：未连接到服务器");
            return;
        }

        Platform.runLater(() -> {
            try {
                System.out.println("[MainControl] 刷新好友列表...");
                stateService.loadFriendsFromServer(socketClient, friendService);

                // 强制刷新UI
                contactsListView.refresh();

                System.out.println("[MainControl] 好友列表刷新完成");

            } catch (Exception e) {
                System.err.println("[MainControl] 刷新好友列表失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 刷新群组列表（供外部调用）
     */
    public void refreshGroups() {
        if (!isConnected()) {
            System.out.println("[MainControl] 无法刷新群组列表：未连接到服务器");
            return;
        }

        Platform.runLater(() -> {
            try {
                System.out.println("[MainControl] 刷新群组列表...");
                stateService.loadGroupsFromServer(socketClient, groupService);

                // 强制刷新UI
                groupsListView.refresh();

                System.out.println("[MainControl] 群组列表刷新完成");

            } catch (Exception e) {
                System.err.println("[MainControl] 刷新群组列表失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ========== 通知管理方法 ==========

    private void updateNotificationButton() {
        Platform.runLater(() -> {
            if (notificationCount > 0) {
                notificationButton.setText("通知(" + notificationCount + ")");
                notificationButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
            } else {
                notificationButton.setText("通知");
                notificationButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            }
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
        String windowKey = getChatWindowKey(chat);
        if (isWindowAlreadyOpen(windowKey)) {
            bringWindowToFront(windowKey);
            return;
        }

        String fxmlPath = chat.isGroup() ? "/com/chat/fxml/ChatGroup.fxml" : "/com/chat/fxml/ChatPrivate.fxml";
        String title = chat.isGroup() ? "群聊: " + chat.getName() : "与 " + chat.getName() + " 聊天";

        openManagedWindow(fxmlPath, controller -> setupChatController(controller, chat), title, 600, 700, windowKey);
    }

    private void openPrivateChat(FriendItem friend) {
        String windowKey = "private_" + friend.getUserId();
        if (isWindowAlreadyOpen(windowKey)) {
            bringWindowToFront(windowKey);
            return;
        }

        openManagedWindow("/com/chat/fxml/ChatPrivate.fxml",
                controller -> setupPrivateController(controller, friend),
                "与 " + friend.getUsername() + " 聊天", 600, 700, windowKey);

        stateService.upsertChat(friend.getUserId(), friend.getUsername(),
                friend.getAvatarUrl(), "已打开聊天", false, false);
    }

    private void openGroupChat(GroupItem group) {
        String windowKey = "group_" + group.getGroupId();
        if (isWindowAlreadyOpen(windowKey)) {
            bringWindowToFront(windowKey);
            return;
        }

        openManagedWindow("/com/chat/fxml/ChatGroup.fxml",
                controller -> setupGroupController(controller, group),
                "群聊: " + group.getName(), 600, 700, windowKey);

        stateService.upsertChat(group.getGroupId(), group.getName(),
                group.getAvatarUrl(), "已打开聊天", false, true);
    }

    private String getChatWindowKey(ChatItem chat) {
        return chat.isGroup() ? "group_" + chat.getId() : "private_" + chat.getId();
    }

    private boolean isWindowAlreadyOpen(String windowKey) {
        if (windowKey == null) return false;
        Stage stage = activeWindows.get(windowKey);
        return stage != null && stage.isShowing();
    }

    private void bringWindowToFront(String windowKey) {
        Stage stage = activeWindows.get(windowKey);
        if (stage != null) {
            stage.toFront();
            stage.requestFocus();
            if (stage.isIconified()) {
                stage.setIconified(false);
            }
        }
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

    private void openManagedWindow(String fxmlPath, Consumer<Object> controllerSetup,
                                   String title, int width, int height, String windowKey) {
        if (isWindowAlreadyOpen(windowKey)) {
            bringWindowToFront(windowKey);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            controllerSetup.accept(loader.getController());

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));

            // 保存加载器引用以便获取控制器
            stage.setUserData(loader);

            stage.setOnCloseRequest(event -> {
                activeWindows.remove(windowKey);
                // 如果是通知窗口关闭，清空通知计数
                if (NOTIFICATION_WINDOW_KEY.equals(windowKey)) {
                    clearNotificationCount();
                }
            });

            stage.show();
            activeWindows.put(windowKey, stage);

        } catch (IOException e) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "打开窗口失败: " + e.getMessage());
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
        stateService.loadFriendsFromServer(socketClient, friendService);
        stateService.loadGroupsFromServer(socketClient, groupService);
        dataLoaded = true;
    }

    // ========== 消息处理 ==========
    private void registerMessageHandlers() {
        messageHandlers.put(MessageType.FRIEND_LIST_RESPONSE, this::handleFriendListResponse);
        messageHandlers.put(MessageType.GROUP_LIST_RESPONSE, this::handleGroupListResponse);
        messageHandlers.put(MessageType.CHAT_PRIVATE_RECEIVE, this::handlePrivateMessage);
        messageHandlers.put(MessageType.CHAT_GROUP_RECEIVE, this::handleGroupMessage);
        messageHandlers.put(MessageType.FRIEND_ADD_RESPONSE, this::handleFriendAddResponse);
        messageHandlers.put(MessageType.GROUP_CREATE_RESPONSE, this::handleGroupCreateResponse);
        messageHandlers.put(MessageType.FRIEND_REQUEST_RECEIVE, this::handleFriendRequestReceive); // 添加好友请求接收
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
            stateService.loadFriendsFromServer(socketClient, friendService);
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
            stateService.loadGroupsFromServer(socketClient, groupService);
        } else {
            String errorMsg = response != null ? response.getMessage() : "未知错误";
            DialogHelper.showError(mainContainer.getScene().getWindow(), "群聊创建失败: " + errorMsg);
        }
    }

    // ========== 处理好友请求通知 ==========
    private void handleFriendRequestReceive(String messageJson) {
        try {
            JsonObject obj = gson.fromJson(messageJson, JsonObject.class);
            Long requestId = obj.get("requestId").getAsLong();
            Long fromUserId = obj.get("fromUserId").getAsLong();
            String fromUsername = obj.get("fromUsername").getAsString();
            String requestTime = obj.get("requestTime").getAsString();

            Platform.runLater(() -> {
                // 增加通知计数
                addNotification();

                // 如果通知窗口已打开，直接添加通知
                if (isWindowAlreadyOpen(NOTIFICATION_WINDOW_KEY)) {
                    NotificationControl control = getNotificationController();
                    if (control != null) {
                        // 传递参数而不是对象
                        control.addNotificationFromMainControl(
                                requestId,
                                fromUserId,
                                fromUsername,
                                requestTime
                        );
                    }
                }

                // 显示桌面通知
                showDesktopNotification("新好友请求",
                        "用户 " + fromUsername + " 请求添加您为好友");
            });
        } catch (Exception e) {
            System.err.println("处理好友请求失败: " + e.getMessage());
        }
    }

    private void showDesktopNotification(String title, String message) {
        Platform.runLater(() -> {
            try {
                Alert notification = new Alert(Alert.AlertType.INFORMATION);
                notification.setTitle(title);
                notification.setHeaderText(null);
                notification.setContentText(message);
                notification.initOwner(mainContainer.getScene().getWindow());

                notification.show();

                // 自动关闭通知
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    Platform.runLater(() -> {
                        if (notification.isShowing()) {
                            notification.close();
                        }
                    });
                }).start();

            } catch (Exception e) {
                System.err.println("显示桌面通知失败: " + e.getMessage());
            }
        });
    }

    private NotificationControl getNotificationController() {
        try {
            Stage stage = activeWindows.get(NOTIFICATION_WINDOW_KEY);
            if (stage != null) {
                FXMLLoader loader = (FXMLLoader) stage.getUserData();
                if (loader != null) {
                    return loader.getController();
                }
            }
        } catch (Exception e) {
            System.err.println("获取通知控制器失败: " + e.getMessage());
        }
        return null;
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
        closeAllWindows();
    }

    private void closeAllWindows() {
        for (Stage stage : activeWindows.values()) {
            if (stage != null && stage.isShowing()) {
                stage.close();
            }
        }
        activeWindows.clear();
    }

    private void clearAllData() {
        stateService.clearAll();
    }

    private void performLogout() {
        if (DialogHelper.showConfirmation(mainContainer.getScene().getWindow(),
                "确定要退出登录吗？退出后需要重新登录。")) {

            executeLogout();
        }
    }

    /**
     * 执行退出登录的统一逻辑
     */
    private void executeLogout() {
        System.out.println("执行退出登录...");

        // 1. 停止消息监听器
        stopMessageListener();

        // 2. 注意：由于协议中没有 logout_request，我们不发送退出请求
        // 直接断开连接即可
        System.out.println("协议中没有 logout_request 消息类型，直接断开连接");

        // 3. 断开Socket连接
        if (socketClient != null) {
            socketClient.disconnect();
        }

        // 4. 关闭所有窗口并显示登录界面
        Platform.runLater(() -> {
            // 关闭所有打开的窗口
            closeAllWindows();

            // 关闭主窗口
            Stage mainStage = (Stage) mainContainer.getScene().getWindow();
            if (mainStage != null && mainStage.isShowing()) {
                mainStage.close();
            }

            // 显示登录界面
            showLoginScreen();
        });
    }

    // ========== UI 操作方法 ==========
    @FXML
    private void showNotifications() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        // 打开通知中心窗口
        openManagedWindow("/com/chat/fxml/Notifications.fxml",
                controller -> {
                    NotificationControl notificationControl = (NotificationControl) controller;
                    notificationControl.setSocketClient(socketClient);
                    notificationControl.setUserId(userId);
                },
                "通知中心" + (notificationCount > 0 ? " (" + notificationCount + ")" : ""),
                650, 550, NOTIFICATION_WINDOW_KEY);

        // 打开通知窗口后清空通知计数
        clearNotificationCount();
    }

    @FXML
    private void showUserProfile() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        openManagedWindow("/com/chat/fxml/UserProfile.fxml",
                controller -> ((UserProfileControl) controller).setUserInfo(username, userId, socketClient),
                "个人资料", 400, 500, USER_PROFILE_WINDOW_KEY);
    }

    @FXML
    private void showAddFriendDialog() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        openManagedWindow("/com/chat/fxml/AddFriend.fxml",
                controller -> ((AddFriendControl) controller).setSocketClient(socketClient),
                "添加好友", 400, 250, ADD_FRIEND_WINDOW_KEY);
    }

    @FXML
    private void showCreateGroupDialog() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        openManagedWindow("/com/chat/fxml/CreateGroup.fxml",
                controller -> ((CreateGroupControl) controller).setSocketClient(socketClient),
                "创建群聊", 400, 250, CREATE_GROUP_WINDOW_KEY);
    }

    @FXML
    private void showSettings() {
        if (!isConnected()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        // 打开设置窗口
        openManagedWindow("/com/chat/fxml/Settings.fxml",
                controller -> {
                    SettingControl settingControl = (SettingControl) controller;
                    settingControl.setUserInfo(username, userId);
                    settingControl.setSocketClient(socketClient);

                    // 设置退出回调：关闭所有窗口并返回登录界面
                    settingControl.setLogoutCallback(() -> {
                        System.out.println("设置界面触发退出，关闭所有窗口...");

                        // 1. 停止消息监听
                        stopMessageListener();

                        // 2. 断开Socket连接
                        if (socketClient != null) {
                            socketClient.disconnect();
                        }

                        // 3. 关闭所有打开的窗口（包括主窗口）
                        Platform.runLater(() -> {
                            // 关闭主窗口
                            Stage mainStage = (Stage) mainContainer.getScene().getWindow();
                            if (mainStage != null && mainStage.isShowing()) {
                                mainStage.close();
                            }

                            // 关闭所有其他窗口
                            closeAllWindows();

                            // 4. 显示登录界面
                            showLoginScreen();
                        });
                    });

                    // 设置密码重置回调：在密码重置成功后关闭主窗口
                    settingControl.setPasswordResetCallback((success) -> {
                        if (success) {
                            System.out.println("收到密码重置成功通知，准备关闭主窗口...");
                            // 这里不需要立即关闭窗口，因为logoutCallback会处理所有关闭逻辑
                            // 只是提供一个信号给设置界面
                        }
                    });
                },
                "设置 - " + username, 500, 450, SETTINGS_WINDOW_KEY);
    }

    /**
     * 显示登录界面
     */
    private void showLoginScreen() {
        try {
            System.out.println("显示登录界面...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/fxml/Login.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
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
            Long userIdLong = Long.parseLong(userId);
            UserInfoRequest request = new UserInfoRequest(userIdLong);

            String response = socketClient.sendRequest(request);
            if (response != null) {
                UserInfoResponse userInfo = gson.fromJson(response, UserInfoResponse.class);
                if (userInfo != null && userInfo.getAvatarUrl() != null) {
                    AvatarHelper.loadAvatar(avatarImage, userInfo.getAvatarUrl(), false, 40);
                    return;
                }
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