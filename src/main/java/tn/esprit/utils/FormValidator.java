package tn.esprit.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FormValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern CIN_PATTERN =
            Pattern.compile("^[0-9]{8}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+216)?[0-9]{8}$");

    private static final String STYLE_ERROR  =
            "-fx-border-color: #e53e3e; -fx-border-width: 1.5; -fx-border-radius: 8; " +
                    "-fx-background-color: #fff5f5; -fx-background-radius: 8; -fx-padding: 8 12;";

    private static final String STYLE_OK =
            "-fx-background-color: #F8FAFC; -fx-background-radius: 8; " +
                    "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 8 12;";

    public static boolean validateRequired(TextField field, String labelName, List<String> errors) {
        if (field == null) return true;
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) {
            markError(field);
            errors.add(labelName + " est obligatoire.");
            return false;
        }
        markOk(field);
        return true;
    }

    public static boolean validateEmail(TextField field, List<String> errors) {
        if (field == null) return true;
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) {
            markError(field);
            errors.add("L'email est obligatoire.");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(val).matches()) {
            markError(field);
            errors.add("L'email n'est pas valide (ex : nom@domaine.com).");
            return false;
        }
        markOk(field);
        return true;
    }

    public static boolean validatePassword(PasswordField field, boolean required, List<String> errors) {
        if (field == null) return true;
        String val = field.getText() == null ? "" : field.getText();
        if (required && val.isEmpty()) {
            markError(field);
            errors.add("Le mot de passe est obligatoire.");
            return false;
        }
        if (!val.isEmpty() && val.length() < 6) {
            markError(field);
            errors.add("Le mot de passe doit contenir au moins 6 caractères.");
            return false;
        }
        markOk(field);
        return true;
    }

    public static boolean validatePasswordMatch(PasswordField pwd, PasswordField confirm, List<String> errors) {
        if (pwd == null || confirm == null) return true;
        if (!pwd.getText().equals(confirm.getText())) {
            markError(confirm);
            errors.add("Les mots de passe ne correspondent pas.");
            return false;
        }
        markOk(confirm);
        return true;
    }

    public static boolean validateCIN(TextField field, List<String> errors) {
        if (field == null) return true;
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) { markOk(field); return true; }
        if (!CIN_PATTERN.matcher(val).matches()) {
            markError(field);
            errors.add("Le CIN doit contenir exactement 8 chiffres.");
            return false;
        }
        markOk(field);
        return true;
    }

    public static boolean validatePhone(TextField field, List<String> errors) {
        if (field == null) return true;
        String val = field.getText() == null ? "" : field.getText().replaceAll("\\s", "");
        if (val.isEmpty()) { markOk(field); return true; }
        if (!PHONE_PATTERN.matcher(val).matches()) {
            markError(field);
            errors.add("Le téléphone doit contenir 8 chiffres (ex : 55123456 ou +21655123456).");
            return false;
        }
        markOk(field);
        return true;
    }

    public static boolean validateDate(DatePicker picker, String labelName, boolean required, List<String> errors) {
        if (picker == null) return true;
        if (required && picker.getValue() == null) {
            picker.setStyle("-fx-border-color: #e53e3e; -fx-border-width: 1.5; -fx-border-radius: 8;");
            errors.add(labelName + " est obligatoire.");
            return false;
        }
        picker.setStyle("");
        return true;
    }

    public static void resetStyle(TextField field) {
        if (field != null) markOk(field);
    }

    public static void showErrors(Label statusLabel, List<String> errors) {
        if (statusLabel == null) return;
        if (errors.isEmpty()) {
            statusLabel.setText("");
            statusLabel.setVisible(false);
        } else {
            statusLabel.setText("⚠ " + String.join("\n⚠ ", errors));
            statusLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: 11px; -fx-font-weight: bold;");
            statusLabel.setVisible(true);
        }
    }

    private static void markError(TextField field) { field.setStyle(STYLE_ERROR); }
    private static void markOk(TextField field) { field.setStyle(STYLE_OK); }
    private static void markError(PasswordField field) { field.setStyle(STYLE_ERROR); }
    private static void markOk(PasswordField field) { field.setStyle(STYLE_OK); }
}