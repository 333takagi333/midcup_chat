package com.chat.control;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import com.chat.network.SocketClient;
import com.chat.ui.DialogHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URL;
import java.util.ResourceBundle;

public class NotificationControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private TableView<NotificationItem> notificationTable;
    @FXML private TableColumn<NotificationItem, String> fromUserColumn;
    @FXML private TableColumn<NotificationItem, String> timeColumn;
    @FXML private TableColumn<NotificationItem, String> statusColumn;
    @FXML private Label notificationCountLabel;
    @FXML private Button clearAllButton;
    @FXML private Button refreshButton;
    @FXML private Button acceptAllButton;

    private SocketClient socketClient;
    private String userId;
    private final ObservableList<NotificationItem> notifications = FXCollections.observableArrayList();
    private Stage stage;
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();

        Platform.runLater(() -> {
            setupWindowReference();
        });
    }

    private void setupUI() {
        // 使用Lambda表达式设置CellValueFactory，避免反射问题
        fromUserColumn.setCellValueFactory(cellData -> {
            NotificationItem item = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(item.getFromUsername());
        });

        timeColumn.setCellValueFactory(cellData -> {
            NotificationItem item = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(item.getRequestTime());
        });

        statusColumn.setCellValueFactory(cellData -> {
            NotificationItem item = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(item.getStatusText());
        });

        notificationTable.setItems(notifications);

        // 设置行点击事件
        notificationTable.setRowFactory(tv -> {
            TableRow<NotificationItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    NotificationItem request = row.getItem();
                    showRequestDetail(request);
                }
            });
            return row;
        });

        // 初始状态显示为空
        updateNotificationCount();
    }

    private void setupWindowReference() {
        if (mainContainer != null && mainContainer.getScene() != null) {
            stage = (Stage) mainContainer.getScene().getWindow();
        }
    }

    private void setupEventHandlers() {
        if (clearAllButton != null) {
            clearAllButton.setOnAction(event -> clearAllNotifications());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(event -> loadNotificationsFromServer());
        }
        if (acceptAllButton != null) {
            acceptAllButton.setOnAction(event -> acceptAllRequests());
        }
    }

    // ========== 通知管理方法 ==========
    /**
     * 从服务器加载通知列表
     */
    public void loadNotificationsFromServer() {
        if (!checkConnection()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        new Thread(() -> {
            try {
                // 构建好友请求列表请求
                JsonObject request = new JsonObject();
                request.addProperty("type", "friend_request_list_request");

                String response = socketClient.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null) {
                        try {
                            JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                            if (responseObj.has("success") && responseObj.get("success").getAsBoolean()) {
                                notifications.clear();

                                // 解析请求列表
                                if (responseObj.has("requests")) {
                                    JsonArray requestsArray = responseObj.getAsJsonArray("requests");
                                    for (int i = 0; i < requestsArray.size(); i++) {
                                        JsonObject requestObj = requestsArray.get(i).getAsJsonObject();

                                        Long requestId = requestObj.has("requestId") ?
                                                requestObj.get("requestId").getAsLong() : 0L;
                                        Long fromUserId = requestObj.has("fromUserId") ?
                                                requestObj.get("fromUserId").getAsLong() : 0L;
                                        String fromUsername = requestObj.has("fromUsername") ?
                                                requestObj.get("fromUsername").getAsString() : "未知用户";
                                        String requestTime = requestObj.has("requestTime") ?
                                                requestObj.get("requestTime").getAsString() : "";
                                        Integer status = requestObj.has("status") ?
                                                requestObj.get("status").getAsInt() : 0;

                                        // 只显示待处理的请求
                                        if (status == 0) {
                                            NotificationItem item = new NotificationItem(
                                                    requestId, fromUserId, fromUsername, requestTime, status);
                                            notifications.add(item);
                                        }
                                    }
                                }

                                updateNotificationCount();

                                if (notifications.isEmpty()) {
                                    DialogHelper.showInfo(mainContainer.getScene().getWindow(), "暂无好友请求");
                                }
                            } else {
                                String errorMsg = responseObj.has("message") ?
                                        responseObj.get("message").getAsString() : "加载通知失败";
                                DialogHelper.showError(mainContainer.getScene().getWindow(), errorMsg);
                            }
                        } catch (Exception e) {
                            DialogHelper.showError(mainContainer.getScene().getWindow(),
                                    "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogHelper.showError(mainContainer.getScene().getWindow(), "服务器无响应");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    DialogHelper.showError(mainContainer.getScene().getWindow(),
                            "加载通知失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 从MainControl添加实时通知（供外部调用）
     */
    public void addNotificationFromMainControl(Long requestId, Long fromUserId,
                                               String fromUsername, String requestTime) {
        Platform.runLater(() -> {
            // 检查是否已存在相同的请求
            for (NotificationItem existing : notifications) {
                if (existing.getRequestId().equals(requestId)) {
                    return;
                }
            }

            NotificationItem newItem = new NotificationItem(requestId, fromUserId,
                    fromUsername, requestTime, 0);
            notifications.add(0, newItem); // 添加到列表开头
            updateNotificationCount();

            // 显示桌面通知
            showDesktopNotification("新好友请求",
                    "用户 " + fromUsername + " 请求添加您为好友");
        });
    }

    /**
     * 显示桌面通知
     */
    private void showDesktopNotification(String title, String message) {
        Platform.runLater(() -> {
            try {
                if (mainContainer == null || mainContainer.getScene() == null) {
                    return;
                }

                Alert notification = new Alert(Alert.AlertType.INFORMATION);
                notification.setTitle(title);
                notification.setHeaderText(null);
                notification.setContentText(message);
                notification.initOwner(mainContainer.getScene().getWindow());

                notification.show();

                // 自动关闭通知
                Thread autoCloseThread = new Thread(() -> {
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
                });
                autoCloseThread.setDaemon(true);
                autoCloseThread.start();

            } catch (Exception e) {
                System.err.println("显示桌面通知失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理单个好友请求
     */
    private void processFriendRequest(NotificationItem request, boolean accept) {
        if (!checkConnection()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        new Thread(() -> {
            try {
                // 构建响应请求
                JsonObject responseRequest = new JsonObject();
                responseRequest.addProperty("type", "friend_request_response");
                responseRequest.addProperty("requestId", request.getRequestId());
                responseRequest.addProperty("accept", accept);

                String response = socketClient.sendRequest(responseRequest);

                Platform.runLater(() -> {
                    if (response != null) {
                        try {
                            JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                            if (responseObj.has("success") && responseObj.get("success").getAsBoolean()) {
                                // 更新状态并从列表中移除
                                request.setStatus(accept ? 1 : 2);
                                notifications.remove(request);
                                notificationTable.refresh();

                                String message = accept ?
                                        "已接受 " + request.getFromUsername() + " 的好友请求" :
                                        "已拒绝 " + request.getFromUsername() + " 的好友请求";

                                DialogHelper.showInfo(mainContainer.getScene().getWindow(), message);
                                updateNotificationCount();
                            } else {
                                String errorMsg = responseObj.has("message") ?
                                        responseObj.get("message").getAsString() : "处理请求失败";
                                DialogHelper.showError(mainContainer.getScene().getWindow(), errorMsg);
                            }
                        } catch (Exception e) {
                            DialogHelper.showError(mainContainer.getScene().getWindow(),
                                    "解析响应失败: " + e.getMessage());
                        }
                    } else {
                        DialogHelper.showError(mainContainer.getScene().getWindow(), "服务器无响应");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    DialogHelper.showError(mainContainer.getScene().getWindow(),
                            "处理请求失败: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void acceptAllRequests() {
        if (notifications.isEmpty()) {
            showInfoMessage("没有待处理的请求");
            return;
        }

        // 找出所有待处理的请求
        long pendingCount = notifications.stream()
                .filter(req -> req.getStatus() == 0)
                .count();

        if (pendingCount == 0) {
            showInfoMessage("没有待处理的请求");
            return;
        }

        if (showConfirmationMessage("确定要接受所有好友请求吗？\n共 " + pendingCount + " 个请求")) {
            // 批量处理所有请求
            for (NotificationItem request : notifications) {
                if (request.getStatus() == 0) {
                    processFriendRequest(request, true);
                }
            }
        }
    }

    private void showInfoMessage(String message) {
        if (mainContainer != null && mainContainer.getScene() != null) {
            DialogHelper.showInfo(mainContainer.getScene().getWindow(), message);
        } else if (stage != null) {
            DialogHelper.showInfo(stage, message);
        }
    }

    private boolean showConfirmationMessage(String message) {
        if (mainContainer != null && mainContainer.getScene() != null) {
            return DialogHelper.showConfirmation(mainContainer.getScene().getWindow(), message);
        } else if (stage != null) {
            return DialogHelper.showConfirmation(stage, message);
        }
        return false;
    }

    private void showRequestDetail(NotificationItem request) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("好友请求");
                alert.setHeaderText("来自: " + request.getFromUsername());
                alert.setContentText("请求时间: " + request.getRequestTime() +
                        "\n用户ID: " + request.getFromUserId() +
                        "\n\n是否接受该好友请求？");

                ButtonType acceptButton = new ButtonType("接受");
                ButtonType rejectButton = new ButtonType("拒绝");
                ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(acceptButton, rejectButton, cancelButton);

                // 设置窗口所有者
                if (mainContainer != null && mainContainer.getScene() != null) {
                    alert.initOwner(mainContainer.getScene().getWindow());
                } else if (stage != null) {
                    alert.initOwner(stage);
                }

                alert.showAndWait().ifPresent(buttonType -> {
                    if (buttonType == acceptButton) {
                        processFriendRequest(request, true);
                    } else if (buttonType == rejectButton) {
                        processFriendRequest(request, false);
                    }
                });
            } catch (Exception e) {
                System.err.println("显示请求详情失败: " + e.getMessage());
            }
        });
    }

    /**
     * 清空所有通知
     */
    @FXML
    private void clearAllNotifications() {
        Platform.runLater(() -> {
            if (!notifications.isEmpty()) {
                if (showConfirmationMessage("确定要清空所有通知吗？")) {
                    notifications.clear();
                    updateNotificationCount();
                }
            }
        });
    }

    /**
     * 更新通知计数
     */
    private void updateNotificationCount() {
        long pendingCount = notifications.stream()
                .filter(req -> req.getStatus() == 0)
                .count();

        Platform.runLater(() -> {
            if (notificationCountLabel != null) {
                notificationCountLabel.setText("待处理请求: " + pendingCount + " 条");
            }

            // 如果窗口有标题，更新标题
            if (stage != null) {
                stage.setTitle("通知中心" + (pendingCount > 0 ? " (" + pendingCount + ")" : ""));
            } else if (mainContainer != null && mainContainer.getScene() != null) {
                Stage currentStage = (Stage) mainContainer.getScene().getWindow();
                if (currentStage != null) {
                    currentStage.setTitle("通知中心" + (pendingCount > 0 ? " (" + pendingCount + ")" : ""));
                }
            }
        });
    }

    private boolean checkConnection() {
        return socketClient != null && socketClient.isConnected();
    }

    // ========== Setter 方法 ==========
    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        // 连接后自动加载通知
        if (checkConnection()) {
            Platform.runLater(this::loadNotificationsFromServer);
        }
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // ========== 数据模型类 ==========
    public static class NotificationItem {
        private Long requestId;
        private Long fromUserId;
        private String fromUsername;
        private String requestTime;
        private Integer status; // 0:待处理, 1:已同意, 2:已拒绝

        public NotificationItem(Long requestId, Long fromUserId, String fromUsername,
                                String requestTime, Integer status) {
            this.requestId = requestId;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.requestTime = requestTime;
            this.status = status;
        }

        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }

        public Long getFromUserId() { return fromUserId; }
        public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

        public String getFromUsername() { return fromUsername; }
        public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

        public String getRequestTime() { return requestTime; }
        public void setRequestTime(String requestTime) { this.requestTime = requestTime; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }

        public String getStatusText() {
            switch (status) {
                case 0: return "待处理";
                case 1: return "已同意";
                case 2: return "已拒绝";
                default: return "未知";
            }
        }
    }
}