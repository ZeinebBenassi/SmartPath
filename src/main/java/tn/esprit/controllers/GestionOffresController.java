package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

public class GestionOffresController {

    @FXML private Button btnRetour;

    @FXML
    public void initialize() {
        // Page vide - initialisation simple
    }

    @FXML
    public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (btnRetour != null) btnRetour.getScene().setRoot(root);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
}


