package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.Etudiant;
import models.Prof;
import services.UserService;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField cinField;
    @FXML private TextField telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label messageLabel;

    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("Étudiant", "Professeur");
        roleCombo.setValue("Étudiant");
    }

    @FXML
    public void handleRegister() {
        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim();
        String cin     = cinField.getText().trim();
        String tel     = telephoneField.getText().trim();
        String pwd     = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();
        String role    = roleCombo.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
            showMessage("Veuillez remplir tous les champs obligatoires.", "red");
            return;
        }
        if (!pwd.equals(confirm)) {
            showMessage("Les mots de passe ne correspondent pas.", "red");
            return;
        }
        if (userService.emailExists(email)) {
            showMessage("Cet email est déjà utilisé.", "red");
            return;
        }

        boolean success;
        if (role.equals("Professeur")) {
            Prof prof = new Prof();
            prof.setNom(nom); prof.setPrenom(prenom);
            prof.setEmail(email); prof.setPassword(pwd);
            prof.setCin(cin); prof.setTelephone(tel);
            success = userService.registerProf(prof);
        } else {
            Etudiant etudiant = new Etudiant();
            etudiant.setNom(nom); etudiant.setPrenom(prenom);
            etudiant.setEmail(email); etudiant.setPassword(pwd);
            etudiant.setCin(cin); etudiant.setTelephone(tel);
            success = userService.registerEtudiant(etudiant);
        }

        if (success) {
            showMessage("✅ Compte créé avec succès !", "green");
        } else {
            showMessage("❌ Erreur lors de l'inscription.", "red");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12;");
        messageLabel.setText(msg);
    }
}

