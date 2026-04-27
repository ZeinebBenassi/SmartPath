package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entity.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DashboardAdminController {

    // ── Labels / zones principales ───────────────────────────────────────
    @FXML private Label     adminNameLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     dateLabel;
    @FXML private StackPane contentArea;
    @FXML private VBox      dashboardView;

    // ── Cartes du dashboard ──────────────────────────────────────────────
    @FXML private Label totalUsers;
    @FXML private Label totalEtudiants;
    @FXML private Label totalProfs;
    @FXML private Label totalOffres;

    // ── Boutons sidebar ──────────────────────────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private VBox   usersSubMenu;
    @FXML private Button btnGestionProfs;
    @FXML private Button btnGestionEtudiants;
    @FXML private Button btnFilieres;
    @FXML private Button btnOffres;

    // ── Statistiques (bouton toggle + sous-menu) ─────────────────────────
    @FXML private Button btnStatistiques;
    @FXML private VBox   statsSubMenu;
    @FXML private Button btnStatistiquesUsers;
    @FXML private Button btnStatistiquesQuiz;

    // ── Quiz ─────────────────────────────────────────────────────────────
    @FXML private Button btnQuizAdmin;
    @FXML private Button btnQuizHistorique;

    // ── Notifications ─────────────────────────────────────────────────────
    @FXML private Button btnNotifications;  // cloche dans la topbar
    @FXML private Label  lblNotifBadge;     // badge rouge compteur

    // ── Autres ───────────────────────────────────────────────────────────
    @FXML private Button btnProfil;
    @FXML private Button btnVueEtudiant;

    // ── TableView (caché, compatibilité FXML) ────────────────────────────
    @FXML private TableView<?>      usersTable;
    @FXML private TableColumn<?,?> colNom, colPrenom, colEmail, colType, colStatus, colActions;

    // ── État ─────────────────────────────────────────────────────────────
    private static User currentUser;
    private final UserService        userService  = new UserService();
    private final NotificationService notifService = new NotificationService();
    private boolean usersMenuOpen = false;
    private boolean statsMenuOpen = false;

    public static void setCurrentUser(User user) { currentUser = user; }

    // ─────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        LocalDate today   = LocalDate.now();
        String dayName    = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        String formatted  = dayName.substring(0,1).toUpperCase() + dayName.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        if (dateLabel   != null) dateLabel.setText(formatted);
        if (currentUser != null && adminNameLabel != null)
            adminNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        loadStats();
        refreshNotifBadge();
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
        if (pageTitle != null) pageTitle.setText("Dashboard");
    }

    private void loadStats() {
        try {
            if (totalUsers     != null) totalUsers.setText(String.valueOf(userService.countAll()));
            if (totalEtudiants != null) totalEtudiants.setText(String.valueOf(userService.countByType("etudiant")));
            if (totalProfs     != null) totalProfs.setText(String.valueOf(userService.countByType("prof")));
            if (totalOffres    != null) totalOffres.setText("0");
        } catch (Exception e) { System.out.println("Stats dashboard : " + e.getMessage()); }
    }

    // ── Navigation principale ─────────────────────────────────────────────

    @FXML public void showDashboard() {
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
        if (pageTitle != null) pageTitle.setText("Dashboard");
        closeUsersMenu();
        closeStatsMenu();
    }

    // ── Sous-menu Utilisateurs ─────────────────────────────────────────────

    @FXML public void toggleUsersMenu() {
        if (usersSubMenu == null) return;
        usersMenuOpen = !usersMenuOpen;
        usersSubMenu.setVisible(usersMenuOpen);
        usersSubMenu.setManaged(usersMenuOpen);
        styleToggle(btnUsers, usersMenuOpen);
        if (usersMenuOpen) closeStatsMenu();
    }

    @FXML public void showProfsFromMenu()     { navigate("/tn/esprit/interfaces/GestionProfs.fxml",    "Gestion des professeurs"); }
    @FXML public void showEtudiantsFromMenu() { navigate("/tn/esprit/interfaces/GestionEtudiants.fxml","Gestion des étudiants"); }
    @FXML public void showEtudiants()         { navigate("/tn/esprit/interfaces/GestionEtudiants.fxml","Gestion des étudiants"); }
    @FXML public void showProfs()             { navigate("/tn/esprit/interfaces/GestionProfs.fxml",    "Gestion des professeurs"); }

    @FXML public void showFilieres() {
        setActiveButton(btnFilieres);
        navigate("/tn/esprit/interfaces/FiliereContent.fxml", "Gestion des filières");
    }

    @FXML public void showOffres() {
        setActiveButton(btnOffres);
        navigate("/tn/esprit/interfaces/GestionOffres.fxml", "Offres de stage");
    }

    // ── Sous-menu Statistiques ─────────────────────────────────────────────

    @FXML public void toggleStatsMenu() {
        if (statsSubMenu == null) return;
        statsMenuOpen = !statsMenuOpen;
        statsSubMenu.setVisible(statsMenuOpen);
        statsSubMenu.setManaged(statsMenuOpen);
        styleToggle(btnStatistiques, statsMenuOpen);
        if (statsMenuOpen) closeUsersMenu();
    }

    @FXML public void showStatistiquesUsers() {
        setActiveButton(btnStatistiques);
        navigate("/tn/esprit/interfaces/Statistiques.fxml", "📈 Statistiques Utilisateurs");
    }

    @FXML public void showStatistiquesQuiz() {
        setActiveButton(btnStatistiques);
        navigate("/tn/esprit/interfaces/QuizStatistiques.fxml", "📊 Statistiques Quiz");
    }

    // ── Quiz ──────────────────────────────────────────────────────────────

    @FXML public void showQuizAdmin() {
        setActiveButton(btnQuizAdmin);
        navigate("/tn/esprit/interfaces/QuestionContent.fxml", "Quiz - Gestion des questions");
    }

    @FXML public void showQuizHistorique() {
        setActiveButton(btnQuizHistorique);
        navigate("/tn/esprit/interfaces/QuizHistorique.fxml", "📋 Historique Quiz");
    }

    // ── Notifications ──────────────────────────────────────────────────────

    /** Ouvre le panneau de notifications dans le contentArea. */
    @FXML public void showNotifications() {
        if (pageTitle != null) pageTitle.setText("🔔 Notifications");
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/interfaces/Notifications.fxml"));
            Parent view = loader.load();
            NotificationsController ctrl = loader.getController();
            // Mise à jour du badge en temps réel quand l'utilisateur marque comme lu
            ctrl.setOnUnreadCountChanged(count -> {
                if (lblNotifBadge != null) {
                    lblNotifBadge.setText(count > 0 ? String.valueOf(count) : "");
                    lblNotifBadge.setVisible(count > 0);
                    lblNotifBadge.setManaged(count > 0);
                }
            });
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.out.println("Erreur showNotifications : " + e.getMessage());
        }
        // Après ouverture, rafraîchir le badge (peut avoir changé)
        refreshNotifBadge();
    }

    /** Rafraîchit le badge rouge de la cloche avec le nombre de notifications non lues. */
    public void refreshNotifBadge() {
        if (lblNotifBadge == null) return;
        int unread = notifService.countUnread();
        lblNotifBadge.setText(unread > 0 ? String.valueOf(unread) : "");
        lblNotifBadge.setVisible(unread > 0);
        lblNotifBadge.setManaged(unread > 0);
    }

    // ── Profil / logout ────────────────────────────────────────────────────

    @FXML public void showProfil() {
        setActiveButton(btnProfil);
        navigate("/tn/esprit/interfaces/Profil.fxml", "Mon profil");
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

    // ── Utilitaires privés ─────────────────────────────────────────────────

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
        } catch (Exception e) {
            System.out.println("Navigation " + fxml + " : " + e.getMessage());
        }
    }

    private void showOnly(javafx.scene.Node node) {
        if (contentArea != null) contentArea.getChildren().setAll(node);
    }

    private void closeUsersMenu() {
        if (usersSubMenu == null) return;
        usersMenuOpen = false;
        usersSubMenu.setVisible(false);
        usersSubMenu.setManaged(false);
        styleToggle(btnUsers, false);
    }

    private void closeStatsMenu() {
        if (statsSubMenu == null) return;
        statsMenuOpen = false;
        statsSubMenu.setVisible(false);
        statsSubMenu.setManaged(false);
        styleToggle(btnStatistiques, false);
    }

    private void styleToggle(Button btn, boolean active) {
        if (btn == null) return;
        if (active) {
            btn.getStyleClass().remove("nav-btn");
            if (!btn.getStyleClass().contains("nav-btn-active")) btn.getStyleClass().add("nav-btn-active");
        } else {
            btn.getStyleClass().remove("nav-btn-active");
            if (!btn.getStyleClass().contains("nav-btn")) btn.getStyleClass().add("nav-btn");
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnUsers, btnFilieres, btnOffres,
                        btnQuizAdmin, btnQuizHistorique, btnStatistiques, btnProfil};
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
