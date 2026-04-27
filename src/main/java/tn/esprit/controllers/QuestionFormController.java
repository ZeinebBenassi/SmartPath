package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entity.Answer;
import tn.esprit.entity.Question;
import tn.esprit.services.QuestionService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class QuestionFormController implements Initializable {

    @FXML private Label             lblTitle;
    @FXML private TextArea          txtText;
    @FXML private ComboBox<String>  cbCategory;
    @FXML private Spinner<Integer>  spOrdre;
    @FXML private CheckBox          chkActive;
    @FXML private VBox              vboxAnswers;
    @FXML private Button            btnSave;
    @FXML private Button            btnCancel;

    private final TextField[]         answerTexts    = new TextField[4];
    @SuppressWarnings("unchecked")
    private final Spinner<Integer>[]   answerSpinners = new Spinner[4];
    @SuppressWarnings("unchecked")
    private final ComboBox<String>[]   answerTraits   = new ComboBox[4];

    private final QuestionService  questionService = new QuestionService();
    private QuestionController     parentController;
    private Question               currentQuestion;

    private static final String[] CATEGORIES = {
        "analytique","pratique","creatif","technique",
        "mathematique","algorithmique","systemes","reseaux","securite","donnees"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategory.getItems().addAll(CATEGORIES);
        spOrdre.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        buildAnswerFields();
    }

    @SuppressWarnings("unchecked")
    private void buildAnswerFields() {
        String[] letterLabels = {"A","B","C","D"};
        for (int i = 0; i < 4; i++) {
            Label lbl = new Label("Réponse " + letterLabels[i] + "  *");
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6366F1;");

            answerTexts[i] = new TextField();
            answerTexts[i].setPromptText("Texte de la réponse " + letterLabels[i]);
            answerTexts[i].setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;" +
                    "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;" +
                    "-fx-padding: 8 12; -fx-font-size: 13px;");
            answerTexts[i].setMaxWidth(Double.MAX_VALUE);

            answerSpinners[i] = new Spinner<>();
            answerSpinners[i].setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 5));
            answerSpinners[i].setPrefWidth(90);
            answerSpinners[i].setEditable(true);

            answerTraits[i] = new ComboBox<>();
            answerTraits[i].getItems().addAll(CATEGORIES);
            answerTraits[i].setPromptText("Trait de personnalité");
            answerTraits[i].setPrefWidth(170);

            HBox rowMeta = new HBox(8, new Label("Points :"), answerSpinners[i], new Label("Trait :"), answerTraits[i]);
            rowMeta.setAlignment(Pos.CENTER_LEFT);

            VBox block = new VBox(6, lbl, answerTexts[i], rowMeta);
            block.setPadding(new Insets(10, 14, 10, 14));
            block.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10;" +
                           "-fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1;");
            VBox.setMargin(block, new Insets(0, 0, 4, 0));
            vboxAnswers.getChildren().add(block);
        }
    }

    public void initData(Question question, QuestionController parent) {
        this.parentController = parent;
        this.currentQuestion  = question;

        if (question == null) {
            lblTitle.setText("➕  Nouvelle Question");
            chkActive.setSelected(true);
        } else {
            lblTitle.setText("✏  Modifier la Question");
            txtText.setText(question.getText());
            cbCategory.setValue(question.getCategory());
            spOrdre.getValueFactory().setValue(question.getOrdre());
            chkActive.setSelected(question.isActive());
            List<Answer> answers = question.getAnswers();
            for (int i = 0; i < Math.min(4, answers.size()); i++) {
                Answer a = answers.get(i);
                answerTexts[i].setText(a.getText());
                answerSpinners[i].getValueFactory().setValue(a.getPoints());
                answerTraits[i].setValue(a.getTrait());
            }
        }
    }

    @FXML private void handleSave() {
        if (txtText.getText() == null || txtText.getText().trim().isEmpty()) {
            showWarning("Le texte de la question est obligatoire."); txtText.requestFocus(); return;
        }
        if (txtText.getText().trim().length() < 10) {
            showWarning("Le texte doit contenir au moins 10 caractères."); txtText.requestFocus(); return;
        }
        if (cbCategory.getValue() == null) { showWarning("Sélectionnez une catégorie."); return; }

        for (int i = 0; i < 4; i++) {
            if (answerTexts[i].getText() == null || answerTexts[i].getText().trim().isEmpty()) {
                showWarning("Le texte de la réponse " + (char)('A'+i) + " est obligatoire.");
                answerTexts[i].requestFocus(); return;
            }
            if (answerTraits[i].getValue() == null) {
                showWarning("Sélectionnez un trait pour la réponse " + (char)('A'+i) + "."); return;
            }
        }

        Question q = (currentQuestion == null) ? new Question() : currentQuestion;
        q.setText(txtText.getText().trim());
        q.setCategory(cbCategory.getValue());
        q.setOrdre(spOrdre.getValue());
        q.setActive(chkActive.isSelected());

        List<Answer> answers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Answer a = new Answer();
            if (currentQuestion != null && i < currentQuestion.getAnswers().size())
                a.setId(currentQuestion.getAnswers().get(i).getId());
            a.setText(answerTexts[i].getText().trim());
            a.setPoints(answerSpinners[i].getValue());
            a.setTrait(answerTraits[i].getValue());
            answers.add(a);
        }
        q.setAnswers(answers);

        try {
            if (currentQuestion == null) { questionService.ajouter(q); showInfo("Question ajoutée !"); }
            else                         { questionService.modifier(q); showInfo("Question modifiée !"); }
            closeWindow();
        } catch (IllegalArgumentException ex) { showWarning(ex.getMessage()); }
        catch (SQLException ex) { showError("Erreur BDD : " + ex.getMessage()); }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() { btnCancel.getScene().getWindow().hide(); }
    private void showWarning(String msg) { Alert a = new Alert(Alert.AlertType.WARNING); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
    private void showError(String msg)   { Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
    private void showInfo(String msg)    { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
}
