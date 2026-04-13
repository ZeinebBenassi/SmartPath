package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entity.Matiere;
import org.example.service.MatiereService;

import java.io.IOException;

public class MatiereFormController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private CheckBox visibleCheckBox;
    @FXML private Label formTitle;

    private MatiereService matiereService = new MatiereService();
    private Matiere currentMatiere; // If null, we are in "Create" mode

    public void setMatiere(Matiere m) {
        this.currentMatiere = m;
        if (m != null) {
            formTitle.setText("✏️ Modifier le cours : " + m.getTitre());
            titreField.setText(m.getTitre());
            descriptionArea.setText(m.getDescription());
            visibleCheckBox.setSelected(m.isVisible());
        }
    }

    @FXML
    private void handleSave(javafx.event.ActionEvent event) {
        String titre = titreField.getText().trim();
        String description = descriptionArea.getText().trim();

        // Control de saisie (As requested in your grid)
        if (titre.isEmpty() || titre.length() < 3) {
            showAlert("Erreur de saisie", "Le titre doit contenir au moins 3 caractères.");
            return;
        }

        if (currentMatiere == null) {
            // Logic for CREATE
            Matiere newMatiere = new Matiere();
            newMatiere.setTitre(titre);
            newMatiere.setDescription(description);
            newMatiere.setVisible(visibleCheckBox.isSelected());
            newMatiere.setProfId(1); // Default mock Prof
            
            if (matiereService.add(newMatiere)) {
                showInfo("Succès", "Le cours a été créé avec succès.");
                closeForm(event);
            }
        } else {
            // Logic for UPDATE
            currentMatiere.setTitre(titre);
            currentMatiere.setDescription(description);
            currentMatiere.setVisible(visibleCheckBox.isSelected());

            if (matiereService.update(currentMatiere)) {
                showInfo("Succès", "Le cours a été mis à jour.");
                closeForm(event);
            }
        }
    }

    @FXML
    private void handleCancel(javafx.event.ActionEvent event) {
        closeForm(event);
    }

    private void closeForm(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/cours_view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
