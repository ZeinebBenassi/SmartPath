package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class GestionFilieresController {

    @FXML private Button btnRetour;
    @FXML private TextField searchField;
    @FXML private TableView<?> filieresTable;
    @FXML private ListView<String> filieresList;
    @FXML private TextField nomField;
    @FXML private TextField categorieField;
    @FXML private TextField niveauField;
    @FXML private TextField iconField;
    @FXML private TextArea descriptionField;
    @FXML private TextArea debouchesField;
    @FXML private TextArea competencesField;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        if (filieresList != null) {
            filieresList.getItems().clear();
            filieresList.setPlaceholder(new Label("Aucune filière disponible pour le moment."));
        }
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            javafx.scene.Node src = btnRetour != null ? btnRetour : searchField;
            if (src != null) src.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleSearch() {}
    @FXML public void handleFilter() {}

    @FXML public void addFiliere() {
        new Alert(Alert.AlertType.INFORMATION,
            "Formulaire d'ajout de filière à implémenter.", ButtonType.OK).showAndWait();
    }

    @FXML public void saveFiliere() {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12;");
            messageLabel.setText("Filière enregistrée (simulation).");
        }
    }

    @FXML public void deleteFiliere() {
        if (filieresList != null) {
            String selected = filieresList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                filieresList.getItems().remove(selected);
            }
        }
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
            messageLabel.setText("Filière supprimée (simulation).");
        }
    }

    @FXML public void clearForm() {
        if (nomField != null) nomField.clear();
        if (categorieField != null) categorieField.clear();
        if (niveauField != null) niveauField.clear();
        if (iconField != null) iconField.clear();
        if (descriptionField != null) descriptionField.clear();
        if (debouchesField != null) debouchesField.clear();
        if (competencesField != null) competencesField.clear();
        if (messageLabel != null) messageLabel.setText("");
    }
}

