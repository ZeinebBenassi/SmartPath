package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DashboardAdminController {

    @FXML private Label      adminNameLabel;
    @FXML private Label      pageTitle;
    @FXML private Label      dateLabel;
    @FXML private StackPane  contentArea;
    @FXML private VBox       dashboardView;
    @FXML private Label      totalUsers;
    @FXML private Label      totalEtudiants;
    @FXML private Label      totalProfs;
    @FXML private Label      totalOffres;
    @FXML private Button     btnDashboard;
    @FXML private Button     btnUsers;
    @FXML private VBox       usersSubMenu;
    @FXML private Button     btnGestionProfs;
    @FXML private Button     btnGestionEtudiants;
    @FXML private Button     btnFilieres;
    @FXML private Button     btnOffres;
    @FXML private Button     btnQuizAdmin;
    @FXML private Button     btnProfil;
    @FXML private Button     btnVueEtudiant;
    @FXML private TableView<?>          usersTable;
    @FXML private TableColumn<?, ?>     colNom;
    @FXML private TableColumn<?, ?>     colPrenom;
    @FXML private TableColumn<?, ?>     colEmail;
    @FXML private TableColumn<?, ?>     colType;
    @FXML private TableColumn<?, ?>     colStatus;
    @FXML private TableColumn<?, ?>     colActions;

    private static User currentUser;
    private final UserService userService = new UserService();
    private boolean usersMenuOpen = false;

    public static void setCurrentUser(User user) { currentUser = user; }

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        String formatted = dayName.substring(0,1).toUpperCase() + dayName.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        if (dateLabel != null) dateLabel.setText(formatted);
        if (currentUser != null && adminNameLabel != null)
            adminNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        loadStats();
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
        if (pageTitle != null) pageTitle.setText("Dashboard");
    }

    private void loadStats() {
        try {
            if (totalUsers    != null) totalUsers.setText(String.valueOf(userService.countAll()));
            if (totalEtudiants!= null) totalEtudiants.setText(String.valueOf(userService.countByType("etudiant")));
            if (totalProfs    != null) totalProfs.setText(String.valueOf(userService.countByType("prof")));
            if (totalOffres   != null) totalOffres.setText("0");
        } catch (Exception e) { System.out.println("Stats : " + e.getMessage()); }
    }

    @FXML public void showDashboard() {
        if (pageTitle != null) pageTitle.setText("Dashboard");
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
        // Fermer le sous-menu si ouvert
        if (usersSubMenu != null) {
            usersSubMenu.setVisible(false);
            usersSubMenu.setManaged(false);
        }
        usersMenuOpen = false;
    }

    /**
     * Toggle le sous-menu Gestion utilisateurs (Profs / Étudiants).
     * Ne navigue plus directement vers GestionUsers.
     */
    @FXML public void toggleUsersMenu() {
        if (usersSubMenu == null) return;
        usersMenuOpen = !usersMenuOpen;
        usersSubMenu.setVisible(usersMenuOpen);
        usersSubMenu.setManaged(usersMenuOpen);

        if (usersMenuOpen) {
            btnUsers.getStyleClass().remove("nav-btn");
            if (!btnUsers.getStyleClass().contains("nav-btn-active"))
                btnUsers.getStyleClass().add("nav-btn-active");
        } else {
            btnUsers.getStyleClass().remove("nav-btn-active");
            if (!btnUsers.getStyleClass().contains("nav-btn"))
                btnUsers.getStyleClass().add("nav-btn");
        }
    }

    /** Appelé par le sous-bouton "Gestion profs" */
    @FXML public void showProfsFromMenu() {
        setActiveButton(btnUsers);
        navigate("/tn/esprit/interfaces/GestionProfs.fxml", "Gestion des professeurs");
    }

    /** Appelé par le sous-bouton "Gestion étudiants" */
    @FXML public void showEtudiantsFromMenu() {
        setActiveButton(btnUsers);
        navigate("/tn/esprit/interfaces/GestionEtudiants.fxml", "Gestion des étudiants");
    }

    @FXML public void showEtudiants() {
        navigate("/tn/esprit/interfaces/GestionEtudiants.fxml", "Gestion des étudiants");
    }

    @FXML public void showProfs() {
        navigate("/tn/esprit/interfaces/GestionProfs.fxml", "Gestion des professeurs");
    }

    @FXML public void showFilieres() {
        setActiveButton(btnFilieres);
        navigate("/tn/esprit/interfaces/FiliereContent.fxml", "Gestion des filières");
    }

    @FXML public void showOffres() {
        setActiveButton(btnOffres);
        navigate("/tn/esprit/interfaces/GestionOffres.fxml", "Offres de stage");
    }

    @FXML public void showProfil() {
        setActiveButton(btnProfil);
        navigate("/tn/esprit/interfaces/Profil.fxml", "Mon profil");
    }

    @FXML public void showQuizAdmin() {
        setActiveButton(btnQuizAdmin);
        navigate("/tn/esprit/interfaces/QuestionContent.fxml", "Quiz - Gestion des questions");
    }

    @FXML public void switchToVueEtudiant() {
        try {
            DashboardEtudiantController.setCurrentUser(currentUser);
            DashboardEtudiantController.setSourceDashboardType("admin");
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleLogout() {
        currentUser = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(String fxml, String title) {
        try {
            if (fxml.endsWith("Profil.fxml")) ProfilController.setCurrentUser(currentUser);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
                if (pageTitle != null) pageTitle.setText(title);
            } else {
                btnDashboard.getScene().setRoot(view);
            }
        } catch (Exception e) { System.out.println("Navigation " + fxml + " : " + e.getMessage()); }
    }

    private void showOnly(javafx.scene.Node node) {
        if (contentArea != null) contentArea.getChildren().setAll(node);
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnUsers, btnFilieres, btnOffres, btnQuizAdmin, btnProfil};
        for (Button b : all) {
            if (b == null) continue;
            if (b == active) {
                b.getStyleClass().remove("nav-btn");
                if (!b.getStyleClass().contains("nav-btn-active")) b.getStyleClass().add("nav-btn-active");
            } else {
                b.getStyleClass().remove("nav-btn-active");
                if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
            }
        }
    }
}
