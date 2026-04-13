 module com.smartpath {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.smartpath to javafx.fxml;
    opens com.smartpath.controller.feature_cours_et_quiz to javafx.fxml;
    opens com.smartpath.model to javafx.base;
    opens com.smartpath.model.feature_cours_et_quiz to javafx.base;

    exports com.smartpath;
}

