package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.GroupCreateRequest;
import com.chat.ui.DialogHelper;
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

        if (groupName.isEmpty()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "请输入群聊名称");
            groupNameField.requestFocus();
            return;
        }

        if (groupName.length() > 20) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "群聊名称不能超过20个字符");
            groupNameField.requestFocus();
            return;
        }

        try {
            // 直接发送GroupCreateRequest对象
            GroupCreateRequest request = new GroupCreateRequest(groupName);

            // 使用socketClient.sendRequest发送对象
            String response = socketClient.sendRequest(request);

            if (response != null) {
                DialogHelper.showInfo(mainContainer.getScene().getWindow(), "群聊创建成功");
                groupNameField.clear();
                ((Stage) mainContainer.getScene().getWindow()).close();
            } else {
                DialogHelper.showError(mainContainer.getScene().getWindow(), "创建群聊失败：无响应");
            }
        } catch (Exception e) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "创建群聊失败: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        ((Stage) mainContainer.getScene().getWindow()).close();
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
    }
}