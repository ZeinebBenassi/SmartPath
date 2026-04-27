package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import tn.esprit.entity.User;
import tn.esprit.services.FaceAuthService;
import tn.esprit.services.UserService;

import java.util.List;

public class LoginController {

    // ── Champs email / password ───────────────────────────────────────────────
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    // ── Section Face Auth ─────────────────────────────────────────────────────
    @FXML private Button    faceToggleBtn;   // simple Button (pas ToggleButton)
    @FXML private VBox      facePanel;
    @FXML private ImageView cameraPreview;
    @FXML private Label     faceStatusLabel;
    @FXML private Button    btnStartCamera;
    @FXML private Button    btnVerifyFace;
    @FXML private VBox      passwordGroup;

    // ── Services ──────────────────────────────────────────────────────────────
    private final UserService     userService  = new UserService();
    private final FaceAuthService faceService  = FaceAuthService.getInstance();

    // ── État interne ──────────────────────────────────────────────────────────
    private boolean  faceMode    = false;   // true = mode face auth activé
    private boolean  faceVerified = false;
    private Timeline cameraTimeline;
    private Mat      lastFrame;

    private static final String STYLE_BTN_ON  =
        "-fx-background-color: #2563eb; -fx-text-fill: white; " +
        "-fx-font-size: 11; -fx-font-weight: bold; " +
        "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14; -fx-border-width: 0;";
    private static final String STYLE_BTN_OFF =
        "-fx-background-color: #cbd5e1; -fx-text-fill: white; " +
        "-fx-font-size: 11; -fx-font-weight: bold; " +
        "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14; -fx-border-width: 0;";

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialisation
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        facePanel.setVisible(false);
        facePanel.setManaged(false);
        btnVerifyFace.setDisable(true);
        applyToggleStyle();

