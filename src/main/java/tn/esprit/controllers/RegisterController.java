package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entity.Etudiant;
import services.UserService;

import java.time.LocalDate;
import java.util.Date;
import java.util.regex.Pattern;

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
    
    // Labels d'erreur
    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label emailError;
    @FXML private Label cinError;
    @FXML private Label phoneError;
    @FXML private Label adresseError;
    @FXML private Label dateError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;

    private UserService userService = new UserService();
    
    // Regex pour validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:\\+216|0)[0-9]{8}$");
    private static final Pattern CIN_PATTERN = Pattern.compile(
        "^[0-9]{8}$");

    @FXML
    public void handleRegister() {
        // Effacer tous les messages d'erreur
        clearErrors();
        
        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim();
        String cin     = cinField.getText().trim();
        String tel     = telephoneField.getText().trim();
        String pwd     = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();
        String adresse = adresseField.getText().trim();

        boolean hasErrors = false;

        // Validation Nom
        if (nom.isEmpty()) {
            setError(nomError, "Le nom est obligatoire");
            hasErrors = true;
        } else if (!nom.matches("[a-zA-ZA-Za-zÀ-ÿ\\s'-]+")) {
            setError(nomError, "Le nom ne doit contenir que des lettres");
            hasErrors = true;
        }

        // Validation Prenom
        if (prenom.isEmpty()) {
            setError(prenomError, "Le prenom est obligatoire");
            hasErrors = true;
        } else if (!prenom.matches("[a-zA-ZA-Za-zÀ-ÿ\\s'-]+")) {
            setError(prenomError, "Le prenom ne doit contenir que des lettres");
            hasErrors = true;
        }

        // Validation Email
        if (email.isEmpty()) {
            setError(emailError, "L'email est obligatoire");
            hasErrors = true;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            setError(emailError, "Format email invalide");
            hasErrors = true;
        }

        // Validation CIN (optionnel)
        if (!cin.isEmpty() && !CIN_PATTERN.matcher(cin).matches()) {
            setError(cinError, "Le CIN doit contenir 8 chiffres");
            hasErrors = true;
        }

        // Validation Telephone (optionnel)
        if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
            setError(phoneError, "Format: +216XXXXXXXX ou 0XXXXXXXX");
            hasErrors = true;
        }

        // Validation Adresse (optionnel)
        if (!adresse.isEmpty() && adresse.length() < 5) {
            setError(adresseError, "L'adresse doit faire minimum 5 caracteres");
            hasErrors = true;
        }

        // Validation Date de naissance
        if (dateNaissancePicker.getValue() == null) {
            setError(dateError, "La date de naissance est obligatoire");
            hasErrors = true;
        } else if (dateNaissancePicker.getValue().isAfter(LocalDate.now())) {
            setError(dateError, "La date ne doit pas etre future");
            hasErrors = true;
        } else {
            LocalDate birthDate = dateNaissancePicker.getValue();
            LocalDate today = LocalDate.now();
            int age = today.getYear() - birthDate.getYear();
            if (age < 13) {
                setError(dateError, "Vous devez avoir au moins 13 ans");
                hasErrors = true;
            }
        }

        // Validation Mot de passe
        if (pwd.isEmpty()) {
            setError(passwordError, "Le mot de passe est obligatoire");
            hasErrors = true;
        } else if (pwd.length() < 6) {
            setError(passwordError, "Minimum 6 caracteres");
            hasErrors = true;
        }

        // Validation Confirmation mot de passe
        if (confirm.isEmpty()) {
            setError(confirmError, "Confirmation obligatoire");
            hasErrors = true;
        } else if (!pwd.equals(confirm)) {
            setError(confirmError, "Les mots de passe ne correspondent pas");
            hasErrors = true;
        }

        if (hasErrors) {
            setMessageLabel("Veuillez corriger les erreurs ci-dessus.", "red");
            return;
        }

        // Vérifier si l'email existe déjà
        if (userService.emailExists(email)) {
            setError(emailError, "Cet email est déjà utilisé");
            setMessageLabel("Cet email est déjà utilisé.", "red");
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
        
        etudiant.setNiveau("L1");
        etudiant.setStatus("actif");
        
        System.out.println("Appel registerEtudiant()...");
        boolean success = userService.registerEtudiant(etudiant);

        if (success) {
            setMessageLabel("Compte créé avec succès!", "green");
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
            setMessageLabel("Erreur lors de l'inscription. Consultez la console.", "red");
        }
    }

    private void clearErrors() {
        nomError.setText("");
        prenomError.setText("");
        emailError.setText("");
        cinError.setText("");
        phoneError.setText("");
        adresseError.setText("");
        dateError.setText("");
        passwordError.setText("");
        confirmError.setText("");
        messageLabel.setText("");
    }

    private void setError(Label errorLabel, String message) {
        errorLabel.setText(message);
    }

    private void setMessageLabel(String msg, String color) {
        messageLabel.setText(msg);
        if ("red".equals(color)) {
            messageLabel.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold;");
        } else if ("green".equals(color)) {
            messageLabel.setStyle("-fx-text-fill: #00c896; -fx-font-weight: bold;");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

