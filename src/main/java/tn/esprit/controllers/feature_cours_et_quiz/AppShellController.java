package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.entity.User;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import tn.esprit.utils.feature_cours_et_quiz.ViewNavigator;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class AppShellController {
    @FXML private StackPane contentHost;
    @FXML private Label userLabel;
    @FXML private Label roleBadge;
    @FXML private Button homeBtn;
    @FXML private Button matieresBtn;
    @FXML private Button quizBtn;
    @FXML private Button logoutBtn;

    private Role currentRole = Role.ETUDIANT;
    private static String initialTab = "home";

    public static void setInitialTab(String tab) {
        initialTab = tab;
    }

    @FXML
    public void initialize() {
        onAfterLogin();
        if (currentRole == Role.ADMIN) {
            showHome();
            initialTab = "home";
            return;
        }
        if ("matieres".equals(initialTab)) {
            showMatieres();
        } else if ("quiz".equals(initialTab)) {
            showQuiz();
        } else {
            showHome();
        }
        initialTab = "home"; // Reset for next time
    }

    public void onAfterLogin() {
        User user = AppSession.getCurrentUser();
        currentRole = RoleUtils.normalize(user == null ? null : user.getType());

        if (userLabel != null) {
            userLabel.setText(user == null ? "" : (user.getPrenom() + " " + user.getNom()));
        }
        if (roleBadge != null) {
            roleBadge.getStyleClass().removeAll("role-admin", "role-prof", "role-etudiant");
            roleBadge.setText(RoleUtils.display(currentRole));
            switch (currentRole) {
                case ADMIN -> roleBadge.getStyleClass().add("role-admin");
                case PROF -> roleBadge.getStyleClass().add("role-prof");
                case ETUDIANT -> roleBadge.getStyleClass().add("role-etudiant");
            }
        }

        boolean adminBlocked = currentRole == Role.ADMIN;
        if (matieresBtn != null) matieresBtn.setDisable(adminBlocked);
        if (quizBtn != null) quizBtn.setDisable(adminBlocked);
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    @FXML
    public void showHome() {
        setActiveNav("home");
        setContent("/tn/esprit/feature_cours_et_quiz/home.fxml");
    }

    @FXML
    public void showMatieres() {
        if (currentRole == Role.ADMIN) {
            new Alert(Alert.AlertType.INFORMATION, "Accès refusé: module réservé aux profs et étudiants.").showAndWait();
            showHome();
            return;
        }
        setActiveNav("matieres");
        setContent("/tn/esprit/feature_cours_et_quiz/matiere-cards.fxml");
    }

    @FXML
    public void showQuiz() {
        if (currentRole == Role.ADMIN) {
            new Alert(Alert.AlertType.INFORMATION, "Accès refusé: module réservé aux profs et étudiants.").showAndWait();
            showHome();
            return;
        }
        setActiveNav("quiz");
        setContent("/tn/esprit/feature_cours_et_quiz/quiz-cards.fxml");
    }

    @FXML
    public void handleLogout() {
        AppSession.clear();
        try {
            Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            contentHost.getScene().setRoot(root);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void showMatiereForm(Matiere existing) {
        try {
            setActiveNav("matieres");
            ViewNavigator.LoadedView view = ViewNavigator.load("/tn/esprit/feature_cours_et_quiz/matiere-form.fxml");
            if (view.controller() instanceof MatiereFormController controller) {
                controller.setAppShell(this);
                controller.setExisting(existing);
            }
            setContent(view.root());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void showQuizForm(Quiz existing) {
        try {
            setActiveNav("quiz");
            ViewNavigator.LoadedView view = ViewNavigator.load("/tn/esprit/feature_cours_et_quiz/quiz-form.fxml");
            if (view.controller() instanceof QuizFormController controller) {
                controller.setAppShell(this);
                controller.setExisting(existing);
            }
            setContent(view.root());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void showQuizFormForMatiere(Matiere matiere) {
        try {
            setActiveNav("matieres");
            ViewNavigator.LoadedView view = ViewNavigator.load("/tn/esprit/feature_cours_et_quiz/quiz-form.fxml");
            if (view.controller() instanceof QuizFormController controller) {
                controller.setAppShell(this);
                controller.setCreateContext(matiere);
                controller.setExisting(null);
            }
            setContent(view.root());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void showMatiereCours(Matiere matiere) {
        try {
            setActiveNav("matieres");
            ViewNavigator.LoadedView view = ViewNavigator.load("/tn/esprit/feature_cours_et_quiz/matiere-cours.fxml");
            if (view.controller() instanceof MatiereCoursController controller) {
                controller.setAppShell(this);
                controller.setMatiere(matiere);
            }
            setContent(view.root());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void showQuizPass(Quiz quiz) {
        try {
            setActiveNav("quiz");
            ViewNavigator.LoadedView view = ViewNavigator.load("/tn/esprit/feature_cours_et_quiz/quiz-pass.fxml");
            if (view.controller() instanceof QuizPassController controller) {
                controller.setAppShell(this);
                controller.setQuiz(quiz);
            }
            setContent(view.root());
            // Hide logout button while a quiz is being taken to prevent accidental disconnects
            if (logoutBtn != null) { logoutBtn.setVisible(false); logoutBtn.setManaged(false); }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void setActiveNav(String tab) {
        if (homeBtn != null) homeBtn.getStyleClass().remove("nav-btn-active");
        if (matieresBtn != null) matieresBtn.getStyleClass().remove("nav-btn-active");
        if (quizBtn != null) quizBtn.getStyleClass().remove("nav-btn-active");

        switch (tab) {
            case "home" -> { if (homeBtn != null) homeBtn.getStyleClass().add("nav-btn-active"); }
            case "matieres" -> { if (matieresBtn != null) matieresBtn.getStyleClass().add("nav-btn-active"); }
            case "quiz" -> { if (quizBtn != null) quizBtn.getStyleClass().add("nav-btn-active"); }
        }
    }

    private void setContent(String fxml) {
        try {
            ViewNavigator.LoadedView view = ViewNavigator.load(fxml);
            setContent(view.root());
            if (view.controller() instanceof NavigableController navigable) {
                navigable.setAppShell(this);
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void setContent(Parent root) {
        if (contentHost != null) {
            contentHost.getChildren().setAll(root);
            // Ensure logout button is visible when not in quiz-pass
            if (logoutBtn != null) { logoutBtn.setVisible(true); logoutBtn.setManaged(true); }
        }
    }
}
