package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.utils.feature_cours_et_quiz.AccessControl;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class MatiereCardsController implements NavigableController {
    @FXML private FlowPane cardsPane;
    @FXML private Label errorLabel;
    @FXML private ScrollPane scroll;
    @FXML private Button addBtn;

    private final tn.esprit.services.feature_cours_et_quiz.MatiereCrudService service = new tn.esprit.services.feature_cours_et_quiz.MatiereCrudService();
    private AppShellController appShell;
    private Role role = Role.ETUDIANT;

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
        role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getType());
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
            if (errorLabel != null) errorLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private VBox createCard(Matiere matiere) {
        VBox card = new VBox(15);
        card.getStyleClass().add("sp-card");
        card.setPrefWidth(320);

        Label title = new Label(matiere.getTitre());
        title.getStyleClass().add("sp-card-title");
        title.setWrapText(true);

        String ratingText = matiere.getNbAvis() > 0 
            ? String.format("⭐ %.1f (%d avis)", matiere.getRating(), matiere.getNbAvis())
            : "⭐ Pas encore d'avis";
        Label rating = new Label(ratingText);
        rating.getStyleClass().add("sp-rating-text");
        
        HBox ratingBox = new HBox(rating);
        ratingBox.getStyleClass().add("sp-rating-box");
        ratingBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label desc = new Label(matiere.getDescription() == null ? "" : matiere.getDescription());
        desc.getStyleClass().add("sp-card-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(60);

        VBox actions = new VBox(10);
        actions.getStyleClass().add("sp-card-actions");

        Button viewCours = new Button("📖 Voir cours");
        viewCours.getStyleClass().addAll("sp-btn", "sp-btn-primary");
        viewCours.setMaxWidth(Double.MAX_VALUE);
        viewCours.setOnAction(e -> {
            if (appShell != null) appShell.showMatiereCours(matiere);
        });
        actions.getChildren().add(viewCours);

        if (AccessControl.canManageContent(role)) {
            Button addQuiz = new Button("➕ Ajouter Quiz");
            addQuiz.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
            addQuiz.setMaxWidth(Double.MAX_VALUE);
            addQuiz.setOnAction(e -> {
                if (appShell != null) appShell.showQuizFormForMatiere(matiere);
            });
            actions.getChildren().add(addQuiz);

            Button edit = new Button("✏️ Modifier");
            edit.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
            edit.setMaxWidth(Double.MAX_VALUE);
            edit.setOnAction(e -> {
                if (appShell != null) appShell.showMatiereForm(matiere);
            });
            actions.getChildren().add(edit);
        }

        if (AccessControl.canDelete(role)) {
            Button del = new Button("🗑 Supprimer");
            del.getStyleClass().addAll("sp-btn", "sp-btn-danger");
            del.setMaxWidth(Double.MAX_VALUE);
            del.setOnAction(e -> handleDelete(matiere));
            actions.getChildren().add(del);
        }

        card.getChildren().addAll(title, ratingBox, desc, actions);
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
