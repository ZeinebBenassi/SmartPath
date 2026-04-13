package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entity.User;

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


    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        String initiale = currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty()
            ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase() : "?";
        if (avatarLabel    != null) avatarLabel.setText(initiale);
        if (nomCompletLabel!= null) nomCompletLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());

        String roleDisplay;
        switch (currentUser.getType()) {
            case "admin":
                roleDisplay = "Administrateur";
                break;
            case "prof":
                roleDisplay = "Professeur";
                break;
            case "etudiant":
                roleDisplay = "Étudiant";
                break;
            default:
                roleDisplay = currentUser.getType();
        }
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
        
        // Validation des champs
        String nom = nomField != null ? nomField.getText().trim() : "";
        String prenom = prenomField != null ? prenomField.getText().trim() : "";
        String telephone = telephoneField != null ? telephoneField.getText().trim() : "";
        String adresse = adresseField != null ? adresseField.getText().trim() : "";
        
        if (nom.isEmpty()) {
            showStatus("❌ Le nom est obligatoire.", false);
            return;
        }
        if (prenom.isEmpty()) {
            showStatus("❌ Le prénom est obligatoire.", false);
            return;
        }
        if (telephone.length() > 20) {
            showStatus("❌ Le téléphone ne doit pas dépasser 20 caractères.", false);
            return;
        }
        if (telephone.length() > 0 && !telephone.matches("^[0-9\\s\\-\\+\\(\\)]+$")) {
            showStatus("❌ Le téléphone contient des caractères invalides.", false);
            return;
        }
        if (adresse.length() > 200) {
            showStatus("❌ L'adresse ne doit pas dépasser 200 caractères.", false);
            return;
        }
        
        // Mise à jour locale
        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        if (!telephone.isEmpty()) currentUser.setTelephone(telephone);
        if (!adresse.isEmpty()) currentUser.setAdresse(adresse);

        if (statusLabel != null) {
            statusLabel.setStyle("-fx-text-fill: #00c896; -fx-font-size: 12;");
            statusLabel.setText("✅ Profil mis à jour avec succès.");
        }
        initialize();
    }

    @FXML public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;
        
        String oldPwd = oldPwdField.getText().trim();
        String newPwd = newPwdField.getText().trim();
        String confirmPwd = confirmPwdField.getText().trim();
        
        if (oldPwd.isEmpty()) {
            showStatus("Saisissez votre mot de passe actuel.", false);
            return;
        }
        if (newPwd.isEmpty()) {
            showStatus("Saisissez un nouveau mot de passe.", false);
            return;
        }
        if (confirmPwd.isEmpty()) {
            showStatus("Confirmez le nouveau mot de passe.", false);
            return;
        }
        if (newPwd.length() < 6) {
            showStatus("Le mot de passe doit contenir au moins 6 caracteres.", false);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            showStatus("Les nouveaux mots de passe ne correspondent pas.", false);
            return;
        }
        if (oldPwd.equals(newPwd)) {
            showStatus("Le nouveau mot de passe doit etre different de l'ancien.", false);
            return;
        }
        
        showStatus("Mot de passe modifie avec succes.", true);
        oldPwdField.clear(); 
        newPwdField.clear(); 
        confirmPwdField.clear();
    }

    private void showStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        String color = success ? "#00c896" : "#e94560";
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12; -fx-font-weight: bold;");
        statusLabel.setText(msg);
    }

    @FXML public void goBack() {
        if (currentUser == null) return;
        String dest;
        switch (currentUser.getType()) {
            case "admin":
                dest = "/tn/esprit/interfaces/DashboardAdmin.fxml";
                break;
            case "prof":
                dest = "/tn/esprit/interfaces/DashboardProf.fxml";
                break;
            default:
                dest = "/tn/esprit/interfaces/DashboardEtudiant.fxml";
        }
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

