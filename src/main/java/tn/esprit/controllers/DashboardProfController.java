package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    @FXML private Button btnCours;
    @FXML private Button btnQuiz;
    @FXML private Button btnLecons;
    @FXML private Button btnTests;
    @FXML private Button btnEtudiants;
    @FXML private Button btnProfil;
    @FXML private Button btnVueEtudiant;

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    @FXML
    public void initialize() {
        if (currentUser != null) {
            if (profNameLabel != null) profNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            if (profSpecLabel != null) profSpecLabel.setText("Professeur");
        }
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
        if (pageTitle != null) pageTitle.setText("Tableau de bord");
    }

    @FXML public void showDashboard() { 
        setActiveButton(btnDashboard); 
        if (pageTitle != null) pageTitle.setText("Tableau de bord"); 
        showOnly(dashboardView); 
    }

    @FXML public void showMatieres() { 
        setActiveButton(btnMatieres);
        tn.esprit.utils.feature_cours_et_quiz.AppSession.setCurrentUser(currentUser);
        tn.esprit.controllers.feature_cours_et_quiz.AppShellController.setInitialTab("matieres");
        navigate("/tn/esprit/feature_cours_et_quiz/app-shell.fxml", "Mes Matières"); 
    }

    @FXML public void showCours() {
        setActiveButton(btnCours);
        tn.esprit.utils.feature_cours_et_quiz.AppSession.setCurrentUser(currentUser);
        tn.esprit.controllers.feature_cours_et_quiz.AppShellController.setInitialTab("matieres");
        navigate("/tn/esprit/feature_cours_et_quiz/app-shell.fxml", "Mes Cours");
    }

    @FXML public void showQuiz() {
        setActiveButton(btnQuiz);
        tn.esprit.utils.feature_cours_et_quiz.AppSession.setCurrentUser(currentUser);
        tn.esprit.controllers.feature_cours_et_quiz.AppShellController.setInitialTab("quiz");
        navigate("/tn/esprit/feature_cours_et_quiz/app-shell.fxml", "Quiz");
    }

    @FXML public void showLecons() {
        showMatieres();
    }

    @FXML public void addLecon() {
        showMatieres();
    }

    @FXML public void showTests() { 
        setActiveButton(btnTests);
        tn.esprit.utils.feature_cours_et_quiz.AppSession.setCurrentUser(currentUser);
        tn.esprit.controllers.feature_cours_et_quiz.AppShellController.setInitialTab("quiz");
        navigate("/tn/esprit/feature_cours_et_quiz/app-shell.fxml", "Mes Quiz"); 
    }

    @FXML public void showEtudiants() {
        setActiveButton(btnEtudiants);
        if (pageTitle != null) pageTitle.setText("Mes étudiants");
        showOnly(dashboardView);
    }

    @FXML public void showProfil() { 
        setActiveButton(btnProfil); 
        navigate("/tn/esprit/interfaces/Profil.fxml", "Mon profil"); 
    }

    @FXML public void switchToVueEtudiant() {
        try {
            DashboardEtudiantController.setCurrentUser(currentUser);
            DashboardEtudiantController.setSourceDashboardType("prof");
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleLogout() {
        currentUser = null;
        try { Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml")); btnDashboard.getScene().setRoot(root); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(String fxml, String title) {
        try {
            tn.esprit.utils.feature_cours_et_quiz.AppSession.setCurrentUser(currentUser);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            if (contentArea != null) { 
                contentArea.getChildren().setAll(view); 
                if (pageTitle != null) pageTitle.setText(title); 
            }
            else btnDashboard.getScene().setRoot(view);
        } catch (Exception e) { 
            System.err.println("Navigation error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    private void showOnly(javafx.scene.Node node) { if (contentArea != null) contentArea.getChildren().setAll(node); }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnMatieres, btnCours, btnQuiz, btnLecons, btnTests, btnEtudiants, btnProfil};
        for (Button button : all) {
            if (button == null) continue;
            if (button == active) { 
                button.getStyleClass().remove("nav-btn"); 
                if (!button.getStyleClass().contains("nav-btn-active")) button.getStyleClass().add("nav-btn-active"); 
            } else { 
                button.getStyleClass().remove("nav-btn-active"); 
                if (!button.getStyleClass().contains("nav-btn")) button.getStyleClass().add("nav-btn"); 
            }
        }
    }
}
