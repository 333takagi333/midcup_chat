package com.chat.control;

import com.chat.network.SocketClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 主界面控制器：三层布局，顶部显示当前用户名；中部、底部暂不放内容。
 */
public class MainController implements Initializable {

    @FXML private Label usernameLabel;

    private String currentUsername;
    private SocketClient socketClient;

    /**
     * 初始化：此处无需额外逻辑。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 保留空实现，便于后续扩展
    }

    /**
     * 设置当前登录用户名，并更新顶部显示。
     */
    public void setUsername(String username) {
        this.currentUsername = username;
        if (usernameLabel != null) {
            usernameLabel.setText("当前用户: " + username);
        }
    }

    /**
     * 注入 SocketClient，供后续页面扩展使用。
     */
    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
    }
}