package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

/**
 * GestionFilieresController — utilisé uniquement pour compatibilité FXML.
 * La vraie logique est dans FiliereController (chargé via FiliereContent.fxml).
 * Ce controller NE redirige PLUS toute la scène — il reste dans le contentArea.
 */
public class GestionFilieresController {

    @FXML private Button       btnRetour;
    @FXML private TextField    searchField;
    @FXML private TableView<?> filieresTable;
    @FXML private TableColumn<?,?> colId;
    @FXML private TableColumn<?,?> colNom;
    @FXML private TableColumn<?,?> colDescription;
    @FXML private TableColumn<?,?> colActions;
    @FXML private ListView<String> filieresList;
    @FXML private TextField    nomField;
    @FXML private TextField    categorieField;
    @FXML private TextField    niveauField;
    @FXML private TextField    iconField;
    @FXML private TextArea     descriptionField;
    @FXML private TextArea     debouchesField;
    @FXML private TextArea     competencesField;
    @FXML private Label        messageLabel;

    @FXML
    public void initialize() {
        // Ne rien faire : ce FXML n'est plus utilisé directement.
        // Le DashboardAdminController charge FiliereContent.fxml directement dans contentArea.
    }

    @FXML public void addFiliere()    {}
    @FXML public void saveFiliere()   {}
    @FXML public void deleteFiliere() {}
    @FXML public void clearForm()     {}
    @FXML public void handleSearch()  {}
    @FXML public void handleFilter()  {}

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            javafx.scene.Node src = btnRetour != null ? btnRetour : searchField;
            if (src != null && src.getScene() != null) src.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
