package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.NotificationService;
import com.chat.ui.DialogHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private TableView<NotificationService.NotificationItem> notificationTable;
    @FXML private TableColumn<NotificationService.NotificationItem, String> fromUserColumn;
    @FXML private TableColumn<NotificationService.NotificationItem, String> timeColumn;
    @FXML private TableColumn<NotificationService.NotificationItem, String> statusColumn;
    @FXML private Label notificationCountLabel;
    @FXML private Button clearAllButton;
    @FXML private Button refreshButton;
    @FXML private Button acceptAllButton;

    private SocketClient socketClient;
    private String userId;
    private final javafx.collections.ObservableList<NotificationService.NotificationItem> notifications =
            javafx.collections.FXCollections.observableArrayList();
    private NotificationService notificationService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        fromUserColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFromUsername()));

        timeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRequestTime()));

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatusText()));

        notificationTable.setItems(notifications);

        notificationTable.setRowFactory(tv -> {
            TableRow<NotificationService.NotificationItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    NotificationService.NotificationItem request = row.getItem();
                    showRequestDetail(request);
                }
            });
            return row;
        });

        updateNotificationCount();
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

    public void loadNotificationsFromServer() {
        if (!checkConnection()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        new Thread(() -> {
            try {
                notificationService = new NotificationService();
                List<NotificationService.NotificationItem> requests =
                        notificationService.loadFriendRequests(socketClient);

                Platform.runLater(() -> {
                    notifications.clear();
                    notifications.addAll(requests);
                    updateNotificationCount();

                    if (notifications.isEmpty()) {
                        DialogHelper.showInfo(mainContainer.getScene().getWindow(), "暂无好友请求");
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

    public void addNotificationFromMainControl(Long requestId, Long fromUserId,
                                               String fromUsername, String requestTime) {
        Platform.runLater(() -> {
            for (NotificationService.NotificationItem existing : notifications) {
                if (existing.getRequestId().equals(requestId)) {
                    return;
                }
            }

            NotificationService.NotificationItem newItem = new NotificationService.NotificationItem(
                    requestId, fromUserId, fromUsername, requestTime, 0);
            notifications.add(0, newItem);
            updateNotificationCount();

            showDesktopNotification("新好友请求",
                    "用户 " + fromUsername + " 请求添加您为好友");
        });
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

    private void processFriendRequest(NotificationService.NotificationItem request, boolean accept) {
        if (!checkConnection()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "未连接到服务器");
            return;
        }

        new Thread(() -> {
            try {
                notificationService = new NotificationService();
                boolean success = notificationService.processFriendRequest(
                        socketClient, request.getRequestId(), accept);

                Platform.runLater(() -> {
                    if (success) {
                        notifications.remove(request);
                        notificationTable.refresh();

                        String message = accept ?
                                "已接受 " + request.getFromUsername() + " 的好友请求" :
                                "已拒绝 " + request.getFromUsername() + " 的好友请求";

                        DialogHelper.showInfo(mainContainer.getScene().getWindow(), message);
                        updateNotificationCount();
                    } else {
                        DialogHelper.showError(mainContainer.getScene().getWindow(), "处理请求失败");
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

        long pendingCount = notifications.stream()
                .filter(req -> req.getStatus() == 0)
                .count();

        if (pendingCount == 0) {
            showInfoMessage("没有待处理的请求");
            return;
        }

        if (showConfirmationMessage("确定要接受所有好友请求吗？\n共 " + pendingCount + " 个请求")) {
            for (NotificationService.NotificationItem request : notifications) {
                if (request.getStatus() == 0) {
                    processFriendRequest(request, true);
                }
            }
        }
    }

    private void showInfoMessage(String message) {
        if (mainContainer != null && mainContainer.getScene() != null) {
            DialogHelper.showInfo(mainContainer.getScene().getWindow(), message);
        }
    }

    private boolean showConfirmationMessage(String message) {
        if (mainContainer != null && mainContainer.getScene() != null) {
            return DialogHelper.showConfirmation(mainContainer.getScene().getWindow(), message);
        }
        return false;
    }

    private void showRequestDetail(NotificationService.NotificationItem request) {
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
                alert.initOwner(mainContainer.getScene().getWindow());

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

    private void updateNotificationCount() {
        long pendingCount = notifications.stream()
                .filter(req -> req.getStatus() == 0)
                .count();

        Platform.runLater(() -> {
            if (notificationCountLabel != null) {
                notificationCountLabel.setText("待处理请求: " + pendingCount + " 条");
            }

            if (mainContainer != null && mainContainer.getScene() != null) {
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

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        if (checkConnection()) {
            Platform.runLater(this::loadNotificationsFromServer);
        }
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}