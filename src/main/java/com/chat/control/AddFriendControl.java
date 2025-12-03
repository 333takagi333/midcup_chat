package com.chat.control;

import com.chat.network.SocketClient;
import com.chat.protocol.FriendAddRequest;
import com.chat.ui.DialogHelper;
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

        if (friendIdStr.isEmpty()) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "请输入好友ID");
            return;
        }

        try {
            Long targetId = Long.parseLong(friendIdStr);

            // 直接发送FriendAddRequest对象，而不是手动转JSON
            FriendAddRequest request = new FriendAddRequest();
            request.setToUserId(targetId);

            // 使用socketClient.sendRequest发送对象
            String response = socketClient.sendRequest(request);

            if (response != null) {
                DialogHelper.showInfo(mainContainer.getScene().getWindow(), "好友请求已发送");
                friendIdField.clear();
                ((Stage) mainContainer.getScene().getWindow()).close();
            } else {
                DialogHelper.showError(mainContainer.getScene().getWindow(), "发送好友请求失败");
            }
        } catch (NumberFormatException e) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "请输入有效的用户ID（数字）");
        } catch (Exception e) {
            DialogHelper.showError(mainContainer.getScene().getWindow(), "添加好友失败: " + e.getMessage());
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