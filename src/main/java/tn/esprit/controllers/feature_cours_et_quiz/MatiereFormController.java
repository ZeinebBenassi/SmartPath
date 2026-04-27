package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.services.feature_cours_et_quiz.MatiereCrudService;
import tn.esprit.utils.feature_cours_et_quiz.AccessControl;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class MatiereFormController implements NavigableController {
    @FXML private Label titleLabel;
    @FXML private TextField titreField;
    @FXML private TextArea descField;
    @FXML private Label errorLabel;

    private final MatiereCrudService service = new MatiereCrudService();

    private final Role role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getType());

    private AppShellController appShell;
    private Matiere existing;

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
    }

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    public void setExisting(Matiere existing) {
        this.existing = existing;
        if (existing == null) {
            titleLabel.setText("➕ Nouvelle Matière");
        } else {
            titleLabel.setText("✏️ Modifier Matière");
            titreField.setText(existing.getTitre());
            descField.setText(existing.getDescription());
        }
    }

    @FXML
    public void handleCancel() {
        if (appShell != null) appShell.showMatieres();
    }

    @FXML
    public void handleSave() {
        if (!AccessControl.canManageContent(role)) {
            if (errorLabel != null) errorLabel.setText("Accès refusé: rôle " + role + " (lecture seule)");
            return;
        }

        String titre = safe(titreField.getText());
        String desc = safe(descField.getText());

        if (titre.isBlank()) {
            if (errorLabel != null) errorLabel.setText("Le titre est obligatoire.");
            return;
        }
        if (titre.length() < 3) {
            if (errorLabel != null) errorLabel.setText("Le titre doit contenir au moins 3 caractères.");
            return;
        }

        if (desc.length() > 500) {
            if (errorLabel != null) errorLabel.setText("La description est trop longue (max 500 caractères).");
            return;
        }

        try {
            if (existing == null) {
                int profId = AppSession.getCurrentUser() == null ? 0 : AppSession.getCurrentUser().getId();
                service.create(new Matiere(0, titre, desc), profId);
            } else {
                existing.setTitre(titre);
                existing.setDescription(desc);
                service.update(existing);
            }
            if (appShell != null) appShell.showMatieres();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
