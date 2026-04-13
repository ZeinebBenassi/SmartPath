package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entity.User;
import services.UserService;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserService userService = new UserService();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            // Vérifier si l'utilisateur est banni
            if ("ban".equalsIgnoreCase(user.getStatus())) {
                showError("Accès refusé - Compte banni\n\nVotre compte a été désactivé par un administrateur.\nVeuillez contacter le support.");
                errorLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
                errorLabel.setText("❌ Accès refusé: Votre compte est banni");
                return;
            }

            errorLabel.setStyle("-fx-text-fill: #00c896; -fx-font-size: 12;");
            errorLabel.setText("Bienvenue " + user.getNom() + " ! (" + user.getType() + ")");

            // Navigation selon le rôle
            try {
                String fxml;
                switch (user.getType()) {
                    case "admin":
                        tn.esprit.controllers.DashboardAdminController.setCurrentUser(user);
                        tn.esprit.controllers.DashboardEtudiantController.setSourceDashboardType(null);
                        fxml = "/tn/esprit/interfaces/DashboardAdmin.fxml";
                        break;
                    case "prof":
                        tn.esprit.controllers.DashboardProfController.setCurrentUser(user);
                        tn.esprit.controllers.DashboardEtudiantController.setSourceDashboardType(null);
                        fxml = "/tn/esprit/interfaces/DashboardProf.fxml";
                        break;
                    default:
                        tn.esprit.controllers.DashboardEtudiantController.setCurrentUser(user);
                        tn.esprit.controllers.DashboardEtudiantController.setSourceDashboardType(null);
                        fxml = "/tn/esprit/interfaces/DashboardEtudiant.fxml";
                        break;
                }
                Parent root = FXMLLoader.load(getClass().getResource(fxml));
                emailField.getScene().setRoot(root);
            } catch (Exception e) {
                // Dashboard pas encore créé - message de succès suffit
                System.out.println("Dashboard à créer pour : " + user.getType());
            }
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    @FXML
    public void goToRegister() {
        navigateTo("/tn/esprit/interfaces/Register.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        navigateTo("/tn/esprit/interfaces/ForgotPassword.fxml");
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation impossible: " + fxml);
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
        errorLabel.setText(msg);
    }
}

