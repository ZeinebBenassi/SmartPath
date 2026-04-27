package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * QuizController — pont entre DashboardEtudiant et QuizPlayer.
 * Quand l'étudiant clique "Commencer", ouvre le vrai QuizPlayer (BDD réelle).
 */
public class QuizController {

    @FXML private Label       progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label       questionNum;
    @FXML private Label       questionText;
    @FXML private VBox        answersBox;
    @FXML private Button      btnPrev;
    @FXML private Button      btnNext;
    @FXML private Label       profileTypeLabel;
    @FXML private ListView<String> recommendationsList;

    @FXML private VBox introView;
    @FXML private VBox questionView;
    @FXML private VBox resultView;

    @FXML
    public void initialize() {
        showIntro();
    }

    private void showIntro() {
        setVisible(introView, true);
        setVisible(questionView, false);
        setVisible(resultView, false);
    }

    /**
     * Lance le vrai QuizPlayer.fxml dans la fenêtre courante.
     */
    @FXML public void startQuiz() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/interfaces/QuizPlayer.fxml"));
            Parent root = loader.load();

            // Récupérer la scène courante
            if (progressLabel != null && progressLabel.getScene() != null) {
                Stage stage = (Stage) progressLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("SmartPath — Quiz de Personnalité");
                stage.setMinWidth(900);
                stage.setMinHeight(650);
            } else if (introView != null && introView.getScene() != null) {
                Stage stage = (Stage) introView.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("SmartPath — Quiz de Personnalité");
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossible de lancer le quiz : " + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    @FXML public void nextQuestion() {}
    @FXML public void prevQuestion() {}
    @FXML public void restartQuiz()  { startQuiz(); }

    @FXML public void voirProfil() {
        navigate("/tn/esprit/interfaces/Profil.fxml");
    }

    @FXML public void goBack() {
        navigate("/tn/esprit/interfaces/DashboardEtudiant.fxml");
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            if (progressLabel != null && progressLabel.getScene() != null)
                progressLabel.getScene().setRoot(root);
            else if (introView != null && introView.getScene() != null)
                introView.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println("Nav " + fxml + " : " + e.getMessage());
        }
    }

    private void setVisible(VBox v, boolean val) {
        if (v != null) { v.setVisible(val); v.setManaged(val); }
    }
}
