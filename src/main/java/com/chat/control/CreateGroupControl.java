package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.GroupCreateResponse;
import com.chat.service.GroupManagementService;
import com.chat.ui.DialogHelper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateGroupControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private TextField groupNameField;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    @FXML private Label titleLabel;

    private SocketClient socketClient;
    private GroupManagementService groupService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        titleLabel.setText("创建新群聊");
        groupNameField.setPromptText("请输入群聊名称（1-20个字符）");
    }

    private void setupEventHandlers() {
        groupNameField.setOnAction(event -> createGroup());
        createButton.setOnAction(event -> createGroup());
        cancelButton.setOnAction(event -> cancel());
    }

    @FXML
    private void createGroup() {
        String groupName = groupNameField.getText().trim();

        // 使用Service验证输入
        String validationError = GroupManagementService.validateGroupNameInput(groupName);
        if (validationError != null) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), validationError);
            groupNameField.requestFocus();
            return;
        }

        createButton.setDisable(true);

        Task<GroupCreateResponse> task = new Task<GroupCreateResponse>() {
            @Override
            protected GroupCreateResponse call() {
                try {
                    groupService = new GroupManagementService();
                    return groupService.createGroup(socketClient, groupName);
                } catch (Exception e) {
                    System.err.println("创建群聊失败: " + e.getMessage());
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            createButton.setDisable(false);
            handleCreateGroupResponse(task.getValue());
        });

        task.setOnFailed(e -> {
            createButton.setDisable(false);
            DialogHelper.showError(mainContainer.getScene().getWindow(), "创建群聊失败");
        });

        new Thread(task).start();
    }

    private void handleCreateGroupResponse(GroupCreateResponse response) {
        if (response != null && response.isSuccess()) {
            DialogHelper.showInfo(mainContainer.getScene().getWindow(),
                    "群聊创建成功: " + response.getGroupName());
            groupNameField.clear();
            closeWindow();
        } else {
            String errorMsg = "创建群聊失败";
            if (response != null && response.getMessage() != null) {
                errorMsg += ": " + response.getMessage();
            }
            DialogHelper.showError(mainContainer.getScene().getWindow(), errorMsg);
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) mainContainer.getScene().getWindow()).close();
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
    }
}