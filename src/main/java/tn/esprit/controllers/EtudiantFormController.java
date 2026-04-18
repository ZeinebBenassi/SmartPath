package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entity.Etudiant;
import tn.esprit.services.AdminEtudiantService;
import tn.esprit.utils.FormValidator;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EtudiantFormController {

    @FXML private TextField     txtNom;
    @FXML private TextField     txtPrenom;
    @FXML private TextField     txtEmail;
    @FXML private DatePicker    dpNaissance;
    @FXML private TextField     txtNiveau;
    @FXML private TextField     txtCIN;
    @FXML private TextField     txtTelephone;
    @FXML private TextField     txtAdresse;
    @FXML private Label         lblPasswordHint;
    @FXML private PasswordField tfPassword;
    @FXML private PasswordField tfConfirm;
    @FXML private Label         lblErrors;
    @FXML private Button        btnSave;
    @FXML private Button        btnCancel;

    private final AdminEtudiantService service    = new AdminEtudiantService();
    private Etudiant                   etudiantToEdit;
    private GestionEtudiantsController parent;
    private boolean                    isEditMode = false;

    @FXML public void initialize() {}

    public void initCreate(GestionEtudiantsController parent) {
        this.parent = parent; this.isEditMode = false;
        if (lblPasswordHint != null) lblPasswordHint.setText("Mot de passe");
    }

    public void initEdit(Etudiant etudiant, GestionEtudiantsController parent) {
        this.parent = parent; this.isEditMode = true; this.etudiantToEdit = etudiant;
        if (lblPasswordHint != null) lblPasswordHint.setText("Mot de passe (laisser vide pour conserver)");
        populate(etudiant);
    }

    private void populate(Etudiant e) {
        if (e == null) return;
        set(txtNom, e.getNom()); set(txtPrenom, e.getPrenom()); set(txtEmail, e.getEmail());
        set(txtNiveau, e.getNiveau()); set(txtCIN, e.getCin());
        set(txtTelephone, e.getTelephone()); set(txtAdresse, e.getAdresse());
        if (dpNaissance != null && e.getDateNaissance() != null)
            dpNaissance.setValue(e.getDateNaissance().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
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
            Etudiant e = isEditMode ? etudiantToEdit : new Etudiant();
            e.setNom(txtNom.getText().trim()); e.setPrenom(txtPrenom.getText().trim());
            e.setEmail(txtEmail.getText().trim());
            e.setNiveau(txt(txtNiveau) != null ? txt(txtNiveau) : "L1");
            e.setCin(txt(txtCIN)); e.setTelephone(txt(txtTelephone)); e.setAdresse(txt(txtAdresse));
            if (dpNaissance != null && dpNaissance.getValue() != null)
                e.setDateNaissance(Date.from(dpNaissance.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            String pwd = tfPassword != null ? tfPassword.getText() : "";
            if (!pwd.isEmpty()) e.setPassword(pwd);
            if (isEditMode) service.modifier(e); else service.ajouter(e);
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