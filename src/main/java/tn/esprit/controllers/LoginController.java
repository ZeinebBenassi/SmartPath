package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;
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
            errorLabel.setStyle("-fx-text-fill: #00c896; -fx-font-size: 12;");
            errorLabel.setText("Bienvenue " + user.getNom() + " ! (" + user.getType() + ")");

            // Navigation selon le rôle
            try {
                String fxml;
                switch (user.getType()) {
                    case "admin":
                        DashboardAdminController.setCurrentUser(user);
                        fxml = "/views/DashboardAdmin.fxml";
                        break;
                    case "prof":
                        DashboardProfController.setCurrentUser(user);
                        fxml = "/views/DashboardProf.fxml";
                        break;
                    default:
                        DashboardEtudiantController.setCurrentUser(user);
                        fxml = "/views/DashboardEtudiant.fxml";
                        break;
                }
                Parent root = FXMLLoader.load(getClass().getResource(fxml));
                emailField.getScene().setRoot(root);
            } catch (Exception e) {
                // Dashboard pas encore créé — message de succès suffit
                System.out.println("Dashboard à créer pour : " + user.getType());
            }
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    @FXML
    public void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Register.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void goToForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ForgotPassword.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
        errorLabel.setText(msg);
    }
}


