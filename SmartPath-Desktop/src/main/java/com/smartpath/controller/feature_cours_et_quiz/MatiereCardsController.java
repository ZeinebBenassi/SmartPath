package com.smartpath.controller.feature_cours_et_quiz;

import com.smartpath.model.Matiere;
import com.smartpath.model.feature_cours_et_quiz.Role;
import com.smartpath.service.feature_cours_et_quiz.MatiereCrudService;
import com.smartpath.util.feature_cours_et_quiz.AccessControl;
import com.smartpath.util.feature_cours_et_quiz.RoleUtils;
import com.smartpath.util.feature_cours_et_quiz.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class MatiereCardsController implements NavigableController {
    @FXML private FlowPane cardsPane;
    @FXML private Label errorLabel;
    @FXML private ScrollPane scroll;
    @FXML private Button addBtn;

    private final MatiereCrudService service = new MatiereCrudService();
    private AppShellController appShell;
    private Role role = Role.ETUDIANT;

    @FXML
    public void initialize() {
        errorLabel.setText("");
        role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getRole());
        if (addBtn != null) {
            addBtn.setVisible(AccessControl.canManageContent(role));
            addBtn.setManaged(AccessControl.canManageContent(role));
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
        if (appShell != null) {
            appShell.showMatiereForm(null);
        }
    }

    @FXML
    public void refresh() {
        try {
            List<Matiere> matieres = service.getAll();
            cardsPane.getChildren().clear();
            for (Matiere m : matieres) {
                cardsPane.getChildren().add(createCard(m));
            }
        } catch (SQLException e) {
            errorLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private VBox createCard(Matiere matiere) {
        VBox card = new VBox();
        card.getStyleClass().add("sp-card");

        Label title = new Label(matiere.getTitre());
        title.getStyleClass().add("sp-card-title");

        Label desc = new Label(matiere.getDescription() == null ? "" : matiere.getDescription());
        desc.getStyleClass().add("sp-card-desc");
        desc.setWrapText(true);

        VBox actions = new VBox(8);
        actions.getStyleClass().add("sp-card-actions");

        Button viewCours = new Button("📖 Voir cours");
        viewCours.getStyleClass().addAll("sp-btn", "sp-btn-primary");
        viewCours.setOnAction(e -> {
            if (appShell != null) appShell.showMatiereCours(matiere);
        });
        actions.getChildren().add(viewCours);

        if (AccessControl.canManageContent(role)) {
            Button addQuiz = new Button("➕ Ajouter Quiz");
            addQuiz.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
            addQuiz.setOnAction(e -> {
                if (appShell != null) appShell.showQuizFormForMatiere(matiere);
            });
            actions.getChildren().add(addQuiz);

            Button edit = new Button("✏️ Modifier");
            edit.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
            edit.setOnAction(e -> {
                if (appShell != null) appShell.showMatiereForm(matiere);
            });
            actions.getChildren().add(edit);
        }

        if (AccessControl.canDelete(role)) {
            Button del = new Button("🗑 Supprimer");
            del.getStyleClass().addAll("sp-btn", "sp-btn-danger");
            del.setOnAction(e -> handleDelete(matiere));
            actions.getChildren().add(del);
        }

        card.getChildren().addAll(title, desc, actions);
        return card;
    }

    private void handleDelete(Matiere matiere) {
        if (!AccessControl.canDelete(role)) {
            new Alert(Alert.AlertType.WARNING, "Accès refusé: suppression réservée à ADMIN").showAndWait();
            return;
        }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la matière ?");
        confirm.setContentText(matiere.getTitre());
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(matiere.getId());
                    refresh();
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            }
        });
    }
}
