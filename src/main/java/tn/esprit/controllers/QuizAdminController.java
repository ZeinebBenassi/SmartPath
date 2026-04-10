package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

/**
 * QuizAdminController — pont vers QuestionView.fxml (CRUD complet).
 * Redirige automatiquement au chargement vers la vraie vue de gestion.
 */
public class QuizAdminController {

    // Champs FXML gardés pour compatibilité
    @FXML private ListView<String> questionsList;
    @FXML private Label countLabel, totalQLabel, totalRepLabel, editorTitle, saveStatus;
    @FXML private TextArea questionTextArea;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private TextField ordreField, rep1Field, rep2Field, rep3Field, rep4Field;
    @FXML private ComboBox<String> profil1Combo, profil2Combo, profil3Combo, profil4Combo;

    @FXML
    public void initialize() {
        // Initialiser les combos pour éviter les erreurs NPE
        for (ComboBox<String> cb : new ComboBox[]{profil1Combo, profil2Combo, profil3Combo, profil4Combo, categorieCombo}) {
            if (cb != null) cb.setItems(FXCollections.observableArrayList());
        }
        // Rediriger vers QuestionView.fxml dès que la scène est prête
        javafx.application.Platform.runLater(this::redirectToQuestionView);
    }

    private void redirectToQuestionView() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/tn/esprit/interfaces/QuestionView.fxml"));
            javafx.scene.Node anchor = questionsList != null ? questionsList : totalQLabel;
            if (anchor != null && anchor.getScene() != null) {
                anchor.getScene().setRoot(root);
            }
        } catch (Exception e) {
            System.err.println("Redirection QuestionView impossible : " + e.getMessage());
        }
    }

    // Actions redirigent toutes vers QuestionView
    @FXML public void addQuestion()  { redirectToQuestionView(); }
    @FXML public void saveQuestion() { redirectToQuestionView(); }
    @FXML public void deleteQuestion(){ redirectToQuestionView(); }
    @FXML public void cancelEdit()   {}
    @FXML public void previewQuiz()  { redirectToQuestionView(); }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (questionsList != null && questionsList.getScene() != null)
                questionsList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
