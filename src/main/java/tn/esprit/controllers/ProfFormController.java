package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entity.Prof;
import tn.esprit.services.AdminProfService;
import tn.esprit.services.CloudinaryService;
import tn.esprit.utils.FormValidator;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProfFormController {

    @FXML private Label         lblTitle;
    @FXML private TextField     txtNom;
    @FXML private TextField     txtPrenom;
    @FXML private TextField     txtEmail;
    @FXML private DatePicker    dpNaissance;
    @FXML private TextField     txtSpecialite;
    @FXML private TextField     txtCIN;
    @FXML private TextField     txtTelephone;
    @FXML private TextField     txtAdresse;
    @FXML private Label         lblPasswordHint;
    @FXML private PasswordField tfPassword;
    @FXML private PasswordField tfConfirm;
    @FXML private Label         lblErrors;
    @FXML private Button        btnSave;
    @FXML private Button        btnCancel;

    // ── Photo ──
    @FXML private ImageView photoPreview;
    @FXML private Label     photoFileLabel;
    private File            selectedPhotoFile = null;
    private String          currentPhotoUrl   = null;

    private final AdminProfService  service   = new AdminProfService();
    private final CloudinaryService cloudinary = new CloudinaryService();
    private Prof                   profToEdit;
    private GestionProfsController parent;
    private boolean                isEditMode = false;

    @FXML public void initialize() {}

    public void initCreate(GestionProfsController parent) {
        this.parent = parent; this.isEditMode = false;
        if (lblTitle       != null) lblTitle.setText("Créer un nouveau professeur");
        if (lblPasswordHint!= null) lblPasswordHint.setText("Mot de passe");
    }

    public void initEdit(Prof prof, GestionProfsController parent) {
        this.parent = parent; this.isEditMode = true; this.profToEdit = prof;
        if (lblTitle       != null) lblTitle.setText("Modifier le professeur");
        if (lblPasswordHint!= null) lblPasswordHint.setText("Mot de passe (laisser vide pour conserver)");
        populate(prof);
    }

    private void populate(Prof p) {
        if (p == null) return;
        set(txtNom, p.getNom()); set(txtPrenom, p.getPrenom()); set(txtEmail, p.getEmail());
        set(txtSpecialite, p.getSpecialite()); set(txtCIN, p.getCin());
        set(txtTelephone, p.getTelephone()); set(txtAdresse, p.getAdresse());
        if (dpNaissance != null && p.getDateNaissance() != null)
            dpNaissance.setValue(p.getDateNaissance().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        // Charger photo existante
        currentPhotoUrl = p.getPhoto();
        if (currentPhotoUrl != null && !currentPhotoUrl.isBlank() && photoPreview != null) {
            try {
                photoPreview.setImage(new Image(currentPhotoUrl, true));
                if (photoFileLabel != null) photoFileLabel.setText("Photo actuelle chargée");
            } catch (Exception ex) {
                System.err.println("[ProfForm] Impossible de charger la photo : " + ex.getMessage());
            }
        }
    }

    /** Ouvre le FileChooser pour choisir une photo. */
    @FXML
    private void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        Stage owner = (btnSave != null)
                ? (Stage) btnSave.getScene().getWindow()
                : (Stage) btnCancel.getScene().getWindow();
        File file = chooser.showOpenDialog(owner);
        if (file != null) {
            selectedPhotoFile = file;
            if (photoPreview  != null) photoPreview.setImage(new Image(file.toURI().toString()));
            if (photoFileLabel != null) photoFileLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleSave() {
        List<String> errors = new ArrayList<>();
        boolean ok = true;
        ok &= FormValidator.validateRequired(txtNom,    "Le nom",    errors);
        ok &= FormValidator.validateRequired(txtPrenom, "Le prénom", errors);
        ok &= FormValidator.validateEmail(txtEmail, errors);
        ok &= FormValidator.validatePassword(tfPassword, !isEditMode, errors);
        ok &= FormValidator.validatePasswordMatch(tfPassword, tfConfirm, errors);
        ok &= FormValidator.validateCIN(txtCIN, errors);
        ok &= FormValidator.validatePhone(txtTelephone, errors);
        if (lblErrors != null) { lblErrors.setManaged(!errors.isEmpty()); lblErrors.setVisible(!errors.isEmpty()); }
        FormValidator.showErrors(lblErrors, errors);
        if (!ok) return;

        try {
            Prof p = isEditMode ? profToEdit : new Prof();
            p.setNom(txtNom.getText().trim()); p.setPrenom(txtPrenom.getText().trim());
            p.setEmail(txtEmail.getText().trim()); p.setSpecialite(txt(txtSpecialite));
            p.setCin(txt(txtCIN)); p.setTelephone(txt(txtTelephone)); p.setAdresse(txt(txtAdresse));
            if (dpNaissance != null && dpNaissance.getValue() != null)
                p.setDateNaissance(Date.from(dpNaissance.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            String pwd = tfPassword != null ? tfPassword.getText() : "";
            if (!pwd.isEmpty()) p.setPassword(pwd);

            // ── Gestion photo ───────────────────────────────────────────────
            if (selectedPhotoFile != null) {
                System.out.println("[ProfForm] Upload photo : " + selectedPhotoFile.getAbsolutePath());
                String url = cloudinary.uploadImage(
                        selectedPhotoFile,
                        "prof_" + p.getEmail().replaceAll("[^a-zA-Z0-9]", "_")
                );
                if (url != null && !url.isBlank()) {
                    p.setPhoto(url);
                    System.out.println("[ProfForm] ✅ Photo : " + url);
                } else {
                    System.err.println("[ProfForm] ⚠️ Upload échoué, photo inchangée.");
                    p.setPhoto(currentPhotoUrl);
                }
            } else {
                p.setPhoto(currentPhotoUrl);
            }

            if (isEditMode) service.modifier(p); else service.ajouter(p);
            if (parent != null) parent.loadData();
            close();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }

    @FXML private void handleCancel() { close(); }

    private void close() {
        Stage s = btnCancel != null ? (Stage) btnCancel.getScene().getWindow() : (Stage) btnSave.getScene().getWindow();
        s.close();
    }

    private void set(TextField f, String v) { if (f != null) f.setText(v != null ? v : ""); }
    private String txt(TextField f) { if (f==null) return null; String v=f.getText().trim(); return v.isEmpty()?null:v; }
    private void showError(String t, String msg) { Alert a=new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
}