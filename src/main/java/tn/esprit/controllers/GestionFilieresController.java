package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class GestionFilieresController {

    @FXML private Button btnRetour;
    @FXML private TextField searchField;
    @FXML private TableView<?> filieresTable;
    @FXML private Label countLabel;

    @FXML
    public void initialize() {
        if (countLabel != null) countLabel.setText("0 filières");
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardAdmin.fxml"));
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
}

