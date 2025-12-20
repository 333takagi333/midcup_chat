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
    @FXML private TextArea historyArea;
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
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

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

        // 设置文本区域不可编辑，但可以选择复制
        historyArea.setEditable(false);
        historyArea.setWrapText(true);

        // 设置按钮样式
        setupButtonStyles();

        // 显示初始化提示
        historyArea.setText("正在初始化历史记录窗口...");
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

        // 清空历史区域，显示加载提示
        historyArea.setText("正在加载与 " + targetName + " 的历史记录...");

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

    /**
     * 加载更多历史消息
     */
    @FXML
    private void loadMoreHistory() {
        if (earliestTimestamp != null) {
            loadHistoryMessages();
        }
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
                    historyArea.setText("暂无历史聊天记录");
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
            historyArea.setText("暂无历史聊天记录");
            return;
        }

        // 按时间排序（从旧到新）
        allHistoryMessages.sort((a, b) -> {
            Long timeA = historyService.dbDateTimeToTimestamp(a.getTimestamp());
            Long timeB = historyService.dbDateTimeToTimestamp(b.getTimestamp());
            return timeA.compareTo(timeB);
        });

        StringBuilder sb = new StringBuilder();
        String lastDay = "";

        for (HistoryMessageItem item : allHistoryMessages) {
            try {
                // 解析数据库时间
                java.util.Date messageDate = historyService.parseDbDateTime(item.getTimestamp());

                // 检查日期变化
                String currentDay = dayFormat.format(messageDate);
                if (!currentDay.equals(lastDay)) {
                    if (!lastDay.isEmpty()) {
                        sb.append("\n");
                    }
                    sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    sb.append("                 ").append(currentDay).append("\n");
                    sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
                    lastDay = currentDay;
                }

                // 格式化显示
                String time = timeFormat.format(messageDate);
                String senderName = item.getSenderId().equals(userId) ? "我" :
                        (chatType.equals("private") ? targetName : "用户" + item.getSenderId());

                sb.append("[").append(time).append("] ").append(senderName).append(": ")
                        .append(item.getContent()).append("\n");

            } catch (Exception e) {
                System.err.println("格式化历史消息失败: " + e.getMessage());
            }
        }

        historyArea.setText(sb.toString());

        // 滚动到底部（最新消息）
        historyArea.selectPositionCaret(historyArea.getLength());
        historyArea.deselect();
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        historyArea.setText("错误: " + message + "\n\n请检查网络连接或稍后重试。");
        statusLabel.setText("加载失败");
    }

    /**
     * 关闭窗口
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) historyArea.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 获取窗口标题
     */
    public String getWindowTitle() {
        return targetName + " - 历史记录";
    }
}