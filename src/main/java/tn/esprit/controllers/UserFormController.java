package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;
import tn.esprit.utils.FormValidator;

import java.util.ArrayList;
import java.util.List;

public class UserFormController {

    @FXML private Label            lblTitle;
    @FXML private TextField        txtNom;
    @FXML private TextField        txtPrenom;
    @FXML private TextField        txtEmail;
    @FXML private ComboBox<String> cbType;
    @FXML private PasswordField    tfPassword;
    @FXML private PasswordField    tfConfirm;
    @FXML private Label            lblPassword;
    @FXML private TextField        txtCIN;
    @FXML private TextField        txtTelephone;
    @FXML private TextField        txtAdresse;
    @FXML private Label            lblErrors;
    @FXML private Button           btnCancel;
    @FXML private Button           btnSave;

    private final UserService          userService = new UserService();
    private User                       userToEdit;
    private GestionUsersController     parentController;
    private boolean                    isEditMode  = false;

    @FXML
    public void initialize() {
        if (cbType != null)
            cbType.setItems(FXCollections.observableArrayList("admin", "prof", "etudiant"));
    }

    public void initializeForCreate(GestionUsersController parent) {
        this.parentController = parent; this.isEditMode = false;
        if (lblTitle   != null) lblTitle.setText("➕  Nouvel Utilisateur");
        if (lblPassword!= null) lblPassword.setText("Mot de passe *");
        if (cbType     != null) cbType.getSelectionModel().selectFirst();
    }

    public void initializeForEdit(User user, GestionUsersController parent) {
        this.parentController = parent; this.isEditMode = true; this.userToEdit = user;
        if (lblTitle   != null) lblTitle.setText("✏️  Modifier Utilisateur");
        if (lblPassword!= null) lblPassword.setText("Mot de passe (laisser vide pour conserver)");
        populate(user);
    }

    private void populate(User u) {
        if (u == null) return;
        set(txtNom, u.getNom()); set(txtPrenom, u.getPrenom()); set(txtEmail, u.getEmail());
        set(txtCIN, u.getCin()); set(txtTelephone, u.getTelephone()); set(txtAdresse, u.getAdresse());
        if (cbType != null && u.getType() != null) cbType.setValue(u.getType());
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
            User user = isEditMode ? userToEdit : new User();
            user.setNom(txtNom.getText().trim()); user.setPrenom(txtPrenom.getText().trim());
            user.setEmail(txtEmail.getText().trim());
            user.setType(cbType != null && cbType.getValue() != null ? cbType.getValue() : "etudiant");
            user.setCin(txt(txtCIN)); user.setTelephone(txt(txtTelephone)); user.setAdresse(txt(txtAdresse));
            String pwd = tfPassword != null ? tfPassword.getText() : "";
            if (!pwd.isEmpty()) user.setPassword(pwd);
            if (isEditMode) {
                if (userService.update(user)) { if (parentController!=null) parentController.loadUsers(); closeWindow(); }
                else showError("Erreur", "Impossible de modifier l'utilisateur.");
            } else {
                int id = userService.create(user);
                if (id > 0) { if (parentController!=null) parentController.loadUsers(); closeWindow(); }
                else showError("Erreur", "Impossible de créer l'utilisateur.");
            }
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage s = btnCancel!=null ? (Stage)btnCancel.getScene().getWindow() : (Stage)btnSave.getScene().getWindow();
        s.close();
    }

    private void set(TextField f, String v) { if (f != null) f.setText(v != null ? v : ""); }
    private String txt(TextField f) { if (f==null) return null; String v=f.getText().trim(); return v.isEmpty()?null:v; }
    private void showError(String t, String msg) { Alert a=new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
}