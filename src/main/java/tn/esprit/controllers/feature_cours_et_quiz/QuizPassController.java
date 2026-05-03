package tn.esprit.controllers.feature_cours_et_quiz;

import org.json.JSONObject;
import tn.esprit.entity.feature_cours_et_quiz.Question;
import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.services.feature_cours_et_quiz.QuestionService;
import tn.esprit.utils.MyDatabase;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import tn.esprit.controllers.DashboardEtudiantController; // Import for fallback
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizPassController implements NavigableController {
    @FXML private Label quizTitle;
    @FXML private VBox questionsBox;

    private final QuestionService questionService = new QuestionService();
    private AppShellController appShell;
    private Quiz quiz;
    private final Map<Integer, ToggleGroup> userAnswers = new HashMap<>();
    private List<Question> currentQuestions = new ArrayList<>();

    @Override
    public void setAppShell(AppShellController appShell) { this.appShell = appShell; }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        if (quizTitle != null) quizTitle.setText("▶ " + quiz.getTitre());
        loadQuestions();
    }

    @FXML public void handleBack() { if (appShell != null) appShell.showQuiz(); }

    @FXML
    public void handleFinish() {
        int correctCount = 0;
        int total = currentQuestions.size();
        JSONObject responsesJson = new JSONObject();

        for (Question q : currentQuestions) {
            ToggleGroup group = userAnswers.get(q.getId());
            String userChoice = "None";
            if (group != null && group.getSelectedToggle() != null) {
                userChoice = ((RadioButton) group.getSelectedToggle()).getText();
                if (q.getText().contains("(Rép: " + userChoice + ")")) {
                    correctCount++;
                }
            }
            responsesJson.put(String.valueOf(q.getId()), userChoice);
        }

        double finalScore = (total > 0) ? ((double) correctCount / total) * 20 : 0;
        
        System.out.println("[Quiz] Score calculated: " + finalScore + "/20");
        saveResultToDb(finalScore, responsesJson.toString());

        new Alert(Alert.AlertType.INFORMATION, "Quiz Terminé !\nNote : " + finalScore + "/20").showAndWait();
        if (appShell != null) appShell.showQuiz();
    }

    private void saveResultToDb(double score, String responsesJson) {
        // IMPORTANT: Format "QuizTitle:Score" so AI Dashboard can parse it
        String scoreEntry = quiz.getTitre() + ":" + score;
        
        String sql = "INSERT INTO quiz_result (etudiant_id, scores, responses, recommendations, profile_type, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection cnx = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            
            int userId = 0;
            if (AppSession.getCurrentUser() != null) userId = AppSession.getCurrentUser().getId();
            else if (DashboardEtudiantController.getCurrentUser() != null) userId = DashboardEtudiantController.getCurrentUser().getId();
            
            if (userId == 0) {
                System.err.println("[Quiz] Error: No user ID found to save result.");
                return;
            }

            ps.setInt(1, userId);
            ps.setString(2, scoreEntry); // Saving as "QuizTitle:Score"
            ps.setString(3, responsesJson);
            ps.setString(4, "AI analysis available in the dashboard.");
            ps.setString(5, "Educational");
            ps.executeUpdate();
            System.out.println("[Quiz] Result saved successfully: " + scoreEntry);
        } catch (SQLException e) { 
            System.err.println("[Quiz] DB Error: " + e.getMessage());
        }
    }

    private void loadQuestions() {
        questionsBox.getChildren().clear();
        userAnswers.clear();
        try {
            currentQuestions = questionService.getByTestId(quiz.getId());
            int i = 1;
            for (Question q : currentQuestions) {
                VBox card = new VBox(10);
                card.getStyleClass().add("sp-card");
                String displayMsg = q.getText().split("\\(")[0].trim();
                Label qTitle = new Label(i + ". " + displayMsg);
                qTitle.getStyleClass().add("sp-card-title");
                ToggleGroup group = new ToggleGroup();
                RadioButton rbVrai = new RadioButton("Vrai"); rbVrai.setToggleGroup(group);
                RadioButton rbFaux = new RadioButton("Faux"); rbFaux.setToggleGroup(group);
                userAnswers.put(q.getId(), group);
                card.getChildren().addAll(qTitle, new HBox(20, rbVrai, rbFaux));
                questionsBox.getChildren().add(card);
                i++;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
