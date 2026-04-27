package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.entity.Etudiant;
import tn.esprit.entity.User;
import tn.esprit.services.AdminEtudiantService;
import tn.esprit.services.CloudinaryService;
import tn.esprit.services.UserService;
import tn.esprit.utils.FormValidator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil dedie aux etudiants, charge dans le contentArea du DashboardEtudiant.
 * Affiche niveau, filiere et statut en plus des infos communes.
 * Supporte l'upload de photo de profil.
 */
public class ProfilEtudiantController {

    @FXML private StackPane avatarPane;
    @FXML private Label avatarLabel;
    @FXML private ImageView photoView;
    @FXML private Label nomCompletLabel;
    @FXML private Label niveauBadge;
    @FXML private Label emailLabel;
    @FXML private Label telephoneLabel;
    @FXML private Label adresseLabel;
    @FXML private Label cinLabel;
    @FXML private Label niveauLabel;
    @FXML private Label filiereLabel;
    @FXML private Label dateNaissanceLabel;
    @FXML private Label membreDepuisLabel;

    @FXML private Label niveauCardLabel;
    @FXML private Label filiereCardLabel;
    @FXML private Label statusCardLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField telephoneField;
    @FXML private TextField adresseField;

    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;
    @FXML private Label statusLabel;

    private static User currentUser;
    private final UserService userService = new UserService();
    private final AdminEtudiantService etudiantService = new AdminEtudiantService();
    private final CloudinaryService cloudinary = new CloudinaryService();

    public static void setCurrentUser(User u) {
        currentUser = u;
    }

    @FXML
    public void initialize() {
        if (currentUser == null) return;

        String initiale = (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty())
                ? String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase()
                : "E";

        set(avatarLabel, initiale);
        set(nomCompletLabel, currentUser.getPrenom() + " " + currentUser.getNom());
        set(emailLabel, currentUser.getEmail());
        set(telephoneLabel, nvl(currentUser.getTelephone(), "Non renseigne"));
        set(adresseLabel, nvl(currentUser.getAdresse(), "Non renseignee"));
        set(cinLabel, nvl(currentUser.getCin(), "Non renseigne"));
        set(membreDepuisLabel, currentUser.getCreatedAt() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getCreatedAt())
                : "-");
        set(dateNaissanceLabel, currentUser.getDateNaissance() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(currentUser.getDateNaissance())
                : "Non renseignee");

        loadPhoto(currentUser.getPhoto());

