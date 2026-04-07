package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;

public class DashboardProfController {

    @FXML private Label profNameLabel;
    @FXML private Label profSpecLabel;
    @FXML private Label pageTitle;

    @FXML private Label nbMatieres;
    @FXML private Label nbLecons;
    @FXML private Label nbTests;
    @FXML private Label nbEtudiants;

    @FXML private Button btnDashboard;
    @FXML private Button btnMatieres;
    @FXML private Button btnLecons;
    @FXML private Button btnTests;
    @FXML private Button btnEtudiants;
    @FXML private Button btnProfil;
    @FXML private Button btnVueEtudiant;

    @FXML private TableView<?> leconsTable;
    @FXML private TableColumn<?, ?> colTitre;
    @FXML private TableColumn<?, ?> colMatiere;
    @FXML private TableColumn<?, ?> colDuree;
    @FXML private TableColumn<?, ?> colDate;
    @FXML private TableColumn<?, ?> colActionsLecon;

    private static User currentUser;

    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser != null) {
            if (profNameLabel != null)
                profNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            if (profSpecLabel != null)
                profSpecLabel.setText("Professeur");
        }
        if (nbMatieres  != null) nbMatieres.setText("0");
        if (nbLecons    != null) nbLecons.setText("0");
        if (nbTests     != null) nbTests.setText("0");
        if (nbEtudiants != null) nbEtudiants.setText("0");

        setActiveButton(btnDashboard);
    }

    @FXML public void showDashboard() {
        if (pageTitle != null) pageTitle.setText("Tableau de bord");
        setActiveButton(btnDashboard);
    }

    @FXML public void showMatieres() {
        setActiveButton(btnMatieres);
        if (pageTitle != null) pageTitle.setText("Mes matières");
    }

    @FXML public void showLecons() {
        setActiveButton(btnLecons);
        if (pageTitle != null) pageTitle.setText("Mes leçons");
        navigate("/views/Cours.fxml", "Mes leçons");
    }

    @FXML public void showTests() {
        setActiveButton(btnTests);
        if (pageTitle != null) pageTitle.setText("Tests / Évaluations");
    }

    @FXML public void showEtudiants() {
        setActiveButton(btnEtudiants);
        if (pageTitle != null) pageTitle.setText("Mes étudiants");
    }

    @FXML public void showProfil() {
        setActiveButton(btnProfil);
        navigate("/views/Profil.fxml", "Mon profil");
    }

    @FXML public void addLecon() {
        navigate("/views/LeconForm.fxml", "Nouvelle leçon");
    }

    // ── Vue Étudiant ────────────────────────────────────────────────

    @FXML public void switchToVueEtudiant() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardEtudiant.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Logout ──────────────────────────────────────────────────────

    @FXML public void handleLogout() {
        currentUser = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void navigate(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println("Navigation " + fxml + " : " + e.getMessage());
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnMatieres, btnLecons, btnTests, btnEtudiants, btnProfil};
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #aaaacc; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
        String activeStyle = "-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
        for (Button b : all) {
            if (b != null) b.setStyle(b == active ? activeStyle : inactive);
        }
    }
}

