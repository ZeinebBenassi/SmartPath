package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserService userService = new UserService();

    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            if ("ban".equalsIgnoreCase(user.getStatus())) {
                errorLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
                errorLabel.setText("❌ Accès refusé: Votre compte est banni");
                return;
            }

            errorLabel.setStyle("-fx-text-fill: #00c896; -fx-font-size: 12;");
            errorLabel.setText("Bienvenue " + user.getNom() + " ! (" + user.getType() + ")");

            try {
                String fxml;
                switch (user.getType()) {
                    case "admin":
                        DashboardAdminController.setCurrentUser(user);
                        DashboardEtudiantController.setSourceDashboardType(null);
                        fxml = "/tn/esprit/interfaces/DashboardAdmin.fxml";
                        break;
                    case "prof":
                        DashboardProfController.setCurrentUser(user);
                        DashboardEtudiantController.setSourceDashboardType(null);
                        fxml = "/tn/esprit/interfaces/DashboardProf.fxml";
                        break;
                    default:
                        DashboardEtudiantController.setCurrentUser(user);
                        DashboardEtudiantController.setSourceDashboardType(null);
                        fxml = "/tn/esprit/interfaces/DashboardEtudiant.fxml";
                        break;
                }
                Parent root = FXMLLoader.load(getClass().getResource(fxml));
                emailField.getScene().setRoot(root);
            } catch (Exception e) {
                System.out.println("Erreur navigation: " + e.getMessage());
                e.printStackTrace();
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
