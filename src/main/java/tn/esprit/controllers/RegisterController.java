package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.Etudiant;
import services.UserService;

import java.util.Date;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField cinField;
    @FXML private TextField telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField adresseField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private Label messageLabel;

    private UserService userService = new UserService();

    @FXML
    public void handleRegister() {
        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim();
        String cin     = cinField.getText().trim();
        String tel     = telephoneField.getText().trim();
        String pwd     = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();
        String adresse = adresseField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
            showMessage("Veuillez remplir tous les champs obligatoires.", "red");
            return;
        }
        if (!pwd.equals(confirm)) {
            showMessage("Les mots de passe ne correspondent pas.", "red");
            return;
        }
        
        System.out.println("Vérification des données pour: " + email);
        if (userService.emailExists(email)) {
            showMessage("Cet email est déjà utilisé.", "red");
            return;
        }

        Etudiant etudiant = new Etudiant();
        etudiant.setNom(nom);
        etudiant.setPrenom(prenom);
        etudiant.setEmail(email);
        etudiant.setPassword(pwd);
        etudiant.setCin(cin);
        etudiant.setTelephone(tel);
        etudiant.setAdresse(adresse);
        
        // Date de naissance
        if (dateNaissancePicker.getValue() != null) {
            Date dateNaissance = java.sql.Date.valueOf(dateNaissancePicker.getValue());
            etudiant.setDateNaissance(dateNaissance);
            System.out.println("  Date de naissance: " + dateNaissance);
        }
        
        etudiant.setNiveau("L1"); // Niveau par défaut
        etudiant.setStatus("actif"); // Status par défaut
        
        System.out.println("Appel registerEtudiant()...");
        boolean success = userService.registerEtudiant(etudiant);

        if (success) {
            showMessage("Compte créé avec succès!", "green");
            // Effacer les champs après succès
            nomField.clear();
            prenomField.clear();
            emailField.clear();
            cinField.clear();
            telephoneField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            adresseField.clear();
            dateNaissancePicker.setValue(null);
        } else {
            showMessage("Erreur lors de l'inscription. Consultez la console pour les détails.", "red");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12;");
        messageLabel.setText(msg);
    }
}

