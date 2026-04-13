package com.smartpath.controller.feature_cours_et_quiz;

import com.smartpath.util.feature_cours_et_quiz.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController implements NavigableController {
    @FXML private Label welcomeLabel;

    private AppShellController appShell;

    @FXML
    public void initialize() {
        var user = AppSession.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("👋 Bienvenue, " + user.getFullName() + " !");
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
