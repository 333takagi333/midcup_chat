package com.chat.service;

import com.chat.model.ChatItem;
import com.chat.model.FriendItem;
import com.chat.model.GroupItem;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 窗口管理服务
 */
public class WindowManagementService {

    final Map<String, Stage> activeWindows = new HashMap<>();

    /**
     * 获取聊天窗口的键
     */
    public static String getChatWindowKey(ChatItem chat) {
        return chat.isGroup() ? "group_" + chat.getId() : "private_" + chat.getId();
    }

    /**
     * 获取私聊窗口的键
     */
    public static String getPrivateChatWindowKey(FriendItem friend) {
        return "private_" + friend.getUserId();
    }

    /**
     * 获取群聊窗口的键
     */
    public static String getGroupChatWindowKey(GroupItem group) {
        return "group_" + group.getGroupId();
    }

    /**
     * 检查窗口是否已打开
     */
    public boolean isWindowAlreadyOpen(String windowKey) {
        if (windowKey == null) return false;
        Stage stage = activeWindows.get(windowKey);
        return stage != null && stage.isShowing();
    }

    /**
     * 将窗口提到前面
     */
    public void bringWindowToFront(String windowKey) {
        Stage stage = activeWindows.get(windowKey);
        if (stage != null) {
            stage.toFront();
            stage.requestFocus();
            if (stage.isIconified()) {
                stage.setIconified(false);
            }
        }
    }

    /**
     * 打开管理窗口
     */
    public void openManagedWindow(Class<?> controllerClass, String fxmlPath,
                                  Consumer<Object> controllerSetup, String title,
                                  int width, int height, String windowKey) {
        if (isWindowAlreadyOpen(windowKey)) {
            bringWindowToFront(windowKey);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(controllerClass.getResource(fxmlPath));
            Parent root = loader.load();
            controllerSetup.accept(loader.getController());

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.setUserData(loader);

            stage.setOnCloseRequest(event -> {
                activeWindows.remove(windowKey);
            });

            stage.show();
            activeWindows.put(windowKey, stage);

        } catch (IOException e) {
            System.err.println("打开窗口失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取窗口控制器
     */
    public <T> T getWindowController(String windowKey) {
        try {
            Stage stage = activeWindows.get(windowKey);
            if (stage != null) {
                FXMLLoader loader = (FXMLLoader) stage.getUserData();
                if (loader != null) {
                    return loader.getController();
                }
            }
        } catch (Exception e) {
            System.err.println("获取窗口控制器失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 关闭所有窗口
     */
    public void closeAllWindows() {
        for (Stage stage : activeWindows.values()) {
            if (stage != null && stage.isShowing()) {
                stage.close();
            }
        }
        activeWindows.clear();
    }

    /**
     * 关闭指定窗口
     */
    public void closeWindow(String windowKey) {
        Stage stage = activeWindows.get(windowKey);
        if (stage != null && stage.isShowing()) {
            stage.close();
        }
        activeWindows.remove(windowKey);
    }

    /**
     * 获取活动窗口数量
     */
    public int getActiveWindowCount() {
        return activeWindows.size();
    }
    public void addActiveWindow(String key, Stage stage) {
        if (key != null && stage != null) {
            activeWindows.put(key, stage);
        }
    }
}