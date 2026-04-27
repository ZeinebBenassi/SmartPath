package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Question;
import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.services.feature_cours_et_quiz.QuestionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuizPassController implements NavigableController {
    @FXML private Label quizTitle;
    @FXML private Label quizMeta;
    @FXML private Label errorLabel;
    @FXML private VBox questionsBox;

    private final QuestionService questionService = new QuestionService();

    private AppShellController appShell;
    private Quiz quiz;
    private final List<TextArea> answerInputs = new ArrayList<>();

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        if (quizTitle != null) {
            quizTitle.setText(quiz == null ? "▶ Passer le quiz" : "▶ " + quiz.getTitre());
        }
        if (quizMeta != null && quiz != null) {
            quizMeta.setText("⏱ " + quiz.getDuree() + " min  •  Matière ID: " + quiz.getMatiereId());
        }
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        loadQuestions();
    }

    @FXML
    public void handleBack() {
        if (appShell != null) {
            appShell.showQuiz();
        }
    }

    @FXML
    public void handleFinish() {
        int total = answerInputs.size();
        long answered = answerInputs.stream()
                .filter(a -> a.getText() != null && !a.getText().trim().isEmpty())
                .count();

        new Alert(Alert.AlertType.INFORMATION,
                "Quiz terminé. Réponses: " + answered + "/" + total + ".")
                .showAndWait();

        if (appShell != null) {
            appShell.showQuiz();
        }
    }

    private void loadQuestions() {
        if (questionsBox == null) {
            return;
        }
        questionsBox.getChildren().clear();
        answerInputs.clear();

        if (quiz == null) {
            return;
        }

        try {
            List<Question> questions = questionService.getByTestId(quiz.getId());
            if (questions.isEmpty()) {
                VBox empty = new VBox(10);
                empty.getStyleClass().add("sp-card");

                Label title = new Label("Aucune question");
                title.getStyleClass().add("sp-card-title");

                Label desc = new Label("Ce quiz ne contient pas encore de questions.");
                desc.getStyleClass().add("sp-card-desc");
                desc.setWrapText(true);

                empty.getChildren().addAll(title, desc);
                questionsBox.getChildren().add(empty);
                return;
            }

            int i = 1;
            for (Question q : questions) {
                VBox card = new VBox(10);
                card.getStyleClass().add("sp-card");

                Label qTitle = new Label("Q" + i + ". " + (q.getText() == null ? "" : q.getText()));
                qTitle.getStyleClass().add("sp-card-title");
                qTitle.setWrapText(true);

                TextArea answer = new TextArea();
                answer.setPromptText("Votre réponse...");
                answer.setWrapText(true);

                answerInputs.add(answer);
                card.getChildren().addAll(qTitle, answer);
                questionsBox.getChildren().add(card);
                i++;
            }
        } catch (SQLException e) {
            if (errorLabel != null) {
                errorLabel.setText("Erreur: " + e.getMessage());
            }
        }
    }
}
