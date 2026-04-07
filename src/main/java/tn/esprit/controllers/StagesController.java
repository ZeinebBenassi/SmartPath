package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
        if (btnPostuler != null) {
            btnPostuler.setDisable(currentUser == null);
        }

        if (typeFilter != null) {
            typeFilter.setItems(FXCollections.observableArrayList(
                "Tous", "Stage PFE", "Stage d'été", "Alternance", "CDI", "CDD"
            ));
            typeFilter.getSelectionModel().selectFirst();
        }

        if (offresList != null) {
            offresList.setItems(FXCollections.observableArrayList(
                "Développeur Java - TechCorp",
                "Stage Data Science - DataLab",
                "Développeur Web Full Stack - StartupX",
                "Stage Marketing Digital - MediaGroup",
                "Ingénieur Réseau - NetSolutions",
                "Analyste Financier - BankCo"
            ));
            offresList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                if (val != null) displayOffre(val);
            });
        }
    }

    private void displayOffre(String titre) {
        String[] parts = titre.split(" - ");
        if (offreTitre      != null) offreTitre.setText(parts[0]);
        if (offreEntreprise != null) offreEntreprise.setText(parts.length > 1 ? parts[1] : "");
        if (offreDuree      != null) offreDuree.setText("6 mois");
        if (offreType       != null) offreType.setText("Stage PFE");
        if (offreDescription!= null) offreDescription.setText(
            "Nous recherchons un(e) stagiaire motivé(e) pour rejoindre notre équipe. " +
            "Vous participerez au développement de nouvelles fonctionnalités et à l'amélioration " +
            "de nos systèmes existants dans un environnement agile et dynamique.");
        if (offreCompetences!= null) offreCompetences.setText("Java, Spring Boot, SQL, Git, Agile");
        if (btnPostuler     != null) btnPostuler.setVisible(true);
    }

    @FXML public void postuler() {
        new Alert(Alert.AlertType.INFORMATION,
            "Votre candidature a été envoyée avec succès !", ButtonType.OK).showAndWait();
    }

    @FXML public void handleSearch() {}
    @FXML public void handleFilter() {}

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardEtudiant.fxml"));
            if (offresList != null) offresList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

