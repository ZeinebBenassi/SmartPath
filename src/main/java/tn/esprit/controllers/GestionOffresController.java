package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class GestionOffresController {

    @FXML private Button btnRetour;
    @FXML private TextField searchField;
    @FXML private TableView<?> offresTable;
    @FXML private ListView<String> offresList;
    @FXML private TextField titreField;
    @FXML private TextField entrepriseField;
    @FXML private TextField lieuField;
    @FXML private ComboBox<String> typeField;
    @FXML private TextArea descriptionField;
    @FXML private ListView<String> candidaturesList;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        if (typeField != null && typeField.getItems().isEmpty()) {
            typeField.getItems().addAll("Stage PFE", "Stage ete", "Alternance", "CDI", "CDD");
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

    @FXML public void addOffre() {
        new Alert(Alert.AlertType.INFORMATION,
            "Formulaire d'ajout d'offre Ã  implÃ©menter.", ButtonType.OK).showAndWait();
    }

    @FXML public void saveOffre() {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12;");
            messageLabel.setText("Offre enregistree (simulation).");
        }
    }

    @FXML public void deleteOffre() {
        if (offresList != null) {
            String selected = offresList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                offresList.getItems().remove(selected);
            }
        }
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 12;");
            messageLabel.setText("Offre supprimee (simulation).");
        }
    }

    @FXML public void clearForm() {
        if (titreField != null) titreField.clear();
        if (entrepriseField != null) entrepriseField.clear();
        if (lieuField != null) lieuField.clear();
        if (typeField != null) typeField.getSelectionModel().clearSelection();
        if (descriptionField != null) descriptionField.clear();
        if (messageLabel != null) messageLabel.setText("");
    }
}

