package com.smartpath.controller.feature_cours_et_quiz;

import com.smartpath.model.feature_cours_et_quiz.User;
import com.smartpath.service.feature_cours_et_quiz.AuthService;
import com.smartpath.service.feature_cours_et_quiz.UserService;
import com.smartpath.util.feature_cours_et_quiz.AppSession;
import com.smartpath.util.feature_cours_et_quiz.ViewNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        emailField.textProperty().addListener((obs, o, n) -> errorLabel.setText(""));
        passwordField.textProperty().addListener((obs, o, n) -> errorLabel.setText(""));
    }

    @FXML
    public void handleLogin() {
        String email = safe(emailField.getText());
        String password = safe(passwordField.getText());

        if (email.isBlank() || password.isBlank()) {
            showInlineError("Email et mot de passe sont obligatoires.");
            return;
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showInlineError("Email invalide.");
            return;
        }

        if (password.length() < 3) {
            showInlineError("Mot de passe invalide.");
            return;
        }

        try {
            User user = authService.login(email, password);
            if (user == null) {
                // Better diagnostics: user not found vs wrong password
                if (userService.findAuthRowByEmail(email) == null) {
                    showInlineError("Utilisateur introuvable. Essayez: admin@smartpath.tn / admin, prof@smartpath.tn / prof, etudiant@smartpath.tn / etudiant");
                } else {
                    showInlineError("Mot de passe incorrect.");
                }
                return;
            }

            AppSession.setCurrentUser(user);
            ViewNavigator.switchRoot("/com/smartpath/feature_cours_et_quiz/app-shell.fxml", controller -> {
                if (controller instanceof AppShellController shell) {
                    shell.onAfterLogin();
                }
            });
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur BD: " + e.getMessage()).showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur UI: " + e.getMessage()).showAndWait();
        }
    }

    private void showInlineError(String message) {
        errorLabel.setText(message);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
