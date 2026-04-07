package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class QuizController {

    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label questionNum;
    @FXML private Label questionText;
    @FXML private VBox answersBox;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label profileTypeLabel;
    @FXML private ListView<String> recommendationsList;

    @FXML private VBox introView;
    @FXML private VBox questionView;
    @FXML private VBox resultView;

    private int currentIndex = 0;
    private int[] scores;

    @FXML
    public void initialize() {
        scores = new int[4];
        showIntro();
    }

    private void showIntro() {
        setVisible(introView, true);
        setVisible(questionView, false);
        setVisible(resultView, false);
        if (progressLabel != null) {
            progressLabel.setText("Aucune donnée disponible");
        }
        if (answersBox != null) {
            answersBox.getChildren().clear();
        }
    }

    @FXML public void startQuiz() {
        if (progressLabel != null) {
            progressLabel.setText("Quiz indisponible");
        }
        if (introView != null) {
            introView.setVisible(true);
            introView.setManaged(true);
        }
        if (questionView != null) {
            questionView.setVisible(false);
            questionView.setManaged(false);
        }
        if (resultView != null) {
            resultView.setVisible(false);
            resultView.setManaged(false);
        }
        showUnavailableMessage();
    }

    private void showUnavailableMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
            "Le quiz d'orientation n'est pas encore disponible.", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML public void nextQuestion() { }

    @FXML public void prevQuestion() { }

    @FXML public void restartQuiz() { startQuiz(); }

    @FXML public void voirProfil() {
        navigate("/tn/esprit/interfaces/Profil.fxml");
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            progressLabel.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            progressLabel.getScene().setRoot(root);
        } catch (Exception e) { System.out.println("Nav " + fxml + " : " + e.getMessage()); }
    }

    private void setVisible(VBox v, boolean val) {
        if (v != null) { v.setVisible(val); v.setManaged(val); }
    }
}

