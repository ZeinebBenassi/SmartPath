package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import tn.esprit.entity.Etudiant;
import tn.esprit.services.CloudinaryService;
import tn.esprit.services.UserService;
import tn.esprit.utils.FormValidator;

import java.io.File;
import java.time.LocalDate;
import java.util.Date;
import java.util.regex.Pattern;

public class RegisterController {

    // ── Champs du formulaire ──────────────────────────────────────────────────
    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     emailField;
    @FXML private TextField     cinField;
    @FXML private TextField     telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     adresseField;
    @FXML private DatePicker    dateNaissancePicker;
    @FXML private Label         messageLabel;

    // ── Labels d'erreur ───────────────────────────────────────────────────────
    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label emailError;
    @FXML private Label cinError;
    @FXML private Label phoneError;
    @FXML private Label adresseError;
    @FXML private Label dateError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label niveauError;   // ✅ nouveau

    // ── 🔐 Widgets de force mot de passe ──────────────────────────────────────
    @FXML private ProgressBar strengthBar;
    @FXML private Label       strengthLevel;
    @FXML private Label       strengthAdvice;

    // ── 📷 Photo de profil ────────────────────────────────────────────────────
    @FXML private ImageView photoPreview;
    @FXML private Label     photoLabel;
    private File selectedPhotoFile = null;

    // ── Niveau ────────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> niveauCombo;   // ✅ nouveau

    // ── Services ──────────────────────────────────────────────────────────────
    private final UserService      userService = new UserService();
    private final CloudinaryService cloudinary  = new CloudinaryService();

    // ── Regex ─────────────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:[0-9]{8}|\\+216[0-9]{8}|0[0-9]{8})$");
    private static final Pattern CIN_PATTERN = Pattern.compile(
        "^[0-9]{8}$");

