package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entity.User;

public class DashboardProfController {

    @FXML private Label profNameLabel;
    @FXML private Label profSpecLabel;
    @FXML private Label pageTitle;

    @FXML private StackPane contentArea;
    @FXML private VBox dashboardView;

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

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    @FXML
    public void initialize() {
        if (currentUser != null) {
            if (profNameLabel != null)
                profNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            if (profSpecLabel != null)
                profSpecLabel.setText("Professeur");
        }

        if (nbMatieres != null) nbMatieres.setText("0");
        if (nbLecons   != null) nbLecons.setText("0");
        if (nbTests    != null) nbTests.setText("0");
        if (nbEtudiants!= null) nbEtudiants.setText("0");

        setActiveButton(btnDashboard);
        showOnly(dashboardView);
        if (pageTitle != null) pageTitle.setText("Tableau de bord");
    }

    @FXML
    public void showDashboard() {
        setActiveButton(btnDashboard);
        if (pageTitle != null) pageTitle.setText("Tableau de bord");
        showOnly(dashboardView);
    }

    @FXML
    public void showMatieres() {
        setActiveButton(btnMatieres);
        if (pageTitle != null) pageTitle.setText("Mes matières");
        showOnly(dashboardView);
    }

    @FXML
    public void showLecons() {
        setActiveButton(btnLecons);
        navigate("/tn/esprit/interfaces/Cours.fxml", "Mes leçons");
    }

    @FXML
    public void showTests() {
        setActiveButton(btnTests);
        if (pageTitle != null) pageTitle.setText("Tests / Évaluations");
        showOnly(dashboardView);
    }

    @FXML
    public void showEtudiants() {
        setActiveButton(btnEtudiants);
        if (pageTitle != null) pageTitle.setText("Mes étudiants");
        showOnly(dashboardView);
    }

    @FXML
    public void showProfil() {
        setActiveButton(btnProfil);
        navigate("/tn/esprit/interfaces/Profil.fxml", "Mon profil");
    }

    @FXML
    public void addLecon() {
        navigate("/tn/esprit/interfaces/LeconForm.fxml", "Nouvelle leçon");
    }

    @FXML
    public void switchToVueEtudiant() {
        try {
            DashboardEtudiantController.setCurrentUser(currentUser);
            DashboardEtudiantController.setSourceDashboardType("prof");
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout() {
        currentUser = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(String fxml, String title) {
        try {
            if (fxml.endsWith("Cours.fxml")) {
                CoursController.setCurrentUser(currentUser);
            } else if (fxml.endsWith("Profil.fxml")) {
                ProfilController.setCurrentUser(currentUser);
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
                if (pageTitle != null) pageTitle.setText(title);
            } else {
                btnDashboard.getScene().setRoot(view);
            }
        } catch (Exception e) {
            System.out.println("Navigation " + fxml + " : " + e.getMessage());
        }
    }

    private void showOnly(javafx.scene.Node node) {
        if (contentArea != null) contentArea.getChildren().setAll(node);
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnMatieres, btnLecons, btnTests, btnEtudiants, btnProfil};
        for (Button button : all) {
            if (button == null) continue;
            if (button == active) {
                button.getStyleClass().remove("nav-btn");
                if (!button.getStyleClass().contains("nav-btn-active"))
                    button.getStyleClass().add("nav-btn-active");
            } else {
                button.getStyleClass().remove("nav-btn-active");
                if (!button.getStyleClass().contains("nav-btn"))
                    button.getStyleClass().add("nav-btn");
            }
        }
    }
}
