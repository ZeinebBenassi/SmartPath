package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import services.UserService;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button sendCodeBtn;
    @FXML private Button resetBtn;

    // Champs cachÃ©s au dÃ©but
    @FXML private javafx.scene.layout.VBox step2Box;

    private UserService userService = new UserService();
    private String currentEmail;

    @FXML
    public void initialize() {
        step2Box.setVisible(false);
        step2Box.setManaged(false);
    }

    // Ã‰tape 1 : envoyer le code
    @FXML
    public void handleSendCode() {
        currentEmail = emailField.getText().trim();
        if (currentEmail.isEmpty()) {
            showMessage("Entrez votre email.", "red");
            return;
        }

        String token = userService.generateResetToken(currentEmail);
        if (token == null) {
            showMessage("Aucun compte trouvÃ© avec cet email.", "red");
            return;
        }

        // En production : envoyer par email. Ici on affiche dans la console.
        System.out.println("ðŸ”‘ Code de rÃ©initialisation : " + token);
        showMessage("Code envoyÃ© ! (visible dans la console IntelliJ)", "green");

        // Afficher l'Ã©tape 2
        step2Box.setVisible(true);
        step2Box.setManaged(true);
        emailField.setDisable(true);
        sendCodeBtn.setDisable(true);
    }

    // Ã‰tape 2 : valider le code et changer le mot de passe
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
            showMessage("Le mot de passe doit contenir au moins 6 caractÃ¨res.", "red");
            return;
        }

        boolean success = userService.resetPassword(currentEmail, token, newPwd);
        if (success) {
            showMessage("âœ… Mot de passe rÃ©initialisÃ© avec succÃ¨s !", "green");
            resetBtn.setDisable(true);
        } else {
            showMessage("Code invalide ou expirÃ©.", "red");
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

