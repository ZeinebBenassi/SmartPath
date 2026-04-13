package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;

public class UserFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cbType;
    @FXML private PasswordField tfPassword;
    @FXML private Label lblPassword;
    @FXML private TextField txtCIN;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtAdresse;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private UserService userService = new UserService();
    private User userToEdit;
    private GestionUsersController parentController;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        // Initialiser les types d'utilisateurs
        cbType.setItems(FXCollections.observableArrayList("admin", "prof", "etudiant"));
        cbType.getSelectionModel().selectFirst();

        // Style du ComboBox
        cbType.setStyle("-fx-font-size: 13px; -fx-padding: 8 12; -fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;");
    }

    /**
     * Initialiser le contrôleur en mode création
     */
    public void initializeForCreate(GestionUsersController parentController) {
        this.parentController = parentController;
        this.isEditMode = false;
        this.userToEdit = null;
        lblTitle.setText("➕  Nouvel Utilisateur");
        lblPassword.setText("Mot de passe *");
        tfPassword.setPromptText("Entrer le mot de passe");
        clearForm();
    }

    /**
     * Initialiser le contrôleur en mode édition
     */
    public void initializeForEdit(User user, GestionUsersController parentController) {
        this.parentController = parentController;
        this.isEditMode = true;
        this.userToEdit = user;
        lblTitle.setText("✏️  Modifier Utilisateur");
        lblPassword.setText("Mot de passe (laisser vide pour conserver)");
        tfPassword.setPromptText("Laisser vide pour conserver le mot de passe");
        populateForm(user);
    }

    private void populateForm(User user) {
        if (user != null) {
            txtNom.setText(user.getNom() != null ? user.getNom() : "");
            txtPrenom.setText(user.getPrenom() != null ? user.getPrenom() : "");
            txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            cbType.setValue(user.getType() != null ? user.getType() : "etudiant");
            txtCIN.setText(user.getCin() != null ? user.getCin() : "");
            txtTelephone.setText(user.getTelephone() != null ? user.getTelephone() : "");
            txtAdresse.setText(user.getAdresse() != null ? user.getAdresse() : "");
        }
    }

    private void clearForm() {
        txtNom.clear();
        txtPrenom.clear();
        txtEmail.clear();
        tfPassword.clear();
        txtCIN.clear();
        txtTelephone.clear();
        txtAdresse.clear();
        cbType.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSave() {
        try {
            // Validation
            if (txtNom.getText().isEmpty()) {
                showError("Validation", "Le nom est obligatoire");
                return;
            }
            if (txtPrenom.getText().isEmpty()) {
                showError("Validation", "Le prénom est obligatoire");
                return;
            }
            if (txtEmail.getText().isEmpty()) {
                showError("Validation", "L'email est obligatoire");
                return;
            }

            if (!isEditMode && tfPassword.getText().isEmpty()) {
                showError("Validation", "Le mot de passe est obligatoire pour un nouvel utilisateur");
                return;
            }

            User user = isEditMode ? userToEdit : new User();
            user.setNom(txtNom.getText());
            user.setPrenom(txtPrenom.getText());
            user.setEmail(txtEmail.getText());
            user.setType(cbType.getValue() != null ? cbType.getValue() : "etudiant");
            user.setCin(txtCIN.getText().isEmpty() ? null : txtCIN.getText());
            user.setTelephone(txtTelephone.getText().isEmpty() ? null : txtTelephone.getText());
            user.setAdresse(txtAdresse.getText().isEmpty() ? null : txtAdresse.getText());

            // Gérer le mot de passe
            String password = tfPassword.getText();
            if (!password.isEmpty()) {
                user.setPassword(password);
            }

            if (isEditMode) {
                // Modifier
                if (userService.update(user)) {
                    showSuccess("Succès", "Utilisateur modifié avec succès !\n\n" + user.getNom() + " " + user.getPrenom());
                    if (parentController != null) parentController.loadUsers();
                    closeWindow();
                } else {
                    showError("Erreur", "Impossible de modifier l'utilisateur");
                }
            } else {
                // Créer
                int newId = userService.create(user);
                if (newId > 0) {
                    showSuccess("Succès", "Utilisateur créé avec succès !\n\n" + user.getNom() + " " + user.getPrenom());
                    if (parentController != null) parentController.loadUsers();
                    closeWindow();
                } else {
                    showError("Erreur", "Impossible de créer l'utilisateur");
                }
            }
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-padding: 20;");
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-padding: 20;");
        alert.showAndWait();
    }
}
