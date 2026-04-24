package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import tn.esprit.entity.User;
import tn.esprit.services.CloudinaryService;
import tn.esprit.services.UserService;
import tn.esprit.utils.FormValidator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil utilisateur (admin / prof / étudiant).
 * Les photos sont stockées sur Cloudinary ; seule l'URL est sauvegardée en base.
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

    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     telephoneField;
    @FXML private TextField     adresseField;

    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;

    @FXML private Label statusLabel;

    private static User currentUser;

    private final UserService      userService  = new UserService();
    private final CloudinaryService cloudinary  = new CloudinaryService();

    public static void setCurrentUser(User u) { currentUser = u; }

    // ── Initialisation ───────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        String initiale = (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
                ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase() : "?";

        set(avatarLabel,        initiale);
        set(nomCompletLabel,    currentUser.getPrenom() + " " + currentUser.getNom());
        set(roleLabel,          roleDisplay(currentUser.getType()));
        set(emailLabel,         currentUser.getEmail());
        set(telephoneLabel,     nvl(currentUser.getTelephone(), "Non renseigné"));
        set(adresseLabel,       nvl(currentUser.getAdresse(),   "Non renseignée"));
        set(cinLabel,           nvl(currentUser.getCin(),       "Non renseigné"));
        set(membreDepuisLabel,  currentUser.getCreatedAt() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getCreatedAt()) : "—");
        set(dateNaissanceLabel, currentUser.getDateNaissance() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getDateNaissance()) : "Non renseignée");

        loadPhoto(currentUser.getPhoto());

        setText(nomField,       currentUser.getNom());
        setText(prenomField,    currentUser.getPrenom());
        setText(telephoneField, currentUser.getTelephone());
        setText(adresseField,   currentUser.getAdresse());

        hideStatus();
    }

    // ── Actions ──────────────────────────────────────────────────────────────

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

        if (userService.update(currentUser)) {
            showStatus("✅ Profil mis à jour avec succès.", true);
            initialize();
        } else {
            showStatus("❌ Erreur lors de la sauvegarde.", false);
        }
    }

    @FXML
    public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;

        String oldPwd     = oldPwdField.getText().trim();
        String newPwd     = newPwdField.getText();
        String confirmPwd = confirmPwdField.getText();

        if (oldPwd.isEmpty())             { showStatus("❌ Saisissez votre mot de passe actuel.", false);  return; }
        if (newPwd.isEmpty())             { showStatus("❌ Saisissez un nouveau mot de passe.", false);     return; }
        if (newPwd.length() < 6)          { showStatus("❌ Minimum 6 caractères.", false);                  return; }
        if (!newPwd.equals(confirmPwd))   { showStatus("❌ Les mots de passe ne correspondent pas.", false);return; }

        currentUser.setPassword(newPwd);
        if (userService.update(currentUser)) {
            showStatus("✅ Mot de passe modifié avec succès.", true);
            oldPwdField.clear(); newPwdField.clear(); confirmPwdField.clear();
        } else {
            showStatus("❌ Erreur lors du changement de mot de passe.", false);
        }
    }

    /**
     * Upload la photo choisie vers Cloudinary, sauvegarde l'URL en base,
     * et rafraîchit l'affichage — tout en thread séparé pour ne pas bloquer l'UI.
     */
    @FXML
    public void uploadPhoto() {
        if (currentUser == null) return;

        // 1. Ouvrir le sélecteur de fichier
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        javafx.scene.Node ref = nomField != null ? nomField : avatarLabel;
        if (ref == null || ref.getScene() == null) return;

        File chosen = chooser.showOpenDialog(ref.getScene().getWindow());
        if (chosen == null) return;

        showStatus("⏳ Upload en cours vers le cloud…", true);

        // 2. Upload dans un thread séparé
        Thread uploadThread = new Thread(() -> {
            // 2a. Supprimer l'ancienne image Cloudinary si elle existe
            String oldUrl = currentUser.getPhoto();
            if (CloudinaryService.isCloudinaryUrl(oldUrl)) {
                String oldPublicId = CloudinaryService.extractPublicId(oldUrl);
                cloudinary.deleteImage(oldPublicId);
            }

            // 2b. Upload de la nouvelle image avec public_id fixe → overwrite automatique
            String publicId = "user_" + currentUser.getId();
            String newUrl   = cloudinary.uploadImage(chosen, publicId);

            Platform.runLater(() -> {
                if (newUrl != null) {
                    currentUser.setPhoto(newUrl);  // ← URL Cloudinary en base
                    userService.update(currentUser);
                    loadPhoto(newUrl);
                    showStatus("✅ Photo mise à jour avec succès.", true);
                } else {
                    showStatus("❌ Échec de l'upload. Vérifiez votre connexion et vos credentials Cloudinary.", false);
                }
            });
        });
        uploadThread.setDaemon(true);
        uploadThread.start();
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
            javafx.scene.Node ref = nomField != null ? nomField : emailLabel;
            if (ref != null) ref.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Chargement de la photo ───────────────────────────────────────────────

    /**
     * Charge et affiche la photo depuis :
     *  - une URL Cloudinary (https://...)
     *  - un chemin local legacy (compatibilité ascendante)
     * Le chargement se fait en arrière-plan pour ne pas geler l'UI.
     */
    private void loadPhoto(String photoPath) {
        if (photoView == null || avatarLabel == null) return;

        if (photoPath == null || photoPath.isBlank()) {
            showAvatar();
            return;
        }

        Thread loadThread = new Thread(() -> {
            try {
                Image img;

                if (photoPath.startsWith("http")) {
                    // ✅ URL Cloudinary — JavaFX charge directement depuis le réseau
                    img = new Image(photoPath, 256, 256, true, true, true);
                } else {
                    // Legacy : chemin local
                    File f = new File(photoPath);
                    if (!f.exists()) throw new IOException("Fichier introuvable : " + photoPath);
                    img = new Image(f.toURI().toString(), 256, 256, true, true, true);
                }

                final Image finalImg = img;
                Platform.runLater(() -> applyPhoto(finalImg));

            } catch (Exception e) {
                System.err.println("⚠️ Impossible de charger la photo : " + e.getMessage());
                Platform.runLater(this::showAvatar);
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    /** Applique l'image dans l'ImageView avec clip circulaire. */
    private void applyPhoto(Image img) {
        photoView.setImage(img);

        double w = img.getWidth(), h = img.getHeight();
        if (w > 0 && h > 0) {
            double side = Math.min(w, h);
            photoView.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
        } else {
            photoView.setViewport(null);
        }
        photoView.setPreserveRatio(false);

        double size = photoView.getFitWidth() > 0 ? photoView.getFitWidth() : 88;
        double r    = size / 2.0;
        photoView.setClip(new Circle(r, r, r));

        photoView.setVisible(true);
        photoView.setManaged(true);
        avatarLabel.setVisible(false);
        avatarLabel.setManaged(false);
    }

    /** Affiche l'initiale à la place de la photo. */
    private void showAvatar() {
        if (photoView != null) {
            photoView.setViewport(null);
            photoView.setClip(null);
            photoView.setVisible(false);
            photoView.setManaged(false);
        }
        if (avatarLabel != null) {
            avatarLabel.setVisible(true);
            avatarLabel.setManaged(true);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String roleDisplay(String type) {
        if (type == null) return "Utilisateur";
        return switch (type) {
            case "admin"    -> "Administrateur";
            case "prof"     -> "Professeur";
            case "etudiant" -> "Étudiant";
            default         -> type;
        };
    }

    private void set(Label l, String v)         { if (l != null) l.setText(v != null ? v : "—"); }
    private void setText(TextField f, String v) { if (f != null && v != null) f.setText(v); }
    private String nvl(String s, String def)    { return (s != null && !s.isBlank()) ? s : def; }

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
