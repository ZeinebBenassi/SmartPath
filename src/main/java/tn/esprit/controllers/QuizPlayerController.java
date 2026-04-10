package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entity.Answer;
import tn.esprit.entity.Question;
import tn.esprit.entity.QuizResult;
import services.QuizAnalyzer;
import services.QuizService;
import models.User;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class QuizPlayerController implements Initializable {

    @FXML private Label       lblQuestionNumber;
    @FXML private Label       lblProgress;
    @FXML private ProgressBar progressBar;
    @FXML private Label       lblQuestionText;
    @FXML private VBox        vboxAnswers;
    @FXML private Button      btnPrev;
    @FXML private Button      btnNext;
    @FXML private Label       lblCategory;

    private final QuizService   quizService  = new QuizService();
    private final QuizAnalyzer  quizAnalyzer = new QuizAnalyzer();
    private List<Question>      questions    = new ArrayList<>();
    private int                 currentIndex = 0;
    private final Map<Integer, Integer> selectedAnswers = new LinkedHashMap<>();

    // Étudiant connecté (transmis depuis le dashboard)
    private static User currentUser;
    public static void setCurrentUser(User u) { currentUser = u; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        questions = quizService.findActiveQuestionsForQuiz();
        if (questions.isEmpty()) { showNoQuestions(); return; }
        showQuestion(0);
    }

    private void showQuestion(int index) {
        currentIndex = index;
        Question q   = questions.get(index);

        lblQuestionNumber.setText("Question " + (index + 1) + " / " + questions.size());
        lblProgress.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress((double)(index + 1) / questions.size());
        lblQuestionText.setText(q.getText());
        lblCategory.setText("📂 " + q.getCategory());

        btnPrev.setDisable(index == 0);
        boolean isLast = index == questions.size() - 1;
        btnNext.setText(isLast ? "✅  Terminer" : "Suivant  →");

        vboxAnswers.getChildren().clear();
        ToggleGroup group = new ToggleGroup();

        List<Answer> answers = quizService.findAnswersByQuestion(q.getId());
        for (Answer a : answers) {
            RadioButton rb2 = new RadioButton(a.getText());
            rb2.setToggleGroup(group);
            rb2.setUserData(a.getId());
            rb2.getStyleClass().add("answer-radio");
            rb2.setWrapText(true);

            if (selectedAnswers.containsKey(q.getId()) &&
                selectedAnswers.get(q.getId()) == a.getId()) {
                rb2.setSelected(true);
            }

            rb2.selectedProperty().addListener((obs, was, now) -> {
                if (now) {
                    selectedAnswers.put(q.getId(), (int) rb2.getUserData());
                    btnNext.setDisable(false);
                }
            });
            vboxAnswers.getChildren().add(rb2);
        }

        btnNext.setDisable(!selectedAnswers.containsKey(q.getId()));

        FadeTransition ft = new FadeTransition(Duration.millis(300), vboxAnswers);
        ft.setFromValue(0.3); ft.setToValue(1.0); ft.play();
    }

    @FXML private void handleNext() {
        if (currentIndex == questions.size() - 1) submitQuiz();
        else showQuestion(currentIndex + 1);
    }

    @FXML private void handlePrev() {
        if (currentIndex > 0) showQuestion(currentIndex - 1);
    }

    private void submitQuiz() {
        if (selectedAnswers.size() < questions.size()) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez répondre à toutes les questions avant de terminer.", ButtonType.OK).showAndWait();
            return;
        }

        List<Answer> chosen = new ArrayList<>();
        for (int answerId : selectedAnswers.values()) {
            Answer a = quizService.findAnswerById(answerId);
            if (a != null) chosen.add(a);
        }

        Map<String, Object> analysis = quizAnalyzer.analyzeResponses(chosen);
        @SuppressWarnings("unchecked")
        Map<String, Integer> scores = (Map<String, Integer>) analysis.get("scores");
        String profileType          = (String) analysis.get("profileType");

        QuizResult result = new QuizResult();
        // Utiliser l'id de l'étudiant connecté si disponible
        result.setEtudiantId(currentUser != null ? currentUser.getId() : 1);
        result.setResponses(quizAnalyzer.responsesToJson(new ArrayList<>(selectedAnswers.values())));
        result.setScores(quizAnalyzer.scoresToJson(scores));
        result.setProfileType(profileType);
        result.setRecommendations("[]");
        quizService.saveQuizResult(result);

        openResultPage(result, scores, profileType);
    }

    private void openResultPage(QuizResult result, Map<String, Integer> scores, String profileType) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/interfaces/QuizResult.fxml"));
            Parent root = loader.load();
            QuizResultController ctrl = loader.getController();
            ctrl.initData(result, scores, profileType);

            Stage stage = (Stage) btnNext.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SmartPath — Résultats du Quiz");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les résultats : " + e.getMessage()).show();
        }
    }

    private void showNoQuestions() {
        vboxAnswers.getChildren().clear();
        Label lbl = new Label("⚠ Aucune question active trouvée.\nVeuillez en ajouter dans l'admin.");
        lbl.getStyleClass().add("no-data-label");
        lbl.setWrapText(true);
        vboxAnswers.getChildren().add(lbl);
        btnNext.setDisable(true);
        btnPrev.setDisable(true);
        lblQuestionText.setText("Quiz indisponible");
    }
}
