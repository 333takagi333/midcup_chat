package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.service.HistoryService;
import com.chat.protocol.ChatHistoryResponse;
import com.chat.protocol.ChatHistoryResponse.HistoryMessageItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 历史记录窗口控制器
 */
public class ChatHistoryWindowControl implements Initializable, HistoryService.HistoryCallback {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label infoLabel;
    // replaced TextArea with ListView for bubble rendering
    @FXML private ListView<HistoryMessageItem> historyListView;
    @FXML private ComboBox<Integer> limitComboBox;
    @FXML private Button loadMoreButton;
    @FXML private Button closeButton;

    // 聊天类型：private 或 group
    private String chatType;
    private Long targetId;
    private String targetName;
    private Long userId;
    private SocketClient socketClient;
    private HistoryService historyService;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    // 分页相关
    private Long earliestTimestamp = null;
    private boolean isLoading = false;
    private int totalMessagesLoaded = 0;

    // 存储所有历史消息
    private final List<HistoryMessageItem> allHistoryMessages = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化 HistoryService
        historyService = new HistoryService();

        // 设置限制选项
        ObservableList<Integer> limits = FXCollections.observableArrayList(20, 50, 100, 200);
        limitComboBox.setItems(limits);
        limitComboBox.setValue(50);

        // 设置 ListView 初始状态
        historyListView.setPlaceholder(new Label("正在初始化历史记录窗口..."));
        historyListView.setFocusTraversable(false);

        // 使用自定义 cell 渲染消息气泡
        historyListView.setCellFactory(list -> new HistoryCell());

        // 设置按钮样式
        setupButtonStyles();

        // 显示初始化提示
        historyListView.getItems().clear();

