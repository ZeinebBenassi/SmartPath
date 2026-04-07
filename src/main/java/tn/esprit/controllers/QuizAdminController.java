package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class QuizAdminController {

    @FXML private ListView<String> questionsList;
    @FXML private Label countLabel;
    @FXML private Label totalQLabel;
    @FXML private Label totalRepLabel;
    @FXML private Label editorTitle;
    @FXML private Label saveStatus;

    @FXML private TextArea questionTextArea;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private TextField ordreField;

    @FXML private TextField rep1Field;
    @FXML private TextField rep2Field;
    @FXML private TextField rep3Field;
    @FXML private TextField rep4Field;

    @FXML private ComboBox<String> profil1Combo;
    @FXML private ComboBox<String> profil2Combo;
    @FXML private ComboBox<String> profil3Combo;
    @FXML private ComboBox<String> profil4Combo;

    private static final String[] PROFILS = {"Technologique", "CrÃ©atif", "Management", "Humain"};

    private static final String[] SAMPLE_QUESTIONS = {
        "1. Quel type d'activitÃ©s vous attire le plus ?",
        "2. Vous prÃ©fÃ©rez travailler :",
        "3. Dans un projet, vous prenez le rÃ´le de :",
        "4. Quelle matiÃ¨re prÃ©fÃ©rez-vous ?",
        "5. Votre environnement de travail idÃ©al ?",
        "6. Qu'est-ce qui vous motive le plus ?",
        "7. Votre rapport aux nouvelles technologies ?",
        "8. Comment gÃ©rez-vous un problÃ¨me complexe ?",
        "9. Votre style de communication prÃ©fÃ©rÃ© ?",
        "10. Votre objectif professionnel principal ?"
    };

    @FXML
    public void initialize() {
        for (ComboBox<String> cb : new ComboBox[]{profil1Combo, profil2Combo, profil3Combo, profil4Combo}) {
            if (cb != null) {
                cb.setItems(FXCollections.observableArrayList(PROFILS));
            }
        }
        if (categorieCombo != null) {
            categorieCombo.setItems(FXCollections.observableArrayList(PROFILS));
        }

        if (questionsList != null) {
            questionsList.getItems().clear();
            questionsList.setPlaceholder(new Label("Aucune question disponible pour le moment."));
        }

        updateCounters();
    }

    private void loadQuestion(String selected) {
        int idx = questionsList.getSelectionModel().getSelectedIndex();
        if (editorTitle != null) editorTitle.setText("Modifier : Question " + (idx + 1));
        if (ordreField  != null) ordreField.setText(String.valueOf(idx + 1));

        // Simulation chargement donnÃ©es
        if (questionTextArea != null)
            questionTextArea.setText(selected.substring(selected.indexOf(". ") + 2));
        if (categorieCombo   != null) categorieCombo.getSelectionModel().select(idx % 4);
        if (rep1Field  != null) rep1Field.setText("Analyser des donnÃ©es et rÃ©soudre des problÃ¨mes logiques");
        if (rep2Field  != null) rep2Field.setText("CrÃ©er des designs et interfaces visuelles");
        if (rep3Field  != null) rep3Field.setText("GÃ©rer des Ã©quipes et des projets");
        if (rep4Field  != null) rep4Field.setText("Comprendre et aider les gens");
        if (profil1Combo != null) profil1Combo.getSelectionModel().select(0);
        if (profil2Combo != null) profil2Combo.getSelectionModel().select(1);
        if (profil3Combo != null) profil3Combo.getSelectionModel().select(2);
        if (profil4Combo != null) profil4Combo.getSelectionModel().select(3);
        if (saveStatus != null) saveStatus.setText("");
    }

    @FXML public void addQuestion() {
        int newIdx = questionsList != null ? questionsList.getItems().size() + 1 : 1;
        if (questionsList != null)
            questionsList.getItems().add(newIdx + ". Nouvelle question...");
        if (editorTitle != null) editorTitle.setText("Nouvelle question " + newIdx);
        clearEditor();
        if (ordreField != null) ordreField.setText(String.valueOf(newIdx));
        updateCounters();
    }

    @FXML public void saveQuestion() {
        if (questionTextArea == null || questionTextArea.getText().isEmpty()) {
            if (saveStatus != null) {
                saveStatus.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
                saveStatus.setText("âš  Le texte de la question est requis.");
            }
            return;
        }
        int idx = questionsList != null ? questionsList.getSelectionModel().getSelectedIndex() : -1;
        if (idx >= 0 && questionsList != null) {
            String newText = (idx + 1) + ". " + questionTextArea.getText();
            questionsList.getItems().set(idx, newText);
        }
        if (saveStatus != null) {
            saveStatus.setStyle("-fx-text-fill: #00c896; -fx-font-size: 12;");
            saveStatus.setText("âœ“ Question enregistrÃ©e.");
        }
    }

    @FXML public void deleteQuestion() {
        if (questionsList == null) return;
        int idx = questionsList.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                questionsList.getItems().remove(idx);
                clearEditor();
                updateCounters();
            }
        });
    }

    @FXML public void cancelEdit() {
        clearEditor();
        if (questionsList != null) questionsList.getSelectionModel().clearSelection();
        if (editorTitle != null) editorTitle.setText("SÃ©lectionnez une question Ã  modifier");
    }

    @FXML public void previewQuiz() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Quiz.fxml"));
            questionsList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (questionsList != null) questionsList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void clearEditor() {
        if (questionTextArea != null) questionTextArea.clear();
        if (ordreField       != null) ordreField.clear();
        if (rep1Field        != null) rep1Field.clear();
        if (rep2Field        != null) rep2Field.clear();
        if (rep3Field        != null) rep3Field.clear();
        if (rep4Field        != null) rep4Field.clear();
        if (saveStatus       != null) saveStatus.setText("");
    }

    private void updateCounters() {
        int q = questionsList != null ? questionsList.getItems().size() : 0;
        if (countLabel   != null) countLabel.setText("(" + q + ")");
        if (totalQLabel  != null) totalQLabel.setText(String.valueOf(q));
        if (totalRepLabel!= null) totalRepLabel.setText(String.valueOf(q * 4));
    }
}

