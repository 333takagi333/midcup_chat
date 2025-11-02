package com.chat.ui;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

/**
 * 自定义按钮组件，封装了一个可通过 FXML 定义样式和行为的 Button。
 */
public class CustomButton extends AnchorPane {
    @FXML
    private Button button;

    /**
     * 构造函数：加载对应的 FXML 并将自身设为根节点与控制器。
     */
    public CustomButton() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/chat/fxml/components/CustomButton.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * 获取按钮文本。
     */
    public String getText() {
        return textProperty().get();
    }

    /**
     * 设置按钮文本。
     */
    public void setText(String value) {
        textProperty().set(value);
    }

    /**
     * 获取文本属性，便于与外部进行数据绑定。
     */
    public StringProperty textProperty() {
        return button.textProperty();
    }

    /**
     * 设置按钮点击事件处理器。
     */
    public void setOnAction(EventHandler<ActionEvent> value) {
        button.setOnAction(value);
    }

    /**
     * 获取按钮点击事件处理器。
     */
    public EventHandler<ActionEvent> getOnAction() {
        return button.getOnAction();
    }


}
