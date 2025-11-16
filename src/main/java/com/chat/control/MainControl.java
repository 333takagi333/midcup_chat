package com.chat.control;

import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import com.chat.network.SocketClient;
import com.chat.protocol.*;
import com.chat.ui.DialogHelper;
import com.chat.ui.AvatarHelper;
import com.chat.ui.CellFactoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 主界面控制器 - 精简版
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

    // message handlers registry
    private final Map<String, java.util.function.Consumer<String>> messageHandlers = new HashMap<>();

    /**
     * 检查是否连接到服务器。
     */
    private boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
    }

    /**
     * 在UI线程上运行指定的Runnable，如果当前不在UI线程则使用Platform.runLater。
     */
    private void runOnUi(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    /**
     * 如果数据未加载且用户名和连接就绪，则尝试加载初始数据。
     */
    private void tryLoadInitialDataIfReady() {
        if (!dataLoaded && username != null && isConnected()) {
            loadInitialData();
        }
    }

    /**
     * 初始化控制器，设置UI、事件处理器和消息处理器。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
        registerMessageHandlers();
        // 延迟启动消息监听，等待窗口完全显示
        runOnUi(() -> {
            startMessageListener();
            tryLoadInitialDataIfReady();
        });
    }

    /**
     * 初始化UI组件，包括设置列表视图和清除数据。
     */
    private void setupUI() {
        try {
            setupListViews();
            clearAllData();
        } catch (Exception e) {
            System.err.println("[MainControl] UI设置失败: " + e.getMessage());
        }
    }

    /**
     * 设置消息列表、联系人列表和群组列表的单元格工厂。
     */
    private void setupListViews() {
        messagesListView.setCellFactory(CellFactoryHelper.chatCellFactory());
        contactsListView.setCellFactory(CellFactoryHelper.friendCellFactory());
        groupsListView.setCellFactory(CellFactoryHelper.groupCellFactory());
    }

    /**
     * 加载用户头像。
     */
    private void loadUserAvatar() {
        AvatarHelper.loadAvatar(avatarImage, null, false);
    }

    /**
     * 设置事件处理器，包括列表视图的双击事件和通知按钮。
     */
    private void setupEventHandlers() {
        messagesListView.setOnMouseClicked(event -> {
            ChatItem selected = messagesListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                if (selected.isGroup()) openGroupChat(selected);
                else openPrivateChat(selected);
            }
        });

        contactsListView.setOnMouseClicked(event -> {
            FriendItem selected = contactsListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) openPrivateChatFromContact(selected);
        });

        groupsListView.setOnMouseClicked(event -> {
            GroupItem selected = groupsListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) openGroupChatFromGroup(selected);
        });

        notificationButton.setOnAction(event -> showNotifications());
    }

    /**
     * 通用窗口打开器，用于减少重复代码，加载FXML并设置控制器。
     */
    private void openWindow(String fxmlPath, Consumer<Object> controllerSetup, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controllerSetup != null) controllerSetup.accept(controller);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
        } catch (IOException e) {
            System.err.println("打开窗口失败(" + fxmlPath + "): " + e.getMessage());
            DialogHelper.showError(mainContainer.getScene().getWindow(), "打开窗口失败: " + e.getMessage());
        }
    }

    /**
     * 从联系人列表打开私聊窗口，并更新聊天列表。
     */
    private void openPrivateChatFromContact(FriendItem friend) {
        openWindow("/com/chat/fxml/ChatPrivate.fxml", controller -> {
            ChatPrivateControl c = (ChatPrivateControl) controller;
            c.setChatInfo(friend.getUserId(), friend.getUsername(), friend.getAvatarUrl(), socketClient, userId);
        }, "与 " + friend.getUsername() + " 聊天", 600, 700);

        upsertChat(friend.getUserId(), friend.getUsername(), friend.getAvatarUrl(), "已打开聊天", false, false);
    }

    /**
     * 从群组列表打开群聊窗口，并更新聊天列表。
     */
    private void openGroupChatFromGroup(GroupItem group) {
        openWindow("/com/chat/fxml/ChatGroup.fxml", controller -> {
            ChatGroupControl c = (ChatGroupControl) controller;
            c.setGroupInfo(group.getGroupId(), group.getName(), group.getAvatarUrl(), socketClient, userId);
        }, "群聊: " + group.getName(), 600, 700);

        upsertChat(group.getGroupId(), group.getName(), group.getAvatarUrl(), "已打开聊天", false, true);
    }

    /**
     * 发送请求到服务器，并处理响应。
     */
    private void sendRequest(Object request, java.util.function.Consumer<String> onResponse) {
        if (isConnected()) {
            String response = socketClient.sendRequest(request);
            if (response != null && onResponse != null) Platform.runLater(() -> onResponse.accept(response));
        } else {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
        }
    }

    /**
     * 从服务器加载好友列表。
     */
    public void loadFriendsFromServer() {
        sendRequest(new FriendListRequest(), this::handleFriendListResponse);
    }

    /**
     * 从服务器加载群组列表。
     */
    public void loadGroupsFromServer() {
        sendRequest(new GroupListRequest(), this::handleGroupListResponse);
    }

    /**
     * 更新或插入聊天项到消息列表中。
     */
    private void upsertChat(String id, String name, String avatar, String lastMessage, boolean unread, boolean isGroup) {
        runOnUi(() -> {
            ObservableList<ChatItem> items = messagesListView.getItems();
            int foundIndex = -1;
            for (int i = 0; i < items.size(); i++) {
                ChatItem item = items.get(i);
                if (item.getId().equals(id) && item.isGroup() == isGroup) {
                    foundIndex = i;
                    break;
                }
            }

            String displayName = (name != null) ? name : (isGroup ? "群聊" + id : "用户" + id);
            String avatarUrl = avatar != null ? avatar : "";
            String time = getCurrentTime();
            String messageText = lastMessage != null ? lastMessage : "";

            ChatItem newItem = new ChatItem(id, displayName, messageText, time, avatarUrl, unread, isGroup);

            if (foundIndex >= 0) {
                // replace and move to top
                items.remove(foundIndex);
                items.add(0, newItem);
            } else {
                items.add(0, newItem);
            }

            messagesListView.refresh();
        });
    }

    /**
     * 获取当前时间的字符串表示，格式为HH:mm。
     */
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 打开私聊窗口。
     */
    private void openPrivateChat(ChatItem chat) {
        openWindow("/com/chat/fxml/ChatPrivate.fxml", controller -> {
            ChatPrivateControl c = (ChatPrivateControl) controller;
            c.setChatInfo(chat.getId(), chat.getName(), chat.getAvatarUrl(), socketClient, userId);
        }, "与 " + chat.getName() + " 聊天", 600, 700);
    }

    /**
     * 打开群聊窗口。
     */
    private void openGroupChat(ChatItem chat) {
        openWindow("/com/chat/fxml/ChatGroup.fxml", controller -> {
            ChatGroupControl c = (ChatGroupControl) controller;
            c.setGroupInfo(chat.getId(), chat.getName(), chat.getAvatarUrl(), socketClient, userId);
        }, "群聊: " + chat.getName(), 600, 700);
    }

    /**
     * 显示通知对话框。
     */
    @FXML
    private void showNotifications() {
        DialogHelper.showInfo(mainContainer.getScene().getWindow(), "暂无新通知");
    }

    /**
     * 显示用户资料窗口。
     */
    @FXML
    private void showUserProfile() {
        openWindow("/com/chat/fxml/UserProfile.fxml", controller -> {
            UserProfileControl c = (UserProfileControl) controller;
            c.setUserInfo(username, userId, socketClient);
        }, "个人资料", 400, 500);
    }

    /**
     * 显示设置对话框。
     */
    @FXML
    private void showSettings() {
        DialogHelper.showInfo(mainContainer.getScene().getWindow(), "设置功能开发中...");
    }

    /**
     * 退出登录，停止消息监听并关闭窗口。
     */
    @FXML
    private void logout() {
        if (DialogHelper.showConfirmation(mainContainer.getScene() != null ? mainContainer.getScene().getWindow() : null, "确认退出登录？")) {
            stopMessageListener();
            if (socketClient != null) socketClient.disconnect();
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * 启动消息监听器，定期检查服务器消息。
     */
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

    /**
     * 停止消息监听器。
     */
    private void stopMessageListener() {
        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer = null;
        }
    }

    /**
     * 注册消息处理器，将消息类型映射到处理方法。
     */
    private void registerMessageHandlers() {
        messageHandlers.put("friend_list_response", this::handleFriendListResponse);
        messageHandlers.put("group_list_response", this::handleGroupListResponse);
        messageHandlers.put("user_profile_response", this::handleUserProfileResponse);
        messageHandlers.put("chat_private_receive", this::handlePrivateMessage);
        messageHandlers.put("chat_group_receive", this::handleGroupMessage);
        messageHandlers.put("friend_add_response", this::handleFriendAddResponse);
    }

    /**
     * 处理从服务器接收到的消息，根据类型调用相应的处理器。
     */
    private void handleServerMessage(String message) {
        try {
            JsonObject obj = gson.fromJson(message, JsonObject.class);
            if (obj != null && obj.has("type")) {
                String type = obj.get("type").getAsString();
                java.util.function.Consumer<String> handler = messageHandlers.get(type);
                if (handler != null) runOnUi(() -> handler.accept(message));
            }
        } catch (Exception e) {
            System.err.println("处理服务器消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理好友列表响应，更新好友列表。
     */
    private void handleFriendListResponse(String response) {
        try {
            FriendListResponse friendResponse = gson.fromJson(response, FriendListResponse.class);
            if (friendResponse != null && friendResponse.getFriends() != null) updateFriendList(friendResponse.getFriends());
        } catch (Exception e) {
            System.err.println("解析好友列表响应失败: " + e.getMessage());
        }
    }

    /**
     * 处理群组列表响应，更新群组列表。
     */
    private void handleGroupListResponse(String response) {
        try {
            GroupListResponse groupResponse = gson.fromJson(response, GroupListResponse.class);
            if (groupResponse != null && groupResponse.getGroups() != null) updateGroupList(groupResponse.getGroups());
        } catch (Exception e) {
            System.err.println("解析群组列表响应失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户资料响应，目前为空操作，可用于未来解析头像。
     */
    private void handleUserProfileResponse(String response) {
        try {
            // currently no-op; can parse avatar in future and call loadUserAvatar/loadRemoteAvatar
        } catch (Exception e) {
            System.err.println("解析用户资料响应失败: " + e.getMessage());
        }
    }

    /**
     * 处理私聊消息，更新聊天列表。
     */
    private void handlePrivateMessage(String messageJson) {
        try {
            ChatPrivateReceive message = gson.fromJson(messageJson, ChatPrivateReceive.class);
            if (message != null) upsertChat(message.getFromUserId().toString(), null, null, message.getContent(), true, false);
        } catch (Exception e) {
            System.err.println("解析私聊消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理群聊消息，更新聊天列表。
     */
    private void handleGroupMessage(String messageJson) {
        try {
            ChatGroupReceive message = gson.fromJson(messageJson, ChatGroupReceive.class);
            if (message != null) upsertChat(message.getGroupId().toString(), null, null, message.getContent(), true, true);
        } catch (Exception e) {
            System.err.println("解析群聊消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理好友添加响应，显示结果并重新加载好友列表。
     */
    private void handleFriendAddResponse(String messageJson) {
        try {
            FriendAddResponse response = gson.fromJson(messageJson, FriendAddResponse.class);
            if (response != null && response.isSuccess()) {
                DialogHelper.showInfo(mainContainer.getScene().getWindow(), "好友添加成功");
                loadFriendsFromServer();
            } else {
                DialogHelper.showError(mainContainer.getScene().getWindow(), response != null ? response.getMessage() : "好友添加失败");
            }
        } catch (Exception e) {
            System.err.println("解析好友添加响应失败: " + e.getMessage());
        }
    }

    /**
     * 更新好友列表视图。
     */
    private void updateFriendList(java.util.List<FriendListResponse.FriendItem> friends) {
        ObservableList<FriendItem> items = FXCollections.observableArrayList();
        if (friends != null) {
            for (FriendListResponse.FriendItem friend : friends) {
                FriendItem item = new FriendItem(
                        friend.getUid() != null ? friend.getUid().toString() : "0",
                        friend.getUsername() != null ? friend.getUsername() : "未知用户",
                        "在线",
                        friend.getAvatarUrl(),
                        ""
                );
                items.add(item);
            }
        }
        contactsListView.setItems(items);
    }

    /**
     * 更新群组列表视图。
     */
    private void updateGroupList(java.util.List<GroupListResponse.GroupItem> groups) {
        ObservableList<GroupItem> items = FXCollections.observableArrayList();
        if (groups != null) {
            for (GroupListResponse.GroupItem group : groups) {
                GroupItem item = new GroupItem(
                        group.getId() != null ? group.getId().toString() : "0",
                        group.getName() != null ? group.getName() : "未知群组",
                        "暂无消息",
                        "0",
                        group.getAvatar()
                );
                items.add(item);
            }
        }
        groupsListView.setItems(items);
    }

    /**
     * 设置Socket客户端，并尝试加载用户头像和初始数据。
     */
    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        if (this.userId != null && !this.userId.isEmpty() && isConnected()) {
            loadUserAvatar();
        }
        runOnUi(this::tryLoadInitialDataIfReady);
    }

    /**
     * 设置用户名，并更新UI标签。
     */
    public void setUsername(String username) {
        this.username = username;
        if (usernameLabel != null) usernameLabel.setText(username);
        runOnUi(this::tryLoadInitialDataIfReady);
    }

    /**
     * 设置用户ID，如果连接则加载头像。
     */
    public void setUserId(String userId) {
        if (userId == null) return;
        if (!userId.equals(this.userId)) {
            this.userId = userId;
            if (isConnected()) {
                loadUserAvatar();
            }
        }
    }

    /**
     * 清除所有数据列表。
     */
    private void clearAllData() {
        messagesListView.setItems(FXCollections.observableArrayList());
        contactsListView.setItems(FXCollections.observableArrayList());
        groupsListView.setItems(FXCollections.observableArrayList());
    }

    /**
     * 加载初始数据，包括好友和群组列表。
     */
    public void loadInitialData() {
        if (dataLoaded) return;
        clearAllData();
        if (isConnected()) {
            loadFriendsFromServer();
            loadGroupsFromServer();
            dataLoaded = true;
        } else {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器，无法加载数据");
        }
    }

}
