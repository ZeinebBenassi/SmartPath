package com.smartpath;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        TabPane tabPane = new TabPane();

        // Tab Matiere
        Tab tabMatiere = new Tab("Matieres");
        tabMatiere.setClosable(false);
        tabMatiere.setContent(
                new FXMLLoader(getClass().getResource(
                        "/com/smartpath/matiere-view.fxml")).load()
        );

        // Tab Quiz
        Tab tabQuiz = new Tab("Quiz");
        tabQuiz.setClosable(false);
        tabQuiz.setContent(
                new FXMLLoader(getClass().getResource(
                        "/com/smartpath/quiz-view.fxml")).load()
        );

        tabPane.getTabs().addAll(tabMatiere, tabQuiz);

        Scene scene = new Scene(tabPane, 1000, 600);
        stage.setTitle("SmartPath Desktop - Cours et Quiz");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
