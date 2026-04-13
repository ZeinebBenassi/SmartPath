package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

/**
 * QuizAdminController — utilisé uniquement pour compatibilité FXML.
 * La vraie logique est dans QuestionController (chargé via QuestionContent.fxml).
 * Ce controller NE redirige PLUS toute la scène — il reste dans le contentArea.
 */
public class QuizAdminController {

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
        // Ne rien faire d'autre : ce FXML n'est plus utilisé directement.
        // Le DashboardAdminController charge QuestionContent.fxml directement dans contentArea.
    }

    @FXML public void addQuestion()    {}
    @FXML public void saveQuestion()   {}
    @FXML public void deleteQuestion() {}
    @FXML public void cancelEdit()     {}
    @FXML public void previewQuiz()    {}

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (questionsList != null && questionsList.getScene() != null)
                questionsList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
