package tn.esprit.controllers;

import jakarta.mail.MessagingException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button sendCodeBtn;
    @FXML private Button resetBtn;

    // Champs cachés au début
    @FXML private javafx.scene.layout.VBox step2Box;

    private final UserService  userService  = new UserService();
    private final EmailService emailService = new EmailService();
    private String currentEmail;

    @FXML
    public void initialize() {
        step2Box.setVisible(false);
        step2Box.setManaged(false);
    }

    // Étape 1 : générer et envoyer le code par email
    @FXML
    public void handleSendCode() {
        currentEmail = emailField.getText().trim();
        if (currentEmail.isEmpty()) {
            showMessage("Entrez votre email.", "red");
            return;
        }

        String token = userService.generateResetToken(currentEmail);
        if (token == null) {
            showMessage("Aucun compte trouvé avec cet email.", "red");
            return;
        }

        // Désactiver le bouton pendant l'envoi
        sendCodeBtn.setDisable(true);
        showMessage("⏳ Envoi en cours...", "#2563eb");

        final String finalToken = token;
        Thread t = new Thread(() -> {
            try {
                emailService.sendPasswordResetCode(currentEmail, finalToken, 10);
                Platform.runLater(() -> {
                    showMessage("✅ Code envoyé sur " + currentEmail, "green");
                    step2Box.setVisible(true);
                    step2Box.setManaged(true);
                    emailField.setDisable(true);
                    sendCodeBtn.setDisable(true);
                });
            } catch (MessagingException e) {
                // Fallback : afficher dans la console si l'email échoue
                System.out.println("[ForgotPassword] Envoi email échoué : " + e.getMessage());
                System.out.println("[ForgotPassword] Code de réinitialisation (fallback console) : " + finalToken);
                Platform.runLater(() -> {
                    showMessage("⚠️ Email non envoyé (config manquante). Code dans la console IntelliJ.", "#d97706");
                    step2Box.setVisible(true);
                    step2Box.setManaged(true);
                    emailField.setDisable(true);
                    sendCodeBtn.setDisable(true);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // Étape 2 : valider le code et changer le mot de passe
    @FXML
    public void handleReset() {
        String token   = tokenField.getText().trim();
        String newPwd  = newPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (token.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            showMessage("Veuillez remplir tous les champs.", "red");
            return;
        }
        if (!newPwd.equals(confirm)) {
            showMessage("Les mots de passe ne correspondent pas.", "red");
            return;
        }
        if (newPwd.length() < 6) {
            showMessage("Le mot de passe doit contenir au moins 6 caractères.", "red");
            return;
        }

        boolean success = userService.resetPassword(currentEmail, token, newPwd);
        if (success) {
            showMessage("✓ Mot de passe réinitialisé avec succès !", "green");
            resetBtn.setDisable(true);
        } else {
            showMessage("Code invalide ou expiré (valable 10 min).", "red");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12;");
        messageLabel.setText(msg);
    }
}