        // 将加载按钮默认文本保留
        loadMoreButton.setText("加载历史记录");
    }

    private void setupButtonStyles() {
        loadMoreButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    /**
     * 设置历史记录窗口信息
     */
    public void setHistoryInfo(String chatType, Long targetId, String targetName,
                               Long userId, SocketClient socketClient) {
        this.chatType = chatType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.userId = userId;
        this.socketClient = socketClient;

        // 设置标题
        titleLabel.setText(targetName + " - 历史记录");
        infoLabel.setText("正在加载历史记录...");
        statusLabel.setText("准备加载...");

        // 禁用加载更多按钮，直到第一次加载完成
        loadMoreButton.setDisable(true);

        // 清空历史区域，显示加载提示（通过 placeholder）
        historyListView.getItems().clear();
        historyListView.setPlaceholder(new Label("正在加载与 " + targetName + " 的历史记录..."));

        // 开始加载历史记录（延迟一点，确保UI完全加载）
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadHistoryMessages());
            }
        }, 300);
    }

    /**
     * 加载历史消息
     */
    @FXML
    private void loadHistoryMessages() {
        if (socketClient == null || !socketClient.isConnected() || targetId == null) {
            showError("未连接到服务器");
            return;
        }

        if (isLoading) {
            System.out.println("[ChatHistoryWindow] 历史消息正在加载中");
            return;
        }

        isLoading = true;
        loadMoreButton.setDisable(true);
        statusLabel.setText("加载中...");

        int limit = limitComboBox.getValue();

        System.out.println("[ChatHistoryWindow] 开始加载历史记录: " + chatType + ", 目标ID: " + targetId);

        // 使用 HistoryService 加载历史消息
        historyService.loadHistoryMessages(
                socketClient,
                chatType,
                targetId,
                limit,
                earliestTimestamp, // 如果为null，则获取最新的
                this  // 回调接口
        );
    }

    @Override
    public void onHistoryLoaded(ChatHistoryResponse response, String error) {
        Platform.runLater(() -> {
            isLoading = false;

            if (error != null) {
                System.err.println("[ChatHistoryWindow] 加载历史消息失败: " + error);
                statusLabel.setText("加载失败");
                showError("加载历史消息失败: " + error);
                loadMoreButton.setDisable(false);
                return;
            }

            if (response == null || response.getMessages() == null || response.getMessages().isEmpty()) {
                System.out.println("[ChatHistoryWindow] 没有更多历史消息");
                statusLabel.setText("没有更多历史消息");
                loadMoreButton.setDisable(true);

                if (totalMessagesLoaded == 0) {
                    historyListView.getItems().clear();
                    historyListView.setPlaceholder(new Label("暂无历史聊天记录"));
                    infoLabel.setText("共 0 条记录");
                } else {
                    infoLabel.setText("共 " + totalMessagesLoaded + " 条记录，已加载全部");
                }
                return;
            }

            // 处理历史消息
            List<HistoryMessageItem> messages = response.getMessages();
            System.out.println("[ChatHistoryWindow] 成功加载 " + messages.size() + " 条历史消息");

            // 添加到总列表
            allHistoryMessages.addAll(messages);
            totalMessagesLoaded += messages.size();

            // 重新显示所有历史消息（从旧到新）
            displayAllHistoryMessages();

            // 更新最早时间戳（用于下一次加载）
            Long newEarliestTimestamp = null;
            for (HistoryMessageItem item : messages) {
                Long itemTimestamp = historyService.dbDateTimeToTimestamp(item.getTimestamp());
                if (newEarliestTimestamp == null || itemTimestamp < newEarliestTimestamp) {
                    newEarliestTimestamp = itemTimestamp;
                }
            }
            earliestTimestamp = newEarliestTimestamp;

            statusLabel.setText("已加载 " + totalMessagesLoaded + " 条记录");

            // 检查是否还有更多消息可以加载
            if (messages.size() >= limitComboBox.getValue()) {
                loadMoreButton.setDisable(false);
                loadMoreButton.setText("加载更多 (" + limitComboBox.getValue() + "条)");
            } else {
                loadMoreButton.setDisable(true);
                loadMoreButton.setText("已加载全部");
            }

            infoLabel.setText("共 " + totalMessagesLoaded + " 条历史记录");
        });
    }

    /**
     * 显示所有历史消息（按时间从旧到新排序）
     */
    private void displayAllHistoryMessages() {
        if (allHistoryMessages.isEmpty()) {
            historyListView.getItems().clear();
            historyListView.setPlaceholder(new Label("暂无历史聊天记录"));
            return;
        }

        // 按时间排序（从旧到新）
        allHistoryMessages.sort((a, b) -> {
            Long timeA = historyService.dbDateTimeToTimestamp(a.getTimestamp());
            Long timeB = historyService.dbDateTimeToTimestamp(b.getTimestamp());
            return timeA.compareTo(timeB);
        });

        ObservableList<HistoryMessageItem> items = FXCollections.observableArrayList();

        // 我们保留原有按日分隔的逻辑，但将日期分隔作为占位消息不可行（类型不同），
        // 所以这里只把消息按顺序放入 ListView，ListCell 负责显示时间与内容
        items.addAll(allHistoryMessages);

        historyListView.setItems(items);

        // 滚动到底部（最新消息）
        if (!items.isEmpty()) {
            historyListView.scrollTo(items.size() - 1);
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        historyListView.getItems().clear();
        historyListView.setPlaceholder(new Label("错误: " + message + "\n\n请检查网络连接或稍后重试。"));
        statusLabel.setText("加载失败");
    }

    /**
     * 关闭窗口
     */
    @FXML
    private void closeWindow() {
        Stage stage = null;
        if (closeButton != null && closeButton.getScene() != null) {
            stage = (Stage) closeButton.getScene().getWindow();
        } else if (historyListView != null && historyListView.getScene() != null) {
            stage = (Stage) historyListView.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 自定义 ListCell：消息气泡，自己消息靠右，收到的消息靠左
     */
    private class HistoryCell extends ListCell<HistoryMessageItem> {
        private final HBox container = new HBox();
        private final VBox contentBox = new VBox();
        private final Label messageLabel = new Label();
        private final Label timeLabel = new Label();
        private final Region spacer = new Region();

        public HistoryCell() {
            super();
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(420);
            messageLabel.setPadding(new Insets(8, 12, 8, 12));
            messageLabel.setStyle("-fx-background-radius: 10; -fx-font-size: 13px;");

            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-padding: 2 6 0 6;");

            contentBox.getChildren().addAll(messageLabel, timeLabel);
            contentBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            container.setPadding(new Insets(6, 10, 6, 10));
        }

        @Override
        protected void updateItem(HistoryMessageItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                messageLabel.setText(item.getContent());
                String time = "";
                try {
                    java.util.Date messageDate = historyService.parseDbDateTime(item.getTimestamp());
                    time = timeFormat.format(messageDate);
                } catch (Exception e) {
                    // ignore
                }
                timeLabel.setText(time);

                container.getChildren().clear();

                boolean isMine = (item.getSenderId() != null && item.getSenderId().equals(userId));
                if (isMine) {
                    // 右侧气泡
                    messageLabel.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-size: 13px; -fx-padding: 8 12 8 12;");
                    contentBox.setAlignment(Pos.CENTER_RIGHT);
                    container.setAlignment(Pos.CENTER_RIGHT);
                    container.getChildren().addAll(spacer, contentBox);
                } else {
                    // 左侧气泡
                    messageLabel.setStyle("-fx-background-color: #f1f0f0; -fx-text-fill: #222; -fx-background-radius: 12; -fx-font-size: 13px; -fx-padding: 8 12 8 12;");
                    contentBox.setAlignment(Pos.CENTER_LEFT);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.getChildren().addAll(contentBox, spacer);
                }

                setGraphic(container);
            }
        }
    }

    /**
     * 获取窗口标题
     */
    public String getWindowTitle() {
        return targetName + " - 历史记录";
    }
}
