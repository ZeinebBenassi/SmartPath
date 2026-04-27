package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController implements NavigableController {
    @FXML private Label welcomeLabel;

    private AppShellController appShell;

    @FXML
    public void initialize() {
        String name = AppSession.getFullName();
        if (name != null && !name.trim().isEmpty()) {
            welcomeLabel.setText("👋 Bienvenue, " + name + " !");
        } else {
            welcomeLabel.setText("👋 Bienvenue sur SmartPath !");
        }
    }

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    @FXML
    public void goMatieres() {
        if (appShell != null) appShell.showMatieres();
    }

    @FXML
    public void goQuiz() {
        if (appShell != null) appShell.showQuiz();
    }
}
