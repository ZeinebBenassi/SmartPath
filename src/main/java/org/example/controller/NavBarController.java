package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class NavBarController {

    @FXML
    private void goHome(ActionEvent event) {
        navigate(event, "/org/example/home_view.fxml");
    }

    @FXML
    private void goCourses(ActionEvent event) {
        navigate(event, "/org/example/cours_view.fxml");
    }

    @FXML
    private void goAddCourse(ActionEvent event) {
        navigate(event, "/org/example/matiere_form.fxml");
    }

    private void navigate(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
