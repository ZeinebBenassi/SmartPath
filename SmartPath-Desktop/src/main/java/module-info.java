 module com.smartpath {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.smartpath to javafx.fxml;
    opens com.smartpath.controller to javafx.fxml;
    opens com.smartpath.model to javafx.base;

    exports com.smartpath;
}

