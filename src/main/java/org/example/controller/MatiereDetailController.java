package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entity.Lecon;
import org.example.entity.Matiere;
import org.example.entity.Test;
import org.example.service.MatiereService;

import java.io.IOException;

public class MatiereDetailController {

    @FXML private Label matiereTitleLabel;
    @FXML private Label matiereDescLabel;
    @FXML private ListView<Lecon> leconsListView;
    @FXML private ListView<Test> testsListView;

    private MatiereService matiereService = new MatiereService();
    private Matiere currentMatiere;

    public void initData(int matiereId) {
        currentMatiere = matiereService.findWithDetails(matiereId);
        if (currentMatiere != null) {
            matiereTitleLabel.setText(currentMatiere.getTitre());
            matiereDescLabel.setText(currentMatiere.getDescription());
            
            ObservableList<Lecon> lecons = FXCollections.observableArrayList(currentMatiere.getLecons());
            leconsListView.setItems(lecons);
            
            ObservableList<Test> tests = FXCollections.observableArrayList(currentMatiere.getTests());
            testsListView.setItems(tests);
        }
    }

    @FXML
    private void handleBack(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/cours_view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStartSelectedTest(javafx.event.ActionEvent event) {
        Test selected = testsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/quiz_questions.fxml"));
                Parent root = loader.load();
                
                QuizQuestionController quizController = loader.getController();
                quizController.initData(selected.getId());
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Erreur lors du lancement du quiz.");
                alert.show();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Veuillez sélectionner un quiz.");
            alert.show();
        }
    }
}
