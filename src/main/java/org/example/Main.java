package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
        Scene scene = new Scene(root);
        
        // Charger le fichier CSS
        String css = getClass().getResource("/tn/esprit/interfaces/smartpath.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        primaryStage.setTitle("SmartPath");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(660);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
