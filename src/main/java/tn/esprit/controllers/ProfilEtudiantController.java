package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.entity.Etudiant;
import tn.esprit.entity.User;
import tn.esprit.services.AdminEtudiantService;
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
 * Profil dédié aux étudiants — chargé dans le contentArea du DashboardEtudiant.
 * Affiche niveau, filière et statut en plus des infos communes.
 * Supporte l'upload de photo de profil.
 */
public class ProfilEtudiantController {

    // Carte identité gauche
    @FXML private StackPane  avatarPane;
    @FXML private Label      avatarLabel;
    @FXML private ImageView  photoView;
    @FXML private Label      nomCompletLabel;
    @FXML private Label      niveauBadge;
    @FXML private Label      emailLabel;
    @FXML private Label      telephoneLabel;
    @FXML private Label      adresseLabel;
    @FXML private Label      cinLabel;
    @FXML private Label      niveauLabel;
    @FXML private Label      filiereLabel;
    @FXML private Label      dateNaissanceLabel;
    @FXML private Label      membreDepuisLabel;

    // Cartes académiques
    @FXML private Label niveauCardLabel;
    @FXML private Label filiereCardLabel;
    @FXML private Label statusCardLabel;

    // Champs éditables
    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     telephoneField;
    @FXML private TextField     adresseField;

    // Mot de passe
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;
    @FXML private Label         statusLabel;

    private static User currentUser;
    private final UserService          userService     = new UserService();
    private final AdminEtudiantService etudiantService = new AdminEtudiantService();

    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        String initiale = (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
                ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase() : "E";
        set(avatarLabel,       initiale);
        set(nomCompletLabel,   currentUser.getPrenom() + " " + currentUser.getNom());
        set(emailLabel,        currentUser.getEmail());
        set(telephoneLabel,    nvl(currentUser.getTelephone(), "Non renseigné"));
        set(adresseLabel,      nvl(currentUser.getAdresse(),   "Non renseignée"));
        set(cinLabel,          nvl(currentUser.getCin(),       "Non renseigné"));
        set(membreDepuisLabel, currentUser.getCreatedAt() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getCreatedAt()) : "—");
        set(dateNaissanceLabel, currentUser.getDateNaissance() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getDateNaissance()) : "Non renseignée");

        // Afficher la photo si disponible
        loadPhoto(currentUser.getPhoto());

        // Infos étudiant (niveau / filière / statut)
        String niveau  = "—";
        String filiere = "—";
        String statut  = "actif";
        try {
            List<Etudiant> tous = etudiantService.getAll();
            for (Etudiant e : tous) {
                if (e.getId() == currentUser.getId()) {
                    niveau = nvl(e.getNiveau(), "—");
                    statut = nvl(e.getStatus(), "actif");
                    break;
                }
            }
        } catch (Exception ignored) {}

        set(niveauBadge,      "Niveau " + niveau);
        set(niveauLabel,      niveau);
        set(filiereLabel,     filiere);
        set(niveauCardLabel,  niveau);
        set(filiereCardLabel, filiere);
        set(statusCardLabel,  "ban".equalsIgnoreCase(statut) ? "🚫 Banni" : "✅ Actif");

        // Champs édition
        setText(nomField,       currentUser.getNom());
        setText(prenomField,    currentUser.getPrenom());
        setText(telephoneField, currentUser.getTelephone());
        setText(adresseField,   currentUser.getAdresse());

        if (statusLabel != null) { statusLabel.setText(""); statusLabel.setVisible(false); }
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
            showStatus("❌ Erreur lors de la sauvegarde.", false);
        }
    }

    @FXML
    public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;
        String oldPwd  = oldPwdField.getText().trim();
        String newPwd  = newPwdField.getText();
        String confirm = confirmPwdField.getText();

        if (oldPwd.isEmpty())        { showStatus("❌ Saisissez votre mot de passe actuel.", false); return; }
        if (newPwd.isEmpty())        { showStatus("❌ Saisissez un nouveau mot de passe.", false); return; }
        if (newPwd.length() < 6)     { showStatus("❌ Le mot de passe doit contenir au moins 6 caractères.", false); return; }
        if (!newPwd.equals(confirm)) { showStatus("❌ Les mots de passe ne correspondent pas.", false); return; }

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
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        // Ouvrir sur le bureau par défaut
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        javafx.scene.Node ref = (nomField != null) ? nomField : avatarLabel;
        if (ref == null || ref.getScene() == null) return;

        File chosen = chooser.showOpenDialog(ref.getScene().getWindow());
        if (chosen == null) return;

        try {
            // Copier vers un dossier photos dans le répertoire utilisateur
            File photosDir = new File(System.getProperty("user.home"), "SmartPathPhotos");
            photosDir.mkdirs();
            String ext = chosen.getName().substring(chosen.getName().lastIndexOf('.'));
            File dest = new File(photosDir, "user_" + currentUser.getId() + ext);
            Files.copy(chosen.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Sauvegarder le chemin en base
            currentUser.setPhoto(dest.getAbsolutePath());
            userService.update(currentUser);

            // Afficher immédiatement
            loadPhoto(dest.getAbsolutePath());
            showStatus("✅ Photo mise à jour avec succès.", true);
        } catch (IOException e) {
            showStatus("❌ Impossible de copier la photo : " + e.getMessage(), false);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void loadPhoto(String photoPath) {
        if (photoView == null || avatarLabel == null) return;
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File f = new File(photoPath);
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(), 88, 88, false, true);
                    photoView.setImage(img);
                    // Appliquer un clip circulaire
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(44, 44, 44);
                    photoView.setClip(clip);
                    photoView.setVisible(true);
                    photoView.setManaged(true);
                    avatarLabel.setVisible(false);
                    avatarLabel.setManaged(false);
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Pas de photo : afficher l'initiale
        photoView.setVisible(false);
        photoView.setManaged(false);
        avatarLabel.setVisible(true);
        avatarLabel.setManaged(true);
    }

    private void set(Label l, String v)          { if (l != null) l.setText(v != null ? v : "—"); }
    private void setText(TextField f, String v)  { if (f != null && v != null) f.setText(v); }
    private String nvl(String s, String def)     { return (s != null && !s.isBlank()) ? s : def; }

    private void showStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (success ? "#10b981" : "#dc2626")
                + "; -fx-font-size: 12; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }
}
