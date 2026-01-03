package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.FriendAddResponse;
import com.chat.service.FriendManagementService;
import com.chat.ui.DialogHelper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AddFriendControl implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private TextField friendIdField;
    @FXML private Button addButton;
    @FXML private Button cancelButton;

    private SocketClient socketClient;
    private FriendManagementService friendService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        friendIdField.setOnAction(event -> addFriend());
        addButton.setOnAction(event -> addFriend());
        cancelButton.setOnAction(event -> cancel());
    }

    @FXML
    private void addFriend() {
        String friendIdStr = friendIdField.getText().trim();

        // 使用Service验证输入
        String validationError = FriendManagementService.validateFriendIdInput(friendIdStr);
        if (validationError != null) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), validationError);
            return;
        }

        addButton.setDisable(true);

        Task<FriendAddResponse> task = new Task<FriendAddResponse>() {
            @Override
            protected FriendAddResponse call() {
                try {
                    Long targetId = Long.parseLong(friendIdStr);
                    friendService = new FriendManagementService();
                    return friendService.sendFriendRequest(socketClient, targetId);
                } catch (Exception e) {
                    System.err.println("添加好友失败: " + e.getMessage());
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            addButton.setDisable(false);
            handleAddFriendResponse(task.getValue());
        });

        task.setOnFailed(e -> {
            addButton.setDisable(false);
            DialogHelper.showError(mainContainer.getScene().getWindow(), "添加好友失败");
        });

        new Thread(task).start();
    }

    private void handleAddFriendResponse(FriendAddResponse response) {
        if (response != null && response.isSuccess()) {
            DialogHelper.showInfo(mainContainer.getScene().getWindow(), "好友请求已发送");
            friendIdField.clear();
            closeWindow();
        } else {
            String errorMsg = "发送好友请求失败";
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