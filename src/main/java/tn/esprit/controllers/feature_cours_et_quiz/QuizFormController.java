package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.entity.feature_cours_et_quiz.Question;
import tn.esprit.services.feature_cours_et_quiz.QuestionService;
import tn.esprit.services.feature_cours_et_quiz.QuizCrudService;
import tn.esprit.utils.feature_cours_et_quiz.AccessControl;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class QuizFormController implements NavigableController {
    @FXML private Label titleLabel;
    @FXML private Label matiereContextLabel;
    @FXML private TextField titreField;
    @FXML private TextArea contenuField;
    @FXML private TextField dureeField;
    @FXML private Label errorLabel;

    private final QuizCrudService service = new QuizCrudService();
    private final QuestionService questionService = new QuestionService();

    private final Role role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getType());

    private AppShellController appShell;
    private Quiz existing;
    private Integer createMatiereId;
    private String createMatiereTitle;

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
        if (dureeField != null) {
            dureeField.setTextFormatter(new TextFormatter<>(change -> {
                String text = change.getControlNewText();
                return text.matches("\\d*") ? change : null;
            }));
        }
    }

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    public void setExisting(Quiz existing) {
        this.existing = existing;
        if (existing == null) {
            titleLabel.setText("➕ Nouveau Quiz");
            if (matiereContextLabel != null) {
                if (createMatiereId != null && createMatiereId > 0) {
                    String matTitle = (createMatiereTitle == null || createMatiereTitle.isBlank()) ? ("ID " + createMatiereId) : createMatiereTitle;
                    matiereContextLabel.setText("Matière: " + matTitle);
                } else {
                    matiereContextLabel.setText("Matière: sélectionnez une matière d'abord.");
                }
            }
        } else {
            titleLabel.setText("✏️ Modifier Quiz");
            titreField.setText(existing.getTitre());
            contenuField.setText(existing.getContenu());
            dureeField.setText(String.valueOf(existing.getDuree()));
            if (matiereContextLabel != null) {
                matiereContextLabel.setText("Matière ID: " + existing.getMatiereId());
            }
        }
    }

    public void setCreateContext(Matiere matiere) {
        if (matiere == null) {
            createMatiereId = null;
            createMatiereTitle = null;
            return;
        }
        createMatiereId = matiere.getId();
        createMatiereTitle = matiere.getTitre();
    }

    @FXML
    public void handleCancel() {
        if (appShell != null) appShell.showQuiz();
    }

    @FXML
    public void handleSave() {
        if (!AccessControl.canManageContent(role)) {
            if (errorLabel != null) errorLabel.setText("Accès refusé: rôle " + role + " (lecture seule)");
            return;
        }

        String titre = safe(titreField.getText());
        String contenu = safe(contenuField.getText());
        int duree = parseIntOrZero(dureeField.getText());
        int matiereId = existing == null
            ? (createMatiereId == null ? 0 : createMatiereId)
            : existing.getMatiereId();

        if (titre.isBlank()) {
            if (errorLabel != null) errorLabel.setText("Le titre est obligatoire.");
            return;
        }
        if (titre.length() < 3) {
            if (errorLabel != null) errorLabel.setText("Le titre doit contenir au moins 3 caractères.");
            return;
        }
        if (duree <= 0) {
            if (errorLabel != null) errorLabel.setText("La durée doit être un nombre > 0.");
            return;
        }
        if (matiereId <= 0) {
            if (errorLabel != null) errorLabel.setText("Créez le quiz depuis la page Matières (Ajouter Quiz). ");
            return;
        }

        if (contenu.length() > 2000) {
            if (errorLabel != null) errorLabel.setText("Contenu trop long (max 2000 caractères).");
            return;
        }

        try {
            if (existing == null) {
                Quiz q = new Quiz(0, titre, contenu, duree, matiereId);
                int profId = AppSession.getCurrentUser() == null ? 0 : AppSession.getCurrentUser().getId();
                int quizId = service.createAndReturnId(q, profId);
                if (quizId > 0) {
                    createQuestionsFlow(quizId);
                }
            } else {
                existing.setTitre(titre);
                existing.setContenu(contenu);
                existing.setDuree(duree);
                existing.setMatiereId(matiereId);
                service.update(existing);
            }

            if (appShell != null) appShell.showQuiz();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void createQuestionsFlow(int quizId) {
        int profId = AppSession.getCurrentUser() == null ? 0 : AppSession.getCurrentUser().getId();
        int count = askQuestionCount();
        if (count <= 0) {
            return;
        }

        for (int i = 1; i <= count; i++) {
            Optional<QuestionDraft> draftOpt = askOneQuestion(i, count);
            if (draftOpt.isEmpty()) {
                break;
            }

            QuestionDraft draft = draftOpt.get();
            if (draft.text.isBlank()) {
                i--; // retry same index
                continue;
            }

            try {
                Question q = new Question();
                q.setText(draft.text);
                q.setCategory(draft.category);
                q.setOrdre(i);
                q.setActive(true);
                q.setTestId(quizId);
                questionService.createForTestId(quizId, q, profId);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur création question: " + e.getMessage()).showAndWait();
                break;
            }
        }
    }

    private int askQuestionCount() {
        var dialog = new javafx.scene.control.TextInputDialog("5");
        dialog.setTitle("Questions");
        dialog.setHeaderText("Combien de questions pour ce quiz ?");
        dialog.setContentText("Nombre:");

        Optional<String> value = dialog.showAndWait();
        if (value.isEmpty()) return 0;

        try {
            int n = Integer.parseInt(value.get().trim());
            return Math.max(0, Math.min(n, 50));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Optional<QuestionDraft> askOneQuestion(int index, int total) {
        Dialog<QuestionDraft> dialog = new Dialog<>();
        dialog.setTitle("Question " + index + "/" + total);
        dialog.setHeaderText("Saisissez la question (une par une)");

        ButtonType save = new ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextArea text = new TextArea();
        text.setPromptText("Texte de la question...");
        text.setWrapText(true);
        text.setPrefRowCount(4);

        TextField category = new TextField();
        category.setPromptText("Catégorie (optionnel)");

        VBox box = new VBox(10, text, category);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == save) {
                return new QuestionDraft(safe(text.getText()), safe(category.getText()));
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private record QuestionDraft(String text, String category) {}

    private static int parseIntOrZero(String s) {
        if (s == null || s.trim().isBlank()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
