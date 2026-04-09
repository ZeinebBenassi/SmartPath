package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entity.User;
import services.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DashboardAdminController {

    @FXML private Label adminNameLabel;
    @FXML private Label pageTitle;
    @FXML private Label dateLabel;

    @FXML private StackPane contentArea;
    @FXML private VBox dashboardView;

    @FXML private Label totalUsers;
    @FXML private Label totalEtudiants;
    @FXML private Label totalProfs;
    @FXML private Label totalOffres;

    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnFilieres;
    @FXML private Button btnOffres;
    @FXML private Button btnQuizAdmin;
    @FXML private Button btnProfil;
    @FXML private Button btnVueEtudiant;

    @FXML private TableView<?> usersTable;
    @FXML private TableColumn<?, ?> colNom;
    @FXML private TableColumn<?, ?> colPrenom;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colType;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colActions;

    private static User currentUser;
    private final UserService userService = new UserService();

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        String formatted = dayName.substring(0, 1).toUpperCase() + dayName.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        if (dateLabel != null) dateLabel.setText(formatted);

        if (currentUser != null && adminNameLabel != null) {
            adminNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        }

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
        } catch (Exception e) {
            System.out.println("Stats non disponibles : " + e.getMessage());
        }
    }

    @FXML
    public void showDashboard() {
        if (pageTitle != null) pageTitle.setText("Dashboard");
        setActiveButton(btnDashboard);
        showOnly(dashboardView);
    }

    @FXML
    public void showUsers() {
        setActiveButton(btnUsers);
        navigate("/tn/esprit/interfaces/GestionUsers.fxml", "Gestion des utilisateurs");
    }

    @FXML
    public void showFilieres() {
        setActiveButton(btnFilieres);
        navigate("/tn/esprit/interfaces/GestionFilieres.fxml", "Gestion des filières");
    }

    @FXML
    public void showOffres() {
        setActiveButton(btnOffres);
        navigate("/tn/esprit/interfaces/GestionOffres.fxml", "Offres de stage");
    }

    @FXML
    public void showProfil() {
        setActiveButton(btnProfil);
        navigate("/tn/esprit/interfaces/Profil.fxml", "Mon profil");
    }

    @FXML
    public void showQuizAdmin() {
        setActiveButton(btnQuizAdmin);
        navigate("/tn/esprit/interfaces/QuizAdmin.fxml", "Gestion des quiz");
    }

    @FXML
    public void switchToVueEtudiant() {
        try {
            DashboardEtudiantController.setCurrentUser(currentUser);
            DashboardEtudiantController.setSourceDashboardType("admin");
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            btnDashboard.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible de charger la vue étudiant.");
        }
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
            if (fxml.endsWith("Profil.fxml")) {
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
            System.out.println("Navigation vers " + fxml + " : " + e.getMessage());
        }
    }

    private void showOnly(javafx.scene.Node node) {
        if (contentArea != null) contentArea.getChildren().setAll(node);
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnDashboard, btnUsers, btnFilieres, btnOffres, btnQuizAdmin, btnProfil};
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

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