        String niveau = "-";
        String filiere = "-";
        String statut = "actif";
        try {
            List<Etudiant> tous = etudiantService.getAll();
            for (Etudiant e : tous) {
                if (e.getId() == currentUser.getId()) {
                    niveau = nvl(e.getNiveau(), "-");
                    statut = nvl(e.getStatus(), "actif");
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        set(niveauBadge, "Niveau " + niveau);
        set(niveauLabel, niveau);
        set(filiereLabel, filiere);
        set(niveauCardLabel, niveau);
        set(filiereCardLabel, filiere);
        set(statusCardLabel, "ban".equalsIgnoreCase(statut) ? "Banni" : "Actif");

        setText(nomField, currentUser.getNom());
        setText(prenomField, currentUser.getPrenom());
        setText(telephoneField, currentUser.getTelephone());
        setText(adresseField, currentUser.getAdresse());

        if (statusLabel != null) {
            statusLabel.setText("");
            statusLabel.setVisible(false);
        }
    }

    @FXML
    public void saveProfile() {
        if (currentUser == null) return;

        List<String> errors = new ArrayList<>();
        boolean ok = true;
        ok &= FormValidator.validateRequired(nomField, "Le nom", errors);
        ok &= FormValidator.validateRequired(prenomField, "Le prenom", errors);
        ok &= FormValidator.validatePhone(telephoneField, errors);
        FormValidator.showErrors(statusLabel, errors);
        if (!ok) return;

        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        if (telephoneField != null && !telephoneField.getText().trim().isEmpty()) {
            currentUser.setTelephone(telephoneField.getText().trim());
        }
        if (adresseField != null && !adresseField.getText().trim().isEmpty()) {
            currentUser.setAdresse(adresseField.getText().trim());
        }

        boolean saved = userService.update(currentUser);
        if (saved) {
            showStatus("Profil mis a jour avec succes.", true);
            initialize();
        } else {
            showStatus("Erreur lors de la sauvegarde.", false);
        }
    }

    @FXML
    public void changePassword() {
        if (oldPwdField == null || newPwdField == null || confirmPwdField == null) return;

        String oldPwd = oldPwdField.getText().trim();
        String newPwd = newPwdField.getText();
        String confirm = confirmPwdField.getText();

        if (oldPwd.isEmpty()) {
            showStatus("Saisissez votre mot de passe actuel.", false);
            return;
        }
        if (newPwd.isEmpty()) {
            showStatus("Saisissez un nouveau mot de passe.", false);
            return;
        }
        if (newPwd.length() < 6) {
            showStatus("Le mot de passe doit contenir au moins 6 caracteres.", false);
            return;
        }
        if (!newPwd.equals(confirm)) {
            showStatus("Les mots de passe ne correspondent pas.", false);
            return;
        }

        currentUser.setPassword(newPwd);
        boolean saved = userService.update(currentUser);
        if (saved) {
            showStatus("Mot de passe modifie avec succes.", true);
            oldPwdField.clear();
            newPwdField.clear();
            confirmPwdField.clear();
        } else {
            showStatus("Erreur lors du changement de mot de passe.", false);
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

        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        javafx.scene.Node ref = (nomField != null) ? nomField : avatarLabel;
        if (ref == null || ref.getScene() == null) return;

        File chosen = chooser.showOpenDialog(ref.getScene().getWindow());
        if (chosen == null) return;

        showStatus("Upload en cours vers le cloud...", true);

        Thread uploadThread = new Thread(() -> {
            String oldUrl = currentUser.getPhoto();
            if (CloudinaryService.isCloudinaryUrl(oldUrl)) {
                String oldPublicId = CloudinaryService.extractPublicId(oldUrl);
                cloudinary.deleteImage(oldPublicId);
            }

            String publicId = "user_" + currentUser.getId();
            String newUrl = cloudinary.uploadImage(chosen, publicId);

            Platform.runLater(() -> {
                if (newUrl != null && !newUrl.isBlank()) {
                    currentUser.setPhoto(newUrl);
                    userService.update(currentUser);
                    loadPhoto(newUrl);
                    showStatus("Photo mise a jour avec succes.", true);
                } else {
                    showStatus("Echec de l'upload de la photo.", false);
                }
            });
        });
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

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
                    java.net.URL url = new java.net.URL(photoPath);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(15000);
                    conn.connect();
                    try (java.io.InputStream is = conn.getInputStream()) {
                        img = new Image(is);
                    } finally {
                        conn.disconnect();
                    }
                } else {
                    File f = new File(photoPath);
                    if (!f.exists()) throw new java.io.IOException("Fichier introuvable : " + photoPath);
                    try (java.io.InputStream is = new java.io.FileInputStream(f)) {
                        img = new Image(is);
                    }
                }

                if (img.isError()) {
                    Platform.runLater(this::showAvatar);
                } else {
                    Platform.runLater(() -> applyPhoto(img));
                }
            } catch (Exception ignored) {
                Platform.runLater(this::showAvatar);
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void applyPhoto(Image img) {
        photoView.setImage(img);

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
    }

    private void showAvatar() {
        if (photoView != null) {
            photoView.setViewport(null);
            photoView.setClip(null);
            photoView.setVisible(false);
            photoView.setManaged(false);
        }
        avatarLabel.setVisible(true);
        avatarLabel.setManaged(true);
    }

    private void set(Label l, String v) {
        if (l != null) l.setText(v != null ? v : "-");
    }

    private void setText(TextField f, String v) {
        if (f != null && v != null) f.setText(v);
    }

    private String nvl(String s, String def) {
        return (s != null && !s.isBlank()) ? s : def;
    }

    private void showStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (success ? "#10b981" : "#dc2626")
                + "; -fx-font-size: 12; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }
}
