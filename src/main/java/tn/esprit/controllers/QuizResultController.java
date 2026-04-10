package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entity.QuizResult;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class QuizResultController implements Initializable {

    @FXML private Label   lblProfileType;
    @FXML private Label   lblProfileEmoji;
    @FXML private Label   lblProfileDesc;
    @FXML private VBox    vboxScores;
    @FXML private Button  btnRetake;
    @FXML private Button  btnClose;
    @FXML private Label   lblCongrats;

    private static final Map<String, String[]> PROFILE_INFO = new LinkedHashMap<>() {{
        put("Data Analyst",            new String[]{"📊", "Vous excellez dans l'analyse et l'interprétation des données."});
        put("Développeur Full Stack",  new String[]{"💻", "Vous aimez créer des applications web de bout en bout."});
        put("UX/UI Designer",          new String[]{"🎨", "Votre créativité vous pousse vers le design d'interfaces."});
        put("Ingénieur Système",       new String[]{"⚙",  "Vous maîtrisez les systèmes et l'infrastructure."});
        put("Data Scientist",          new String[]{"🧮", "Les mathématiques et la modélisation sont vos points forts."});
        put("Expert IA/ML",            new String[]{"🤖", "L'intelligence artificielle et le machine learning vous passionnent."});
        put("Administrateur Système",  new String[]{"🖥",  "La gestion et l'optimisation des systèmes vous convient."});
        put("Ingénieur Réseau",        new String[]{"🌐", "Les réseaux et la connectivité sont votre domaine."});
        put("Expert Cybersécurité",    new String[]{"🔒", "Vous avez l'instinct pour protéger les systèmes."});
        put("Ingénieur Big Data",      new String[]{"🗄",  "La gestion de grandes masses de données est votre force."});
    }};

    private static final Map<String, String> TRAIT_COLORS = new LinkedHashMap<>() {{
        put("analytique",    "#4A90D9"); put("pratique",      "#27AE60");
        put("creatif",       "#E74C8C"); put("technique",     "#8E44AD");
        put("mathematique",  "#E67E22"); put("algorithmique", "#2980B9");
        put("systemes",      "#16A085"); put("reseaux",       "#2ECC71");
        put("securite",      "#E74C3C"); put("donnees",       "#3498DB");
    }};

    @Override public void initialize(URL url, ResourceBundle rb) {}

    public void initData(QuizResult result, Map<String, Integer> scores, String profileType) {
        String[] info = PROFILE_INFO.getOrDefault(profileType,
                new String[]{"🎓", "Vous avez un profil équilibré en informatique."});
        lblProfileEmoji.setText(info[0]);
        lblProfileType.setText(profileType);
        lblProfileDesc.setText(info[1]);
        buildScoreBars(scores);
    }

    private void buildScoreBars(Map<String, Integer> scores) {
        vboxScores.getChildren().clear();
        int maxScore = scores.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (maxScore == 0) maxScore = 1;

        int delay = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String trait = entry.getKey();
            int    score = entry.getValue();
            double pct   = (double) score / maxScore;
            String color = TRAIT_COLORS.getOrDefault(trait, "#4A90D9");

            Label lblTrait = new Label(capitalize(trait));
            lblTrait.getStyleClass().add("score-trait-label");
            lblTrait.setMinWidth(140);

            StackPane barBg = new StackPane();
            barBg.getStyleClass().add("score-bar-bg");
            barBg.setPrefHeight(26);
            barBg.setMaxWidth(Double.MAX_VALUE);

            Region barFill = new Region();
            barFill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12px;");
            barFill.setPrefHeight(26); barFill.setPrefWidth(0); barFill.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
            barBg.getChildren().add(barFill);

            Label lblVal = new Label(score + " pts");
            lblVal.getStyleClass().add("score-value-label");
            lblVal.setMinWidth(60); lblVal.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(10, lblTrait, barBg, lblVal);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(barBg, Priority.ALWAYS);
            row.getStyleClass().add("score-row");
            vboxScores.getChildren().add(row);

            final double targetPct  = pct;
            final Region finalFill  = barFill;
            final int    finalDelay = delay;

            barBg.widthProperty().addListener((obs, oldW, newW) -> {
                double target = newW.doubleValue() * targetPct;
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(finalFill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(800 + finalDelay), new KeyValue(finalFill.prefWidthProperty(), target))
                );
                tl.play();
            });
            delay += 80;
        }
    }

    @FXML private void handleRetake() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/QuizPlayer.fxml")));
            Stage stage = (Stage) btnRetake.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SmartPath — Quiz de Personnalité");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de relancer le quiz : " + e.getMessage()).show();
        }
    }

    @FXML private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        // Retourner au dashboard étudiant si la fenêtre est intégrée, sinon fermer
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml")));
            stage.setScene(new Scene(root));
            stage.setTitle("SmartPath — Dashboard Étudiant");
        } catch (IOException e) {
            stage.hide();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
