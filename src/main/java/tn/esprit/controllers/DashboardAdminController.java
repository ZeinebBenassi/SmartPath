package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.User;
import services.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DashboardAdminController {

    // ── Labels sidebar ──
    @FXML private Label adminNameLabel;

    // ── Header ──
    @FXML private Label pageTitle;
    @FXML private Label dateLabel;

    // ── Content area ──
    @FXML private StackPane contentArea;
    @FXML private VBox dashboardView;

    // ── Stat cards ──
    @FXML private Label totalUsers;
    @FXML private Label totalEtudiants;
    @FXML private Label totalProfs;
    @FXML private Label totalOffres;

    // ── Nav buttons ──
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnFilieres;
    @FXML private Button btnOffres;
    @FXML private Button btnStats;
    @FXML private Button btnVueEtudiant;

    // ── Table ──
    @FXML private TableView<?> usersTable;
    @FXML private TableColumn<?, ?> colNom;
    @FXML private TableColumn<?, ?> colPrenom;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colType;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colActions;

    private static User currentUser;
    private UserService userService = new UserService();

    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        // Date
        LocalDate today = LocalDate.now();
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        String formatted = dayName.substring(0, 1).toUpperCase() + dayName.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        if (dateLabel != null) dateLabel.setText(formatted);

        // Nom admin
        if (currentUser != null && adminNameLabel != null) {
            adminNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        }

        // Stats
        loadStats();

        // Active nav
        setActiveButton(btnDashboard);
    }

    private void loadStats() {
        try {
            if (totalUsers    != null) totalUsers.setText(String.valueOf(userService.countAll()));
            if (totalEtudiants!= null) totalEtudiants.setText(String.valueOf(userService.countByType("etudiant")));
            if (totalProfs    != null) totalProfs.setText(String.valueOf(userService.countByType("prof")));
            if (totalOffres   != null) totalOffres.setText("0"); // à brancher
        } catch (Exception e) {
            System.out.println("Stats non disponibles : " + e.getMessage());
        }
    }

    // ── Navigation interne ──────────────────────────────────────────

    @FXML public void showDashboard() {
        pageTitle.setText("Dashboard");
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
    }

    @FXML public void showUsers() {
        setActiveButton(btnUsers);
        navigate("/views/GestionUsers.fxml", "Gestion des utilisateurs");
    }

    @FXML public void showFilieres() {
        setActiveButton(btnFilieres);
        navigate("/views/GestionFilieres.fxml", "Gestion des filières");
    }

    @FXML public void showOffres() {
        setActiveButton(btnOffres);
        navigate("/views/GestionOffres.fxml", "Offres de stage");
    }

    @FXML public void showStats() {
        setActiveButton(btnStats);
        pageTitle.setText("Statistiques");
        // à implémenter
    }

    @FXML public void showQuizAdmin() {
        navigate("/views/QuizAdmin.fxml", "Gestion des quiz");
    }

    // ── Vue Étudiant (bouton "Voir comme étudiant") ─────────────────

    @FXML public void switchToVueEtudiant() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/DashboardEtudiant.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible de charger la vue étudiant.");
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
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
                pageTitle.setText(title);
            } else {
                btnDashboard.getScene().setRoot(view);
            }
        } catch (Exception e) {
            System.out.println("Navigation vers " + fxml + " : " + e.getMessage());
        }
    }

    private void showOnly(javafx.scene.Node node) {
        if (contentArea != null) {
            contentArea.getChildren().setAll(node);
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnUsers, btnFilieres, btnOffres, btnStats};
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #aaaacc; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-font-family: 'Segoe UI';";
        String activeStyle = "-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 13; -fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-font-family: 'Segoe UI';";
        for (Button b : all) {
            if (b != null) b.setStyle(b == active ? activeStyle : inactive);
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }
}

