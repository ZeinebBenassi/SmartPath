package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entity.QcmQuestion;
import org.example.entity.QcmReponse;
import org.example.entity.Test;
import org.example.service.TestService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizQuestionController {

    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox answersBox;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private TestService testService = new TestService();
    private Test currentTest;
    private int currentQuestionIndex = 0;
    private Map<Integer, Integer> selectedAnswers = new HashMap<>();

    public void initData(int testId) {
        currentTest = testService.findWithDetails(testId);
        if (currentTest != null) {
            displayQuestion();
        }
    }

    private void displayQuestion() {
        List<QcmQuestion> questions = currentTest.getQcmQuestions();
        if (questions == null || questions.isEmpty()) return;

        QcmQuestion q = questions.get(currentQuestionIndex);
        
        int total = questions.size();
        double progress = (double)(currentQuestionIndex + 1) / total;
        progressBar.setProgress(progress);
        progressLabel.setText("Question " + (currentQuestionIndex + 1) + " sur " + total);
        questionNumberLabel.setText("QUESTION " + (currentQuestionIndex + 1) + " / " + total);
        questionTextLabel.setText(q.getTexte());

        prevBtn.setVisible(currentQuestionIndex > 0);
        nextBtn.setText(currentQuestionIndex == total - 1 ? "Terminer ✓" : "Suivant →");
        nextBtn.setDisable(!selectedAnswers.containsKey(q.getId()));

        answersBox.getChildren().clear();
        ToggleGroup group = new ToggleGroup();
        
        for (QcmReponse r : q.getReponses()) {
            RadioButton rb = new RadioButton(r.getTexte());
            rb.setToggleGroup(group);
            rb.getStyleClass().add("answer-option");
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setPrefHeight(60);
            
            if (selectedAnswers.containsKey(q.getId()) && selectedAnswers.get(q.getId()) == r.getId()) {
                rb.setSelected(true);
            }

            rb.setOnAction(e -> {
                selectedAnswers.put(q.getId(), r.getId());
                nextBtn.setDisable(false);
            });

            answersBox.getChildren().add(rb);
        }
    }

    @FXML
    private void handleNext(javafx.event.ActionEvent event) {
        if (currentQuestionIndex < currentTest.getQcmQuestions().size() - 1) {
            currentQuestionIndex++;
            displayQuestion();
        } else {
            handleSubmit(event);
        }
    }

    @FXML
    private void handlePrev() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayQuestion();
        }
    }

    private void handleSubmit(javafx.event.ActionEvent event) {
        // Calculate score
        float correctAnswers = 0;
        List<QcmQuestion> questions = currentTest.getQcmQuestions();
        for (QcmQuestion q : questions) {
            Integer selectedId = selectedAnswers.get(q.getId());
            if (selectedId != null) {
                for (QcmReponse r : q.getReponses()) {
                    if (r.getId() == selectedId && r.isEstCorrecte()) {
                        correctAnswers++;
                        break;
                    }
                }
            }
        }
        float score = (correctAnswers / questions.size()) * 20;
        
        // Save to Database (Mock etudiant_id = 1)
        testService.submitResult(1, currentTest.getId(), score);

        // Show Success Alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quiz Terminé");
        alert.setHeaderText("Félicitations !");
        alert.setContentText(String.format("Votre score est : %.2f / 20", score));
        alert.showAndWait();

        // Redirect back to Matiere Details
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/matiere_details.fxml"));
            Parent root = loader.load();
            MatiereDetailController detailController = loader.getController();
            detailController.initData(currentTest.getMatiereId());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