    // ── Initialisation ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Écoute en temps réel sur le champ mot de passe
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) ->
                FormValidator.updatePasswordStrengthUI(
                    passwordField, strengthBar, strengthLevel, strengthAdvice));
        }
        // ✅ Remplir la ComboBox Niveau
        if (niveauCombo != null) {
            niveauCombo.getItems().addAll("L1", "L2", "L3", "M1", "M2", "Doctorat");
            niveauCombo.setValue("L1"); // valeur par défaut
        }
    }

    // ── Inscription ───────────────────────────────────────────────────────────
    @FXML
    public void handleRegister() {
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
        } else if (!nom.matches("[a-zA-ZÀ-ÿ\\s'-]+")) {
            setError(nomError, "Le nom ne doit contenir que des lettres");
            hasErrors = true;
        }

        // Validation Prenom
        if (prenom.isEmpty()) {
            setError(prenomError, "Le prenom est obligatoire");
            hasErrors = true;
        } else if (!prenom.matches("[a-zA-ZÀ-ÿ\\s'-]+")) {
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
            setError(phoneError, "Format: XXXXXXXX, +216XXXXXXXX ou 0XXXXXXXX");
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
            int age = LocalDate.now().getYear() - birthDate.getYear();
            if (age < 13) {
                setError(dateError, "Vous devez avoir au moins 13 ans");
                hasErrors = true;
            }
        }

        // ── 🔐 Validation Mot de passe avec niveau de force ──────────────────
        if (pwd.isEmpty()) {
            setError(passwordError, "Le mot de passe est obligatoire");
            hasErrors = true;
        } else if (pwd.length() < 6) {
            setError(passwordError, "Minimum 6 caracteres");
            hasErrors = true;
        } else {
            // Vérification : refuser les mots de passe LOW
            tn.esprit.utils.PasswordStrengthUtil.Result r =
                tn.esprit.utils.PasswordStrengthUtil.analyze(pwd);
            if (r.level == tn.esprit.utils.PasswordStrengthUtil.Level.LOW) {
                setError(passwordError, "Mot de passe trop faible — " + r.conseil);
                hasErrors = true;
            }
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

        // Créer l'étudiant
        Etudiant etudiant = new Etudiant();
        etudiant.setNom(nom);
        etudiant.setPrenom(prenom);
        etudiant.setEmail(email);
        etudiant.setPassword(pwd);
        etudiant.setCin(cin);
        etudiant.setTelephone(tel);
        etudiant.setAdresse(adresse);
        if (dateNaissancePicker.getValue() != null) {
            etudiant.setDateNaissance(java.sql.Date.valueOf(dateNaissancePicker.getValue()));
        }
        etudiant.setNiveau(niveauCombo != null && niveauCombo.getValue() != null ? niveauCombo.getValue() : "L1"); // ✅ depuis ComboBox
        etudiant.setStatus("actif");

        // ── Désactiver le bouton pour éviter double-clic ──────────────────────
        // On lance tout dans un thread pour ne pas bloquer l'UI
        setMessageLabel(selectedPhotoFile != null ? "⏳ Upload photo en cours..." : "⏳ Création du compte...", "blue");

        final Etudiant etudiantFinal = etudiant;
        final File photoFile = selectedPhotoFile;
        final String emailFinal = email;

        Thread worker = new Thread(() -> {
            // ── 1. Upload photo (hors UI thread) ─────────────────────────────
            if (photoFile != null) {
                System.out.println("[Register] Upload photo : " + photoFile.getAbsolutePath());
                try {
                    if (!photoFile.exists()) {
                        System.err.println("[Register] ❌ Fichier introuvable : " + photoFile.getAbsolutePath());
                    } else if (photoFile.length() == 0) {
                        System.err.println("[Register] ❌ Fichier vide : " + photoFile.getName());
                    } else {
                        String photoUrl = cloudinary.uploadImage(
                                photoFile,
                                "etudiant_" + emailFinal.replaceAll("[^a-zA-Z0-9]", "_")
                        );
                        if (photoUrl != null && !photoUrl.isBlank()) {
                            etudiantFinal.setPhoto(photoUrl);
                            System.out.println("[Register] ✅ Photo uploadée : " + photoUrl);
                        } else {
                            System.err.println("[Register] ⚠️ Upload échoué, compte créé sans photo.");
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[Register] ❌ Exception upload : " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // ── 2. Créer le compte en base (hors UI thread) ───────────────────
            boolean success = userService.registerEtudiant(etudiantFinal);

            // ── 3. Retour sur le thread UI ────────────────────────────────────
            Platform.runLater(() -> {
                if (success) {
                    setMessageLabel("✅ Compte créé avec succès ! Redirection...", "green");
                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                        Platform.runLater(this::goToLogin);
                    }).start();
                } else {
                    setMessageLabel("❌ Erreur lors de l'inscription. Vérifiez la console.", "red");
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

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
        if (niveauError != null) niveauError.setText("");
        messageLabel.setText("");
    }

    private void clearAllFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        cinField.clear();
        telephoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        adresseField.clear();
        dateNaissancePicker.setValue(null);
    }

    private void setError(Label errorLabel, String message) {
        if (errorLabel != null) errorLabel.setText(message);
    }

    private void setMessageLabel(String msg, String color) {
        if (messageLabel == null) return;
        messageLabel.setText(msg);
        String style;
        switch (color) {
            case "red"   -> style = "-fx-text-fill: #e94560; -fx-font-weight: bold;";
            case "green" -> style = "-fx-text-fill: #00c896; -fx-font-weight: bold;";
            case "blue"  -> style = "-fx-text-fill: #2563eb; -fx-font-weight: bold;";
            default      -> style = "-fx-text-fill: #5a6a8a; -fx-font-weight: bold;";
        }
        messageLabel.setStyle(style);
    }

    // ── Choix de la photo ──────────────────────────────────────────────────────────
    @FXML
    public void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = chooser.showOpenDialog(nomField.getScene().getWindow());
        if (file != null) {
            selectedPhotoFile = file;
            photoLabel.setText(file.getName());
            photoPreview.setImage(new Image(file.toURI().toString()));
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
