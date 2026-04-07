package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;

public class CoursController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> matiereFilter;
    @FXML private Button btnAdd;

    @FXML private ListView<String> leconsList;
    @FXML private Label leconTitre;
    @FXML private Label leconMatiere;
    @FXML private Label leconDuree;
    @FXML private Label leconProf;
    @FXML private Label leconDescription;
    @FXML private Label leconContenu;
    @FXML private Label leconFichier;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnTelecharger;

    private static User currentUser;
    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        // Masquer les boutons prof si étudiant
        boolean isProf = currentUser != null && "prof".equals(currentUser.getType());
        if (btnAdd    != null) btnAdd.setVisible(isProf);
        if (btnEdit   != null) btnEdit.setVisible(false);
        if (btnDelete != null) btnDelete.setVisible(false);

        // Filtre matières
        if (matiereFilter != null) {
            matiereFilter.setItems(FXCollections.observableArrayList(
                "Toutes", "Mathématiques", "Informatique", "Physique", "Économie"
            ));
            matiereFilter.getSelectionModel().selectFirst();
        }

        // Données exemple
        if (leconsList != null) {
            leconsList.setItems(FXCollections.observableArrayList(
                "Introduction à Java",
                "Structures de données",
                "Bases de données SQL",
                "Programmation orientée objet",
                "Algorithmes de tri",
                "JavaFX et interfaces graphiques"
            ));
            leconsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                if (val != null) displayLecon(val);
            });
        }
    }

    private void displayLecon(String titre) {
        if (leconTitre       != null) leconTitre.setText(titre);
        if (leconMatiere     != null) leconMatiere.setText("Informatique");
        if (leconDuree       != null) leconDuree.setText("⏱ 45 min");
        if (leconProf        != null) leconProf.setText("👨‍🏫 Prof. Dupont");
        if (leconDescription != null) leconDescription.setText(
            "Cette leçon couvre les concepts fondamentaux de " + titre +
            ". Elle est conçue pour les étudiants de niveau L2 et comprend des exercices pratiques.");
        if (leconContenu     != null) leconContenu.setText(
            "1. Introduction et objectifs\n\n" +
            "2. Concepts théoriques\n   - Point clé A\n   - Point clé B\n\n" +
            "3. Exemples et applications\n\n" +
            "4. Exercices pratiques\n\n" +
            "5. Résumé et questions de révision");
        if (leconFichier     != null) leconFichier.setText("cours_" + titre.toLowerCase().replace(" ", "_") + ".pdf");
        if (btnEdit          != null && currentUser != null && "prof".equals(currentUser.getType()))
            btnEdit.setVisible(true);
        if (btnDelete        != null && currentUser != null && "prof".equals(currentUser.getType()))
            btnDelete.setVisible(true);
        if (btnTelecharger   != null) btnTelecharger.setVisible(true);
    }

    @FXML public void addLecon() {
        navigate("/views/LeconForm.fxml");
    }

    @FXML public void editLecon() {
        navigate("/views/LeconForm.fxml");
    }

    @FXML public void deleteLecon() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette leçon ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES && leconsList != null) {
                leconsList.getItems().remove(leconsList.getSelectionModel().getSelectedItem());
            }
        });
    }

    @FXML public void telechargerFichier() {
        new Alert(Alert.AlertType.INFORMATION, "Téléchargement simulé.", ButtonType.OK).showAndWait();
    }

    @FXML public void goBack() {
        String dest = (currentUser != null && "prof".equals(currentUser.getType()))
            ? "/views/DashboardProf.fxml" : "/views/DashboardEtudiant.fxml";
        navigate(dest);
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            leconsList.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

