package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;

public class DashboardEtudiantController {

    @FXML private Label welcomeLabel;
    @FXML private Label etudiantNameLabel;
    @FXML private Label etudiantNiveauLabel;

    @FXML private Label moyenneLabel;
    @FXML private Label nbCoursLabel;
    @FXML private Label nbOffresLabel;
    @FXML private Label quizStatusLabel;

    @FXML private Button btnAccueil;
    @FXML private Button btnCours;
    @FXML private Button btnQuiz;
    @FXML private Button btnSmartPath;
    @FXML private Button btnStages;
    @FXML private Button btnReleve;
    @FXML private Button btnProfil;

    @FXML private ListView<?> coursList;
    @FXML private ListView<?> offresList;

    private static User currentUser;

    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser != null) {
            String prenom = currentUser.getPrenom() != null ? currentUser.getPrenom() : "";
            if (welcomeLabel != null)
                welcomeLabel.setText("Bonjour, " + prenom + " !");
            if (etudiantNameLabel != null)
                etudiantNameLabel.setText(prenom + " " + currentUser.getNom());
        }

        if (moyenneLabel   != null) moyenneLabel.setText("--");
        if (nbCoursLabel   != null) nbCoursLabel.setText("0");
        if (nbOffresLabel  != null) nbOffresLabel.setText("0");
        if (quizStatusLabel!= null) quizStatusLabel.setText("Non fait");

        setActiveButton(btnAccueil);
    }

    @FXML public void showAccueil() {
        setActiveButton(btnAccueil);
    }

    @FXML public void showCours() {
        setActiveButton(btnCours);
        navigate("/views/Cours.fxml");
    }

    @FXML public void showQuiz() {
        setActiveButton(btnQuiz);
        navigate("/views/Quiz.fxml");
    }

    @FXML public void showSmartPath() {
        setActiveButton(btnSmartPath);
    }

    @FXML public void showStages() {
        setActiveButton(btnStages);
        navigate("/views/Stages.fxml");
    }

    @FXML public void showReleve() {
        setActiveButton(btnReleve);
        navigate("/views/Releve.fxml");
    }

    @FXML public void showProfil() {
        setActiveButton(btnProfil);
        navigate("/views/Profil.fxml");
    }

    @FXML public void handleLogout() {
        currentUser = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            btnAccueil.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            btnAccueil.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println("Navigation " + fxml + " : " + e.getMessage());
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnAccueil, btnCours, btnQuiz, btnSmartPath, btnStages, btnReleve, btnProfil};
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #aaaacc; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
        String activeStyle = "-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
        for (Button b : all) {
            if (b != null) b.setStyle(b == active ? activeStyle : inactive);
        }
    }
}

