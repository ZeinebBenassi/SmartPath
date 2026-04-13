package com.smartpath;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.smartpath.util.feature_cours_et_quiz.ViewNavigator;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
    var loaded = ViewNavigator.load("/com/smartpath/feature_cours_et_quiz/login.fxml");
    Scene scene = new Scene(loaded.root(), 1100, 700);

    ViewNavigator.init(stage, scene);

    stage.setTitle("SmartPath Desktop");
    stage.setScene(scene);
    stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
