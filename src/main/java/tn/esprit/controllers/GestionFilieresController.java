package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

/**
 * GestionFilieresController — redirige vers FiliereView.fxml (CRUD complet)
 * Ce contrôleur est conservé pour compatibilité avec le FXML existant,
 * mais dès l'initialisation il charge la vraie vue de gestion des filières.
 */
public class GestionFilieresController {

    // Champs FXML — gardés pour éviter les erreurs de chargement FXML
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
        // Rediriger immédiatement vers la vraie vue FiliereView
        javafx.application.Platform.runLater(this::redirectToFiliereView);
    }

    private void redirectToFiliereView() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/tn/esprit/interfaces/FiliereView.fxml"));
            // Trouver un nœud de la scène courante pour récupérer la scène
            javafx.scene.Node anchor = filieresTable != null ? filieresTable
                    : (searchField != null ? searchField : btnRetour);
            if (anchor != null && anchor.getScene() != null) {
                anchor.getScene().setRoot(root);
            }
        } catch (Exception e) {
            System.err.println("Redirection FiliereView impossible : " + e.getMessage());
        }
    }

    // Stubs pour éviter les erreurs FXML
    @FXML public void addFiliere()  { redirectToFiliereView(); }
    @FXML public void saveFiliere() { redirectToFiliereView(); }
    @FXML public void deleteFiliere() { redirectToFiliereView(); }
    @FXML public void clearForm()   {}
    @FXML public void handleSearch(){}
    @FXML public void handleFilter(){}

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            javafx.scene.Node src = btnRetour != null ? btnRetour : searchField;
            if (src != null && src.getScene() != null) src.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
