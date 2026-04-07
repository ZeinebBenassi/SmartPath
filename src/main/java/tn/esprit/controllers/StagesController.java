package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import models.User;

public class StagesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ListView<String> offresList;
    @FXML private Label offreTitre;
    @FXML private Label offreEntreprise;
    @FXML private Label offreDuree;
    @FXML private Label offreType;
    @FXML private Label offreDescription;
    @FXML private Label offreCompetences;
    @FXML private Button btnPostuler;

    private static User currentUser;
    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (typeFilter != null) {
            typeFilter.getItems().clear();
            typeFilter.setPromptText("Filtrer");
        }

        if (offresList != null) {
            offresList.getItems().clear();
            offresList.setPlaceholder(new Label("Aucune offre disponible pour le moment."));
        }

        clearOffreDetails();
    }

    private void displayOffre(String titre) {
        String[] parts = titre.split(" - ");
        if (offreTitre      != null) offreTitre.setText(parts[0]);
        if (offreEntreprise != null) offreEntreprise.setText(parts.length > 1 ? parts[1] : "");
        if (offreDuree      != null) offreDuree.setText("6 mois");
        if (offreType       != null) offreType.setText("Stage PFE");
        if (offreDescription!= null) offreDescription.setText(
            "Nous recherchons un(e) stagiaire motivÃ©(e) pour rejoindre notre Ã©quipe. " +
            "Vous participerez au dÃ©veloppement de nouvelles fonctionnalitÃ©s et Ã  l'amÃ©lioration " +
            "de nos systÃ¨mes existants dans un environnement agile et dynamique.");
        if (offreCompetences!= null) offreCompetences.setText("Java, Spring Boot, SQL, Git, Agile");
        if (btnPostuler     != null) btnPostuler.setVisible(true);
    }

    private void clearOffreDetails() {
        if (offreTitre != null) offreTitre.setText("Sélectionnez une offre");
        if (offreEntreprise != null) offreEntreprise.setText("");
        if (offreDuree != null) offreDuree.setText("");
        if (offreType != null) offreType.setText("");
        if (offreDescription != null) offreDescription.setText("Aucune description disponible.");
        if (offreCompetences != null) offreCompetences.setText("Aucune compétence renseignée.");
        if (btnPostuler != null) {
            btnPostuler.setDisable(true);
            btnPostuler.setVisible(false);
            btnPostuler.setManaged(false);
        }
    }

    @FXML public void postuler() {
        new Alert(Alert.AlertType.INFORMATION,
            "Votre candidature a Ã©tÃ© envoyÃ©e avec succÃ¨s !", ButtonType.OK).showAndWait();
    }

    @FXML public void handleSearch() {}
    @FXML public void handleFilter() {}

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            if (offresList != null) offresList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

