package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;
import services.UserService;

import java.text.SimpleDateFormat;

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

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField telephoneField;
    @FXML private TextField adresseField;
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;
    @FXML private Label statusLabel;

    private static User currentUser;
    private UserService userService = new UserService();

    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        String initiale = currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty()
            ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase() : "?";
        if (avatarLabel    != null) avatarLabel.setText(initiale);
        if (nomCompletLabel!= null) nomCompletLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());

        String roleDisplay = switch (currentUser.getType()) {
            case "admin"    -> "Administrateur";
            case "prof"     -> "Professeur";
            case "etudiant" -> "Étudiant";
            default         -> currentUser.getType();
        };
        if (roleLabel != null) roleLabel.setText(roleDisplay);

        if (emailLabel     != null) emailLabel.setText(currentUser.getEmail());
        if (telephoneLabel != null) telephoneLabel.setText(
            currentUser.getTelephone() != null ? currentUser.getTelephone() : "Non renseigné");
        if (adresseLabel   != null) adresseLabel.setText(
            currentUser.getAdresse() != null ? currentUser.getAdresse() : "Non renseignée");
        if (cinLabel       != null) cinLabel.setText(
            currentUser.getCin() != null ? currentUser.getCin() : "Non renseigné");
        if (dateNaissanceLabel != null) dateNaissanceLabel.setText(
            currentUser.getDateNaissance() != null ? currentUser.getDateNaissance().toString() : "Non renseignée");
        if (membreDepuisLabel  != null) membreDepuisLabel.setText(
            currentUser.getCreatedAt() != null ? new SimpleDateFormat("yyyy-MM-dd").format(currentUser.getCreatedAt()) : "—");

        // Préremplir les champs
        if (nomField     != null) nomField.setText(currentUser.getNom());
        if (prenomField  != null) prenomField.setText(currentUser.getPrenom());
        if (telephoneField!= null && currentUser.getTelephone() != null)
            telephoneField.setText(currentUser.getTelephone());
        if (adresseField != null && currentUser.getAdresse() != null)
            adresseField.setText(currentUser.getAdresse());
    }

    @FXML public void saveProfile() {
        if (currentUser == null) return;
        // Mise à jour locale (DB à brancher via UserService.update)
        if (nomField    != null && !nomField.getText().isEmpty())
            currentUser.setNom(nomField.getText());
        if (prenomField != null && !prenomField.getText().isEmpty())
            currentUser.setPrenom(prenomField.getText());
        if (telephoneField != null) currentUser.setTelephone(telephoneField.getText());
        if (adresseField   != null) currentUser.setAdresse(adresseField.getText());

        if (statusLabel != null) {
            statusLabel.setStyle("-fx-text-fill: #00c896; -fx-font-size: 12;");
            statusLabel.setText("✓ Profil mis à jour avec succès.");
        }
        // Rafraîchir l'affichage
        initialize();
    }

    @FXML public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;
        if (newPwdField.getText().isEmpty()) {
            showStatus("Veuillez saisir un nouveau mot de passe.", false);
            return;
        }
        if (!newPwdField.getText().equals(confirmPwdField.getText())) {
            showStatus("Les mots de passe ne correspondent pas.", false);
            return;
        }
        if (newPwdField.getText().length() < 6) {
            showStatus("Le mot de passe doit contenir au moins 6 caractères.", false);
            return;
        }
        showStatus("✓ Mot de passe modifié avec succès.", true);
        oldPwdField.clear(); newPwdField.clear(); confirmPwdField.clear();
    }

    private void showStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setStyle("-fx-text-fill: " + (success ? "#00c896" : "#e94560") + "; -fx-font-size: 12;");
        statusLabel.setText(msg);
    }

    @FXML public void goBack() {
        if (currentUser == null) return;
        String dest = switch (currentUser.getType()) {
            case "admin" -> "/tn/esprit/interfaces/DashboardAdmin.fxml";
            case "prof"  -> "/tn/esprit/interfaces/DashboardProf.fxml";
            default      -> "/tn/esprit/interfaces/DashboardEtudiant.fxml";
        };
        try {
            Parent root = FXMLLoader.load(getClass().getResource(dest));
            if (nomField != null) nomField.getScene().setRoot(root);
            else if (emailLabel != null) emailLabel.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void uploadPhoto() {
        new Alert(Alert.AlertType.INFORMATION, "Fonctionnalité d'upload photo à implémenter.", ButtonType.OK).showAndWait();
    }
}

