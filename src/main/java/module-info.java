module com.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires javafx.graphics;
    requires java.prefs;

    opens com.chat.control to javafx.fxml;
    opens com.chat.ui to javafx.fxml;
    opens com.chat.protocol to com.google.gson;
    opens com.chat to javafx.graphics;

    exports com.chat;
}
