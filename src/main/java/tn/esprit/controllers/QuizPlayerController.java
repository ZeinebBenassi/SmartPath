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
import tn.esprit.entity.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.QuizAnalyzer;
import tn.esprit.services.QuizService;

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

    private final QuizService         quizService          = new QuizService();
    private final QuizAnalyzer         quizAnalyzer         = new QuizAnalyzer();
    private final NotificationService  notificationService  = new NotificationService();

    private List<Question>              questions       = new ArrayList<>();
    private int                         currentIndex    = 0;
    private final Map<Integer, Integer> selectedAnswers = new LinkedHashMap<>();

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
            if (selectedAnswers.containsKey(q.getId()) && selectedAnswers.get(q.getId()) == a.getId())
                rb2.setSelected(true);
            rb2.selectedProperty().addListener((obs, was, now) -> {
                if (now) { selectedAnswers.put(q.getId(), (int) rb2.getUserData()); btnNext.setDisable(false); }
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

    // ------------------------------------------------------------------ //
    //  Soumission — analyse + sauvegarde + navigation vers résultats     //
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    private void submitQuiz() {
        if (selectedAnswers.size() < questions.size()) {
            new Alert(Alert.AlertType.WARNING,
                "Veuillez répondre à toutes les questions.", ButtonType.OK).showAndWait();
            return;
        }

        // 1) Récupérer les Answer objects
        List<Answer> chosen = new ArrayList<>();
        for (int answerId : selectedAnswers.values()) {
            Answer a = quizService.findAnswerById(answerId);
            if (a != null) chosen.add(a);
        }

        // 2) Analyser (scores, profil, recommandations filières)
        Map<String, Object>        analysis = quizAnalyzer.analyzeResponses(chosen);
        Map<String, Integer>       scores   = (Map<String, Integer>) analysis.get("scores");
        String                     profile  = (String) analysis.get("profileType");
        List<Map<String, Object>>  recs     = (List<Map<String, Object>>) analysis.get("recommendations");

        // 3) Sauvegarder le résultat en BDD
        QuizResult result = new QuizResult();
        result.setEtudiantId(currentUser != null ? currentUser.getId() : 1);
        result.setResponses(quizAnalyzer.responsesToJson(new ArrayList<>(selectedAnswers.values())));
        result.setScores(quizAnalyzer.scoresToJson(scores));
        result.setProfileType(profile);
        result.setRecommendations(quizAnalyzer.recommendationsToJson(recs));
        quizService.saveQuizResult(result);

        // 4) Envoyer une notification à l'admin
        if (currentUser != null) {
            notificationService.create(
                currentUser.getId(),
                currentUser.getNom(),
                currentUser.getPrenom(),
                profile
            );
        }

        // 5) Ouvrir la page résultats
        //    → Les universités seront chargées via API Groq directement dans QuizResultController
        openResultPage(result, scores, profile, recs);
    }

    private void openResultPage(QuizResult result,
                                Map<String, Integer> scores,
                                String profileType,
                                List<Map<String, Object>> recommendations) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/interfaces/QuizResult.fxml"));
            Parent root = loader.load();
            QuizResultController ctrl = loader.getController();
            ctrl.initData(result, scores, profileType, recommendations);
            Stage stage = (Stage) btnNext.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SmartPath — Résultats du Quiz");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                "Impossible d'ouvrir les résultats : " + e.getMessage()).show();
        }
    }

    private void showNoQuestions() {
        vboxAnswers.getChildren().clear();
        Label lbl = new Label("⚠ Aucune question active trouvée.");
        lbl.setWrapText(true);
        vboxAnswers.getChildren().add(lbl);
        btnNext.setDisable(true); btnPrev.setDisable(true);
        lblQuestionText.setText("Quiz indisponible");
    }
}
