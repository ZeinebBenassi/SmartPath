package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;
import tn.esprit.utils.FormValidator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil utilisateur — affiché selon le rôle (admin / prof / étudiant).
 * Persiste les modifications en base via UserService.
 */
public class ProfilController {

    @FXML private Label avatarLabel;
    @FXML private Label nomCompletLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailLabel;
    @FXML private Label telephoneLabel;
    @FXML private Label adresseLabel;
    @FXML private Label cinLabel;
    @FXML private Label dateNaissanceLabel;
    @FXML private Label membreDepuisLabel;

    // Champs éditables
    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     telephoneField;
    @FXML private TextField     adresseField;

    // Changement de mot de passe
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;

    @FXML private Label statusLabel;

    private static User currentUser;
    private final UserService userService = new UserService();

    public static void setCurrentUser(User u) { currentUser = u; }

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        // Avatar : initiale du prénom
        String initiale = (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
            ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase() : "?";
        set(avatarLabel,        initiale);
        set(nomCompletLabel,    currentUser.getPrenom() + " " + currentUser.getNom());
        set(roleLabel,          roleDisplay(currentUser.getType()));
        set(emailLabel,         currentUser.getEmail());
        set(telephoneLabel,     nvl(currentUser.getTelephone(),  "Non renseigné"));
        set(adresseLabel,       nvl(currentUser.getAdresse(),    "Non renseignée"));
        set(cinLabel,           nvl(currentUser.getCin(),        "Non renseigné"));
        set(membreDepuisLabel,  currentUser.getCreatedAt() != null
            ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getCreatedAt()) : "—");
        set(dateNaissanceLabel, currentUser.getDateNaissance() != null
            ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getDateNaissance()) : "Non renseignée");

        // Champs édition
        setText(nomField,        currentUser.getNom());
        setText(prenomField,     currentUser.getPrenom());
        setText(telephoneField,  currentUser.getTelephone());
        setText(adresseField,    currentUser.getAdresse());

        hideStatus();
    }

    // ── Sauvegarde profil ────────────────────────────────────────────────────

    @FXML
    public void saveProfile() {
        if (currentUser == null) return;

        List<String> errors = new ArrayList<>();
        boolean ok = true;
        ok &= FormValidator.validateRequired(nomField,    "Le nom",    errors);
        ok &= FormValidator.validateRequired(prenomField, "Le prénom", errors);
        ok &= FormValidator.validatePhone(telephoneField, errors);
        FormValidator.showErrors(statusLabel, errors);
        if (!ok) return;

        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        if (telephoneField != null && !telephoneField.getText().trim().isEmpty())
            currentUser.setTelephone(telephoneField.getText().trim());
        if (adresseField != null && !adresseField.getText().trim().isEmpty())
            currentUser.setAdresse(adresseField.getText().trim());

        boolean saved = userService.update(currentUser);
        if (saved) {
            showStatus("✅ Profil mis à jour avec succès.", true);
            initialize();
        } else {
            showStatus("❌ Erreur lors de la sauvegarde. Vérifiez votre connexion.", false);
        }
    }

    // ── Changement de mot de passe ───────────────────────────────────────────

    @FXML
    public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;

        String oldPwd     = oldPwdField.getText().trim();
        String newPwd     = newPwdField.getText();
        String confirmPwd = confirmPwdField.getText();

        if (oldPwd.isEmpty()) { showStatus("❌ Saisissez votre mot de passe actuel.", false); return; }
        if (newPwd.isEmpty()) { showStatus("❌ Saisissez un nouveau mot de passe.", false); return; }
        if (newPwd.length() < 6) { showStatus("❌ Le mot de passe doit contenir au moins 6 caractères.", false); return; }
        if (!newPwd.equals(confirmPwd)) { showStatus("❌ Les mots de passe ne correspondent pas.", false); return; }

        currentUser.setPassword(newPwd);
        boolean saved = userService.update(currentUser);
        if (saved) {
            showStatus("✅ Mot de passe modifié avec succès.", true);
            oldPwdField.clear(); newPwdField.clear(); confirmPwdField.clear();
        } else {
            showStatus("❌ Erreur lors du changement de mot de passe.", false);
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @FXML
    public void goBack() {
        if (currentUser == null) return;
        String dest = switch (currentUser.getType()) {
            case "admin"    -> "/tn/esprit/interfaces/DashboardAdmin.fxml";
            case "prof"     -> "/tn/esprit/interfaces/DashboardProf.fxml";
            default         -> "/tn/esprit/interfaces/DashboardEtudiant.fxml";
        };
        try {
            Parent root = FXMLLoader.load(getClass().getResource(dest));
            javafx.scene.Node ref = (nomField != null) ? nomField : emailLabel;
            if (ref != null) ref.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void uploadPhoto() {
        new Alert(Alert.AlertType.INFORMATION,
            "Fonctionnalité d'upload photo à implémenter.", ButtonType.OK).showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String roleDisplay(String type) {
        if (type == null) return "Utilisateur";
        return switch (type) {
            case "admin"    -> "Administrateur";
            case "prof"     -> "Professeur";
            case "etudiant" -> "Étudiant";
            default         -> type;
        };
    }

    private void set(Label l, String v)      { if (l != null) l.setText(v); }
    private void setText(TextField f, String v) { if (f != null && v != null) f.setText(v); }
    private String nvl(String s, String def) { return (s != null && !s.isBlank()) ? s : def; }

    private void showStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (success ? "#00c896" : "#e94560")
            + "; -fx-font-size: 12; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }

    private void hideStatus() {
        if (statusLabel != null) { statusLabel.setText(""); statusLabel.setVisible(false); }
    }
}
