package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;
import tn.esprit.utils.FormValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil utilisateur — affiché selon le rôle (admin / prof).
 * Supporte l'upload de photo de profil.
 */
public class ProfilController {

    @FXML private StackPane  avatarPane;
    @FXML private Label      avatarLabel;
    @FXML private ImageView  photoView;
    @FXML private Label      nomCompletLabel;
    @FXML private Label      roleLabel;
    @FXML private Label      emailLabel;
    @FXML private Label      telephoneLabel;
    @FXML private Label      adresseLabel;
    @FXML private Label      cinLabel;
    @FXML private Label      dateNaissanceLabel;
    @FXML private Label      membreDepuisLabel;

    // Champs éditables
    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     telephoneField;
    @FXML private TextField     adresseField;

    // Changement de mot de passe
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;

    @FXML private Label statusLabel;

    private static User currentUser;
    private final UserService userService = new UserService();

    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        String initiale = (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
            ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase() : "?";
        set(avatarLabel,        initiale);
        set(nomCompletLabel,    currentUser.getPrenom() + " " + currentUser.getNom());
        set(roleLabel,          roleDisplay(currentUser.getType()));
        set(emailLabel,         currentUser.getEmail());
        set(telephoneLabel,     nvl(currentUser.getTelephone(),  "Non renseigné"));
        set(adresseLabel,       nvl(currentUser.getAdresse(),    "Non renseignée"));
        set(cinLabel,           nvl(currentUser.getCin(),        "Non renseigné"));
        set(membreDepuisLabel,  currentUser.getCreatedAt() != null
            ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getCreatedAt()) : "—");
        set(dateNaissanceLabel, currentUser.getDateNaissance() != null
            ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getDateNaissance()) : "Non renseignée");

        // Afficher la photo si disponible
        loadPhoto(currentUser.getPhoto());

        // Champs édition
        setText(nomField,        currentUser.getNom());
        setText(prenomField,     currentUser.getPrenom());
        setText(telephoneField,  currentUser.getTelephone());
        setText(adresseField,    currentUser.getAdresse());

        hideStatus();
    }

    @FXML
    public void saveProfile() {
        if (currentUser == null) return;

        List<String> errors = new ArrayList<>();
        boolean ok = true;
        ok &= FormValidator.validateRequired(nomField,    "Le nom",    errors);
        ok &= FormValidator.validateRequired(prenomField, "Le prénom", errors);
        ok &= FormValidator.validatePhone(telephoneField, errors);
        FormValidator.showErrors(statusLabel, errors);
        if (!ok) return;

        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        if (telephoneField != null && !telephoneField.getText().trim().isEmpty())
            currentUser.setTelephone(telephoneField.getText().trim());
        if (adresseField != null && !adresseField.getText().trim().isEmpty())
            currentUser.setAdresse(adresseField.getText().trim());

        boolean saved = userService.update(currentUser);
        if (saved) {
            showStatus("✅ Profil mis à jour avec succès.", true);
            initialize();
        } else {
            showStatus("❌ Erreur lors de la sauvegarde. Vérifiez votre connexion.", false);
        }
    }

    @FXML
    public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;

        String oldPwd     = oldPwdField.getText().trim();
        String newPwd     = newPwdField.getText();
        String confirmPwd = confirmPwdField.getText();

        if (oldPwd.isEmpty())        { showStatus("❌ Saisissez votre mot de passe actuel.", false); return; }
        if (newPwd.isEmpty())        { showStatus("❌ Saisissez un nouveau mot de passe.", false); return; }
        if (newPwd.length() < 6)     { showStatus("❌ Le mot de passe doit contenir au moins 6 caractères.", false); return; }
        if (!newPwd.equals(confirmPwd)) { showStatus("❌ Les mots de passe ne correspondent pas.", false); return; }

        currentUser.setPassword(newPwd);
        boolean saved = userService.update(currentUser);
        if (saved) {
            showStatus("✅ Mot de passe modifié avec succès.", true);
            oldPwdField.clear(); newPwdField.clear(); confirmPwdField.clear();
        } else {
            showStatus("❌ Erreur lors du changement de mot de passe.", false);
        }
    }

    @FXML
    public void uploadPhoto() {
        if (currentUser == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        javafx.scene.Node ref = (nomField != null) ? nomField : avatarLabel;
        if (ref == null || ref.getScene() == null) return;

        File chosen = chooser.showOpenDialog(ref.getScene().getWindow());
        if (chosen == null) return;

        try {
            File photosDir = new File(System.getProperty("user.home"), "SmartPathPhotos");
            photosDir.mkdirs();
            String ext = chosen.getName().substring(chosen.getName().lastIndexOf('.'));
            File dest = new File(photosDir, "user_" + currentUser.getId() + ext);
            Files.copy(chosen.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            currentUser.setPhoto(dest.getAbsolutePath());
            userService.update(currentUser);

            loadPhoto(dest.getAbsolutePath());
            showStatus("✅ Photo mise à jour avec succès.", true);
        } catch (IOException e) {
            showStatus("❌ Impossible de copier la photo : " + e.getMessage(), false);
        }
    }

    @FXML
    public void goBack() {
        if (currentUser == null) return;
        String dest = switch (currentUser.getType()) {
            case "admin"    -> "/tn/esprit/interfaces/DashboardAdmin.fxml";
            case "prof"     -> "/tn/esprit/interfaces/DashboardProf.fxml";
            default         -> "/tn/esprit/interfaces/DashboardEtudiant.fxml";
        };
        try {
            Parent root = FXMLLoader.load(getClass().getResource(dest));
            javafx.scene.Node ref = (nomField != null) ? nomField : emailLabel;
            if (ref != null) ref.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void loadPhoto(String photoPath) {
        if (photoView == null || avatarLabel == null) return;
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File f = new File(photoPath);
                if (f.exists()) {
                    // Charger une version plus grande (meilleure qualité, surtout sur écrans HiDPI)
                    Image img = new Image(f.toURI().toString(), 256, 256, true, true);
                    photoView.setImage(img);

                    // Recadrage carré centré pour éviter la déformation (puis clip circulaire)
                    double w = img.getWidth();
                    double h = img.getHeight();
                    if (w > 0 && h > 0) {
                        double side = Math.min(w, h);
                        photoView.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
                    } else {
                        photoView.setViewport(null);
                    }
                    photoView.setPreserveRatio(false);

                    double size = Math.min(photoView.getFitWidth(), photoView.getFitHeight());
                    if (size <= 0) size = 88;
                    double r = size / 2.0;
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(r, r, r);
                    photoView.setClip(clip);
                    photoView.setVisible(true);
                    photoView.setManaged(true);
                    avatarLabel.setVisible(false);
                    avatarLabel.setManaged(false);
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (photoView != null) {
            photoView.setViewport(null);
            photoView.setClip(null);
        }
        photoView.setVisible(false);
        photoView.setManaged(false);
        avatarLabel.setVisible(true);
        avatarLabel.setManaged(true);
    }

    private String roleDisplay(String type) {
        if (type == null) return "Utilisateur";
        return switch (type) {
            case "admin"    -> "Administrateur";
            case "prof"     -> "Professeur";
            case "etudiant" -> "Étudiant";
            default         -> type;
        };
    }

    private void set(Label l, String v)            { if (l != null) l.setText(v != null ? v : "—"); }
    private void setText(TextField f, String v)    { if (f != null && v != null) f.setText(v); }
    private String nvl(String s, String def)       { return (s != null && !s.isBlank()) ? s : def; }

    private void showStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (success ? "#10b981" : "#dc2626")
            + "; -fx-font-size: 12; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }

    private void hideStatus() {
        if (statusLabel != null) { statusLabel.setText(""); statusLabel.setVisible(false); }
    }
}
