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
 * 主界面控制器 - 添加窗口管理功能
 */
public class MainControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label usernameLabel;
    @FXML private Button notificationButton;
    @FXML private ListView<ChatItem> messagesListView;
    @FXML private ListView<FriendItem> contactsListView;
    @FXML private ListView<GroupItem> groupsListView;

    private String username;
    private String userId;
    private SocketClient socketClient;
    private Timer messageTimer;
    private final Gson gson = new Gson();
    private boolean dataLoaded = false;

    // Service 层引用
    private final ChatStateService stateService = new ChatStateService();
    private final FriendService friendService = new FriendService();
    private final GroupService groupService = new GroupService();

    private final Map<String, Consumer<String>> messageHandlers = new HashMap<>();

    // 新增：窗口管理
    private final Map<String, Stage> activeWindows = new HashMap<>();

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

        notificationButton.setOnAction(event -> showNotifications());
    }

    // ========== 改进的窗口管理 ==========
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
        stateService.upsertChat(friend.getUserId(), friend.getUsername(), friend.getAvatarUrl(), "已打开聊天", false, false);
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
        stateService.upsertChat(group.getGroupId(), group.getName(), group.getAvatarUrl(), "已打开聊天", false, true);
    }

    private String getChatWindowKey(ChatItem chat) {
        return chat.isGroup() ? "group_" + chat.getId() : "private_" + chat.getId();
    }

    private boolean isWindowAlreadyOpen(String windowKey) {
        return activeWindows.containsKey(windowKey);
    }

    private void bringWindowToFront(String windowKey) {
        Stage stage = activeWindows.get(windowKey);
        if (stage != null) {
            stage.toFront();
            stage.requestFocus();
        }
    }

    private void setupChatController(Object controller, ChatItem chat) {
        if (controller instanceof ChatPrivateControl) {
            ((ChatPrivateControl) controller).setChatInfo(chat.getId(), chat.getName(), chat.getAvatarUrl(), socketClient, userId);
        } else if (controller instanceof ChatGroupControl) {
            ((ChatGroupControl) controller).setGroupInfo(chat.getId(), chat.getName(), chat.getAvatarUrl(), socketClient, userId);
        }
    }

    private void setupPrivateController(Object controller, FriendItem friend) {
        if (controller instanceof ChatPrivateControl) {
            ((ChatPrivateControl) controller).setChatInfo(friend.getUserId(), friend.getUsername(), friend.getAvatarUrl(), socketClient, userId);
        }
    }

    private void setupGroupController(Object controller, GroupItem group) {
        if (controller instanceof ChatGroupControl) {
            ((ChatGroupControl) controller).setGroupInfo(group.getGroupId(), group.getName(), group.getAvatarUrl(), socketClient, userId);
        }
    }

    private void openManagedWindow(String fxmlPath, Consumer<Object> controllerSetup, String title, int width, int height, String windowKey) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            controllerSetup.accept(loader.getController());

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));

            // 设置窗口关闭事件处理
            stage.setOnCloseRequest(event -> {
                activeWindows.remove(windowKey);
                System.out.println("[MainControl] 关闭窗口: " + windowKey);
            });

            stage.show();

            // 保存窗口引用
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
        messageHandlers.put("friend_list_response", this::handleFriendListResponse);
        messageHandlers.put("group_list_response", this::handleGroupListResponse);
        messageHandlers.put("chat_private_receive", this::handlePrivateMessage);
        messageHandlers.put("chat_group_receive", this::handleGroupMessage);
        messageHandlers.put("friend_add_response", this::handleFriendAddResponse);
    }

    private void handleServerMessage(String message) {
        try {
            JsonObject obj = gson.fromJson(message, JsonObject.class);
            String type = obj.get("type").getAsString();
            Consumer<String> handler = messageHandlers.get(type);
            if (handler != null) handler.accept(message);
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
            DialogHelper.showError(mainContainer.getScene().getWindow(), "好友添加失败");
        }
    }

    // ========== 工具方法 ==========
    private boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
    }

    private void startMessageListener() {
        messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected()) {
                    String message = socketClient.receiveMessage();
                    if (message != null) Platform.runLater(() -> handleServerMessage(message));
                }
            }
        }, 0, 100);
    }

    private void stopMessageListener() {
        if (messageTimer != null) {
            messageTimer.cancel();
        }
        // 关闭所有窗口
        closeAllWindows();
    }

    private void closeAllWindows() {
        for (Stage stage : activeWindows.values()) {
            if (stage != null) {
                stage.close();
            }
        }
        activeWindows.clear();
    }

    private void clearAllData() {
        stateService.clearAll();
    }

    // ========== UI 操作 ==========
    @FXML private void showNotifications() {
        DialogHelper.showInfo(mainContainer.getScene().getWindow(), "暂无新通知");
    }

    @FXML private void showUserProfile() {
        // 个人资料窗口可以重复打开
        openManagedWindow("/com/chat/fxml/UserProfile.fxml",
                controller -> ((UserProfileControl) controller).setUserInfo(username, userId, socketClient),
                "个人资料", 400, 500, "user_profile_" + System.currentTimeMillis());
    }

    @FXML private void showSettings() {
        DialogHelper.showInfo(mainContainer.getScene().getWindow(), "设置功能开发中...");
    }

    @FXML private void logout() {
        if (DialogHelper.showConfirmation(mainContainer.getScene().getWindow(), "确认退出登录？")) {
            stopMessageListener();
            closeAllWindows();
            if (socketClient != null) socketClient.disconnect();
            ((Stage) mainContainer.getScene().getWindow()).close();
        }
    }

    // ========== 头像加载方法 - 使用AvatarHelper ==========
    private void loadUserAvatar() {
        if (avatarImage == null) {
            System.err.println("avatarImage is null, cannot load avatar");
            return;
        }

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
        } catch (NumberFormatException e) {
            System.err.println("用户ID格式错误: " + userId);
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
        if (usernameLabel != null) usernameLabel.setText(username);
        Platform.runLater(this::tryLoadInitialDataIfReady);
    }

    public void setUserId(String userId) {
        this.userId = userId;
        Platform.runLater(this::loadUserAvatar);
    }
}