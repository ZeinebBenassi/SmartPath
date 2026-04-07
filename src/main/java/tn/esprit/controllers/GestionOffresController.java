package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class GestionOffresController {

    @FXML private Button btnRetour;
    @FXML private TextField searchField;
    @FXML private TableView<?> offresTable;

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardAdmin.fxml"));
            javafx.scene.Node src = btnRetour != null ? btnRetour : searchField;
            if (src != null) src.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleSearch() {}

    @FXML public void addOffre() {
        new Alert(Alert.AlertType.INFORMATION,
            "Formulaire d'ajout d'offre à implémenter.", ButtonType.OK).showAndWait();
    }
}

