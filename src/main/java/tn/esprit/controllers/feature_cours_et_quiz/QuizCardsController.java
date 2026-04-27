package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.services.feature_cours_et_quiz.QuizCrudService;
import tn.esprit.utils.feature_cours_et_quiz.AccessControl;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class QuizCardsController implements NavigableController {
    @FXML private FlowPane cardsPane;
    @FXML private Label errorLabel;
    @FXML private Button addBtn;

    private final QuizCrudService service = new QuizCrudService();
    private AppShellController appShell;
    private Role role = Role.ETUDIANT;

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
        role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getType());
        if (addBtn != null) {
            addBtn.setVisible(false);
            addBtn.setManaged(false);
        }
        refresh();
    }

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    @FXML
    public void handleAdd() {
        if (!AccessControl.canManageContent(role)) {
            new Alert(Alert.AlertType.WARNING, "Accès refusé: rôle " + role + " (lecture seule)").showAndWait();
            return;
        }
        new Alert(Alert.AlertType.INFORMATION,
                "Créez un quiz depuis la page Matières (bouton 'Ajouter Quiz' dans une matière).")
                .showAndWait();
        if (appShell != null) appShell.showMatieres();
    }

    @FXML
    public void refresh() {
        try {
            List<Quiz> quizzes = service.getAll();
            cardsPane.getChildren().clear();
            for (Quiz q : quizzes) {
                cardsPane.getChildren().add(createCard(q));
            }
        } catch (SQLException e) {
            if (errorLabel != null) errorLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private VBox createCard(Quiz quiz) {
        VBox card = new VBox();
        card.getStyleClass().add("sp-card");

        Label title = new Label(quiz.getTitre());
        title.getStyleClass().add("sp-card-title");

        Label meta = new Label("⏱ " + quiz.getDuree() + " min  •  Matière ID: " + quiz.getMatiereId());
        meta.getStyleClass().add("sp-card-meta");

        Label content = new Label(quiz.getContenu() == null ? "" : quiz.getContenu());
        content.getStyleClass().add("sp-card-desc");
        content.setWrapText(true);

        VBox actions = new VBox(8);
        actions.getStyleClass().add("sp-card-actions");

        Button pass = new Button("▶ Passer le quiz");
        pass.getStyleClass().addAll("sp-btn", "sp-btn-primary");
        pass.setOnAction(e -> {
            if (appShell != null) appShell.showQuizPass(quiz);
        });
        actions.getChildren().add(pass);

        if (AccessControl.canManageContent(role)) {
            Button edit = new Button("✏️ Modifier");
            edit.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
            edit.setOnAction(e -> {
                if (appShell != null) appShell.showQuizForm(quiz);
            });
            actions.getChildren().add(edit);
        }

        if (AccessControl.canDelete(role)) {
            Button del = new Button("🗑 Supprimer");
            del.getStyleClass().addAll("sp-btn", "sp-btn-danger");
            del.setOnAction(e -> handleDelete(quiz));
            actions.getChildren().add(del);
        }

        card.getChildren().addAll(title, meta, content, actions);
        return card;
    }

    private void handleDelete(Quiz quiz) {
        if (!AccessControl.canDelete(role)) {
            new Alert(Alert.AlertType.WARNING, "Accès refusé: suppression réservée à ADMIN").showAndWait();
            return;
        }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le quiz ?");
        confirm.setContentText(quiz.getTitre());
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(quiz.getId());
                    refresh();
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        });
    }
}
