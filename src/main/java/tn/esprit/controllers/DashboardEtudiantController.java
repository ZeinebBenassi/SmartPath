package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entity.User;

public class DashboardEtudiantController {

    @FXML private Label welcomeLabel;
    @FXML private Label etudiantNameLabel;
    @FXML private Label etudiantNiveauLabel;
    @FXML private Label moyenneLabel;
    @FXML private Label nbCoursLabel;
    @FXML private Label nbOffresLabel;
    @FXML private Label quizStatusLabel;
    @FXML private Button btnAccueil;
    @FXML private Button btnAboutContact;
    @FXML private Button btnCours;
    @FXML private Button btnQuiz;
    @FXML private Button btnStages;
    @FXML private Button btnRetourDashboard;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane homeView;
    @FXML private ScrollPane aboutContactView;
    @FXML private ListView<?> coursList;
    @FXML private ListView<?> offresList;

    private static User currentUser;
    private static String sourceDashboardType;

    public static void setCurrentUser(User u) { currentUser = u; }
    public static void setSourceDashboardType(String role) { sourceDashboardType = role; }

    @FXML
    public void initialize() {
        if (currentUser != null) {
            String prenom = currentUser.getPrenom() != null ? currentUser.getPrenom() : "";
            if (welcomeLabel      != null) welcomeLabel.setText("Bonjour, " + prenom + " !");
            if (etudiantNameLabel != null) etudiantNameLabel.setText(prenom + " " + currentUser.getNom());
        }
        if (moyenneLabel    != null) moyenneLabel.setText("--");
        if (nbCoursLabel    != null) nbCoursLabel.setText("0");
        if (nbOffresLabel   != null) nbOffresLabel.setText("0");
        if (quizStatusLabel != null) quizStatusLabel.setText("Non fait");
        showHomeView();
        setActiveButton(btnAccueil);
        if (btnRetourDashboard != null) {
            boolean canReturn = sourceDashboardType != null && !sourceDashboardType.isBlank() && !"etudiant".equals(sourceDashboardType);
            btnRetourDashboard.setVisible(canReturn);
            btnRetourDashboard.setManaged(canReturn);
        }
    }

    @FXML public void showAccueil()      { setActiveButton(btnAccueil); showHomeView(); }
    @FXML public void showAboutContact() { setActiveButton(btnAboutContact); showAboutContactView(); }
    @FXML public void showCours()        { setActiveButton(btnCours); navigate("/tn/esprit/interfaces/Cours.fxml"); }
    @FXML public void showStages()       { setActiveButton(btnStages); navigate("/tn/esprit/interfaces/Stages.fxml"); }

    @FXML public void showQuiz() {
        setActiveButton(btnQuiz);
        try { QuizPlayerController.setCurrentUser(currentUser); navigate("/tn/esprit/interfaces/QuizPlayer.fxml"); }
        catch (Exception e) { navigate("/tn/esprit/interfaces/Quiz.fxml"); }
    }

    @FXML public void returnToSourceDashboard() {
        String source = sourceDashboardType;
        if (source == null || source.isBlank() || "etudiant".equals(source)) return;
        try {
            String fxml;
            if ("admin".equals(source)) { DashboardAdminController.setCurrentUser(currentUser); fxml = "/tn/esprit/interfaces/DashboardAdmin.fxml"; }
            else                        { DashboardProfController.setCurrentUser(currentUser);  fxml = "/tn/esprit/interfaces/DashboardProf.fxml"; }
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            btnAccueil.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleLogout() {
        currentUser = null; sourceDashboardType = null;
        try { Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml")); btnAccueil.getScene().setRoot(root); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(String fxml) {
        try {
            if (fxml.endsWith("Cours.fxml"))   CoursController.setCurrentUser(currentUser);
            if (fxml.endsWith("Stages.fxml"))  StagesController.setCurrentUser(currentUser);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (contentArea != null) contentArea.getChildren().setAll(root);
            else btnAccueil.getScene().setRoot(root);
        } catch (Exception e) { System.out.println("Navigation " + fxml + " : " + e.getMessage()); e.printStackTrace(); }
    }

    private void showHomeView() {
        if (contentArea == null || homeView == null) return;
        contentArea.getChildren().setAll(homeView);
        homeView.setManaged(true); homeView.setVisible(true);
        if (aboutContactView != null) { aboutContactView.setManaged(false); aboutContactView.setVisible(false); }
    }

    private void showAboutContactView() {
        if (contentArea == null || aboutContactView == null) return;
        contentArea.getChildren().setAll(aboutContactView);
        aboutContactView.setManaged(true); aboutContactView.setVisible(true);
        if (homeView != null) { homeView.setManaged(false); homeView.setVisible(false); }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnAccueil, btnAboutContact, btnCours, btnQuiz, btnStages};
        for (Button b : all) {
            if (b == null) continue;
            if (b == active) b.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-border-color: #bfdbfe; -fx-border-radius: 8;");
            else             b.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a6a8a; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
        }
    }
}
