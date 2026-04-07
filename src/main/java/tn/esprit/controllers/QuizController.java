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

    // Questions du quiz d'orientation
    private static final String[] QUESTIONS = {
        "Quel type d'activités vous attire le plus ?",
        "Vous préférez travailler :",
        "Dans un projet, vous prenez plutôt le rôle de :",
        "Quelle matière préférez-vous ?",
        "Votre environnement de travail idéal ?",
        "Qu'est-ce qui vous motive le plus ?",
        "Votre rapport aux nouvelles technologies ?",
        "Comment gérez-vous un problème complexe ?",
        "Votre style de communication préféré ?",
        "Votre objectif professionnel principal ?"
    };

    private static final String[][] ANSWERS = {
        {"Analyser des données et résoudre des problèmes logiques", "Créer des designs et interfaces visuelles", "Gérer des équipes et des projets", "Comprendre et aider les gens"},
        {"Seul(e) sur des tâches précises", "En équipe avec des échanges réguliers", "En autonomie avec suivi de résultats", "En contact direct avec des clients"},
        {"Le technicien qui implémente", "Le créatif qui conçoit", "Le leader qui organise", "Le médiateur qui coordonne"},
        {"Mathématiques / Informatique", "Arts / Design", "Management / Économie", "Sciences humaines"},
        {"Bureau avec des outils tech", "Studio créatif", "Open space dynamique", "Terrain / contact humain"},
        {"Résoudre un bug complexe", "Créer quelque chose de beau", "Atteindre des objectifs", "Aider et impacter positivement"},
        {"J'adore programmer et automatiser", "Je les utilise pour créer", "Je les pilote pour la stratégie", "Je les utilise pour communiquer"},
        {"J'analyse méthodiquement", "Je cherche des solutions créatives", "Je délègue et coordonne", "Je consulte les autres"},
        {"Écrit / documentation", "Visuel / présentations", "Oral / réunions", "Écoute / accompagnement"},
        {"Expert technique reconnu", "Directeur artistique", "Manager / entrepreneur", "Consultant / formateur"}
    };

    @FXML
    public void initialize() {
        scores = new int[4]; // [Tech, Design, Management, Humain]
        showIntro();
    }

    private void showIntro() {
        setVisible(introView, true);
        setVisible(questionView, false);
        setVisible(resultView, false);
    }

    @FXML public void startQuiz() {
        currentIndex = 0;
        scores = new int[4];
        setVisible(introView, false);
        setVisible(questionView, true);
        setVisible(resultView, false);
        loadQuestion();
    }

    private void loadQuestion() {
        if (currentIndex >= QUESTIONS.length) { showResults(); return; }

        questionNum.setText("Question " + (currentIndex + 1));
        questionText.setText(QUESTIONS[currentIndex]);
        if (progressLabel != null)
            progressLabel.setText("Question " + (currentIndex + 1) + " / " + QUESTIONS.length);
        if (progressBar != null)
            progressBar.setProgress((double)(currentIndex + 1) / QUESTIONS.length);

        answersBox.getChildren().clear();
        String[] opts = ANSWERS[currentIndex];
        ToggleGroup group = new ToggleGroup();
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            RadioButton rb = new RadioButton(opts[i]);
            rb.setToggleGroup(group);
            rb.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-padding: 14 20; -fx-background-color: #16213e; -fx-background-radius: 10; -fx-cursor: hand;");
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setOnAction(e -> {
                // highlight selected
                for (javafx.scene.Node n : answersBox.getChildren()) {
                    n.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-padding: 14 20; -fx-background-color: #16213e; -fx-background-radius: 10; -fx-cursor: hand;");
                }
                rb.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-padding: 14 20; -fx-background-color: #2d1a5e; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #7c6af7; -fx-border-radius: 10; -fx-border-width: 1.5;");
                scores[idx]++;
            });
            answersBox.getChildren().add(rb);
        }

        if (btnPrev != null) btnPrev.setDisable(currentIndex == 0);
        if (btnNext != null) btnNext.setText(currentIndex == QUESTIONS.length - 1 ? "Terminer ✓" : "Suivant →");
    }

    @FXML public void nextQuestion() {
        currentIndex++;
        if (currentIndex >= QUESTIONS.length) showResults();
        else loadQuestion();
    }

    @FXML public void prevQuestion() {
        if (currentIndex > 0) { currentIndex--; loadQuestion(); }
    }

    private void showResults() {
        setVisible(questionView, false);
        setVisible(resultView, true);

        int maxScore = 0, maxIdx = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) { maxScore = scores[i]; maxIdx = i; }
        }

        String[] profiles = {"Profil Technologique", "Profil Créatif", "Profil Management", "Profil Humain"};
        String[] recs = {
            "Génie Informatique\nDéveloppement Logiciel\nData Science\nCybersécurité",
            "Design Graphique\nMultimédia\nArchitecture\nUX/UI Design",
            "Management\nMarketing\nFinance\nLogistique",
            "Psychologie\nSociologie\nFormation\nRessources Humaines"
        };

        if (profileTypeLabel != null) profileTypeLabel.setText(profiles[maxIdx]);
        if (recommendationsList != null) {
            recommendationsList.getItems().setAll(recs[maxIdx].split("\n"));
        }
    }

    @FXML public void restartQuiz() { startQuiz(); }

    @FXML public void voirProfil() {
        navigate("/views/Profil.fxml");
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardEtudiant.fxml"));
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