        if (!faceService.isAvailable()) {
            faceToggleBtn.setDisable(true);
            faceToggleBtn.setTooltip(new Tooltip("OpenCV non disponible sur ce système."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Toggle Face Auth  (appelé par onAction="#handleFaceToggle" dans le FXML)
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleFaceToggle() {
        faceMode = !faceMode;          // simple bascule booléenne
        faceVerified = false;
        applyToggleStyle();

        facePanel.setVisible(faceMode);
        facePanel.setManaged(faceMode);

        if (!faceMode) {
            stopCamera();
            passwordGroup.setVisible(true);
            passwordGroup.setManaged(true);
            setFaceStatus("Reconnaissance faciale désactivée.", "info");
        } else {
            passwordGroup.setVisible(false);
            passwordGroup.setManaged(false);
            setFaceStatus("Cliquez sur « Activer la caméra » pour commencer.", "info");
        }
    }

    /** Met à jour le texte et la couleur du bouton toggle selon l'état faceMode. */
    private void applyToggleStyle() {
        if (faceToggleBtn == null) return;
        faceToggleBtn.setText(faceMode ? "ON" : "OFF");
        faceToggleBtn.setStyle(faceMode ? STYLE_BTN_ON : STYLE_BTN_OFF);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Caméra
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleStartCamera() {
        if (!faceService.isAvailable()) {
            setFaceStatus("OpenCV non disponible.", "error"); return;
        }
        if (!faceService.openCamera()) {
            setFaceStatus("Impossible d'ouvrir la caméra.", "error"); return;
        }
        btnStartCamera.setDisable(true);
        btnVerifyFace.setDisable(false);
        setFaceStatus("Caméra active — positionnez votre visage puis cliquez sur Vérifier.", "info");
        startPreview();
    }

    private void startPreview() {
        cameraTimeline = new Timeline(new KeyFrame(Duration.millis(40), e -> {
            Mat frame = faceService.captureFrame();
            if (frame == null) return;
            lastFrame = frame;
            List<Rect> faces = faceService.detectFaces(frame);
            Mat display = faceService.drawFaceBoxes(frame, faces);
            javafx.scene.image.Image img = faceService.frameToFxImage(display);
            if (img != null) cameraPreview.setImage(img);
        }));
        cameraTimeline.setCycleCount(Timeline.INDEFINITE);
        cameraTimeline.play();
    }

    private void stopCamera() {
        if (cameraTimeline != null) { cameraTimeline.stop(); cameraTimeline = null; }
        faceService.closeCamera();
        if (cameraPreview  != null) cameraPreview.setImage(null);
        if (btnStartCamera != null) btnStartCamera.setDisable(false);
        if (btnVerifyFace  != null) btnVerifyFace.setDisable(true);
        lastFrame = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Vérification faciale
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleVerifyFace() {
        faceVerified = false;
        String email = emailField.getText().trim();
        if (email.isEmpty())  { setFaceStatus("Saisissez votre email avant la vérification.", "error"); return; }
        if (lastFrame == null){ setFaceStatus("Activez d'abord la caméra.", "error"); return; }

        User user = userService.findByEmail(email);
        if (user == null) { setFaceStatus("Aucun compte trouvé pour cet email.", "error"); return; }

        String refPhoto = (user.getPhotoFace() != null && !user.getPhotoFace().isBlank())
                ? user.getPhotoFace() : user.getPhoto();
        if (refPhoto == null || refPhoto.isBlank()) {
            setFaceStatus("Aucune photo de référence pour ce compte.", "error"); return;
        }

        setFaceStatus("Analyse en cours…", "info");
        btnVerifyFace.setDisable(true);
        Mat frameCopy = lastFrame.clone();

        new Thread(() -> {
            boolean match = faceService.matchFace(frameCopy, refPhoto);
            Platform.runLater(() -> {
                btnVerifyFace.setDisable(false);
                if (match) {
                    faceVerified = true;
                    setFaceStatus("✓ Visage reconnu ! Cliquez sur Se connecter.", "success");
                } else {
                    setFaceStatus("✗ Visage non reconnu. Réessayez.", "error");
                }
            });
        }, "face-verify").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Connexion
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();

        // ── Mode face ──
        if (faceMode) {
            if (email.isEmpty())  { showError("Saisissez votre email."); return; }
            if (!faceVerified)    { showError("Vérification faciale requise avant connexion."); return; }
            User user = userService.findByEmail(email);
            if (user == null)     { showError("Aucun compte trouvé pour cet email."); return; }
            if ("ban".equalsIgnoreCase(user.getStatus())) { showError("❌ Compte banni."); return; }
            stopCamera();
            navigateToDashboard(user);
            return;
        }

        // ── Mode classique ──
        String password = passwordField.getText().trim();
        if (email.isEmpty() || password.isEmpty()) { showError("Veuillez remplir tous les champs."); return; }

        User user = userService.login(email, password);
        if (user != null) {
            if ("ban".equalsIgnoreCase(user.getStatus())) { showError("❌ Compte banni."); return; }
            navigateToDashboard(user);
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation
    // ─────────────────────────────────────────────────────────────────────────
    private void navigateToDashboard(User user) {
        try {
            String fxml;
            switch (user.getType()) {
                case "admin":
                    DashboardAdminController.setCurrentUser(user);
                    DashboardEtudiantController.setSourceDashboardType(null);
                    fxml = "/tn/esprit/interfaces/DashboardAdmin.fxml"; break;
                case "prof":
                    DashboardProfController.setCurrentUser(user);
                    DashboardEtudiantController.setSourceDashboardType(null);
                    fxml = "/tn/esprit/interfaces/DashboardProf.fxml"; break;
                default:
                    DashboardEtudiantController.setCurrentUser(user);
                    DashboardEtudiantController.setSourceDashboardType(null);
                    fxml = "/tn/esprit/interfaces/DashboardEtudiant.fxml"; break;
            }
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML public void goToRegister()       { navigateTo("/tn/esprit/interfaces/Register.fxml"); }
    @FXML public void goToForgotPassword() { navigateTo("/tn/esprit/interfaces/ForgotPassword.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            emailField.getScene().setRoot(root);
        } catch (Exception e) { showError("Navigation impossible."); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers UI
    // ─────────────────────────────────────────────────────────────────────────
    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
        errorLabel.setText(msg);
    }

    private void setFaceStatus(String msg, String level) {
        if (faceStatusLabel == null) return;
        String color = switch (level) {
            case "success" -> "#059669";
            case "error"   -> "#dc2626";
            default        -> "#2563eb";
        };
        faceStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12;");
        faceStatusLabel.setText(msg);
    }
}
