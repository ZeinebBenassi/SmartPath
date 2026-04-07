package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class LeconFormController {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private ComboBox<String> matiereCombo;
    @FXML private TextField dureeField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea contenuArea;
    @FXML private TextField fichierField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        if (matiereCombo != null) {
            matiereCombo.setItems(FXCollections.observableArrayList(
                "Mathématiques", "Informatique", "Physique",
                "Économie", "Anglais", "Base de données"
            ));
        }
    }

    @FXML public void save() {
        if (titreField == null || titreField.getText().isEmpty()) {
            showError("Le titre est obligatoire.");
            return;
        }
        if (matiereCombo == null || matiereCombo.getValue() == null) {
            showError("Veuillez sélectionner une matière.");
            return;
        }
        // Simulation sauvegarde
        new Alert(Alert.AlertType.INFORMATION,
            "Leçon \"" + titreField.getText() + "\" enregistrée avec succès.", ButtonType.OK).showAndWait();
        goBack();
    }

    @FXML public void cancel() { goBack(); }

    @FXML public void choisirFichier() {
        if (fichierField != null)
            fichierField.setText("lecon_" + System.currentTimeMillis() + ".pdf");
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardProf.fxml"));
            if (titreField != null) titreField.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
            errorLabel.setText(msg);
        }
    }
}

