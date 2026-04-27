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
    @FXML private Button matieresBtn;
    @FXML private Button quizBtn;

    private Role currentRole = Role.ETUDIANT;

    public void onAfterLogin() {
        User user = AppSession.getCurrentUser();
        currentRole = RoleUtils.normalize(user == null ? null : user.getType());

        userLabel.setText(user == null ? "" : (user.getPrenom() + " " + user.getNom()));
        roleBadge.getStyleClass().removeAll("role-admin", "role-prof", "role-etudiant");
        roleBadge.setText(RoleUtils.display(currentRole));
        switch (currentRole) {
            case ADMIN -> roleBadge.getStyleClass().add("role-admin");
            case PROF -> roleBadge.getStyleClass().add("role-prof");
            case ETUDIANT -> roleBadge.getStyleClass().add("role-etudiant");
        }

        matieresBtn.setDisable(false);
        quizBtn.setDisable(false);
        showHome();
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    @FXML
    public void showHome() {
        setContent("/tn/esprit/feature_cours_et_quiz/home.fxml");
    }

    @FXML
    public void showMatieres() {
        setContent("/tn/esprit/feature_cours_et_quiz/matiere-cards.fxml");
    }

    @FXML
    public void showQuiz() {
        setContent("/tn/esprit/feature_cours_et_quiz/quiz-cards.fxml");
    }

    @FXML
    public void handleLogout() {
        AppSession.clear();
        try {
            // Navigate back to TN Esprit login
            Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/Login.fxml"));
            matieresBtn.getScene().setRoot(root);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    public void showMatiereForm(Matiere existing) {
        try {
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
            ViewNavigator.LoadedView view = ViewNavigator.load("/tn/esprit/feature_cours_et_quiz/quiz-pass.fxml");
            if (view.controller() instanceof QuizPassController controller) {
                controller.setAppShell(this);
                controller.setQuiz(quiz);
            }
            setContent(view.root());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
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
        contentHost.getChildren().setAll(root);
    }
}
