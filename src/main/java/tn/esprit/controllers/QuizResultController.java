package tn.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
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
import tn.esprit.services.UniversiteAIService;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * QuizResultController
 *
 * Affiche :
 *  1) Le profil détecté + barres de scores
 *  2) Les filières recommandées
 *  3) Pour chaque filière : les universités/facultés récupérées
 *     DIRECTEMENT via l'API Groq (UniversiteAIService) — SANS base de données.
 */
public class QuizResultController implements Initializable {

    @FXML private Label  lblProfileType;
    @FXML private Label  lblProfileEmoji;
    @FXML private Label  lblProfileDesc;
    @FXML private VBox   vboxScores;
    @FXML private VBox   vboxRecommendations;
    @FXML private Button btnRetake;
    @FXML private Button btnClose;

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

    private final UniversiteAIService aiService = new UniversiteAIService();

    @Override public void initialize(URL url, ResourceBundle rb) {}

    // ------------------------------------------------------------------ //
    //  Initialisation — appelée depuis QuizPlayerController              //
    // ------------------------------------------------------------------ //

    public void initData(QuizResult result,
                         Map<String, Integer> scores,
                         String profileType,
                         List<Map<String, Object>> recommendations) {

        // --- Profil ---
        String[] info = PROFILE_INFO.getOrDefault(profileType,
                new String[]{"🎓", "Vous avez un profil équilibré en informatique."});
        lblProfileEmoji.setText(info[0]);
        lblProfileType.setText(profileType);
        lblProfileDesc.setText(info[1]);

        // --- Barres de scores ---
        buildScoreBars(scores);

        // --- Recommandations filières + universités via API ---
        buildRecommendations(recommendations);
    }

    /** Compatibilité avec l'ancien appel sans recommandations. */
    public void initData(QuizResult result, Map<String, Integer> scores, String profileType) {
        initData(result, scores, profileType, new ArrayList<>());
    }

    // ------------------------------------------------------------------ //
    //  Barres de scores animées                                           //
    // ------------------------------------------------------------------ //

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
            lblTrait.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");
            lblTrait.setMinWidth(140);

            StackPane barBg = new StackPane();
            barBg.setStyle("-fx-background-color:#F1F5F9; -fx-background-radius:12;");
            barBg.setPrefHeight(22);
            barBg.setMaxWidth(Double.MAX_VALUE);

            Region barFill = new Region();
            barFill.setStyle("-fx-background-color:" + color + "; -fx-background-radius:12;");
            barFill.setPrefHeight(22);
            barFill.setPrefWidth(0);
            barFill.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
            barBg.getChildren().add(barFill);

            Label lblVal = new Label(score + " pts");
            lblVal.setStyle("-fx-font-size:11px; -fx-text-fill:#64748B;");
            lblVal.setMinWidth(55);
            lblVal.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(10, lblTrait, barBg, lblVal);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(barBg, Priority.ALWAYS);
            vboxScores.getChildren().add(row);

            final double targetPct = pct;
            final int    finalDelay = delay;
            barBg.widthProperty().addListener((obs, o, n) -> {
                double target = n.doubleValue() * targetPct;
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(barFill.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(700 + finalDelay), new KeyValue(barFill.prefWidthProperty(), target))
                );
                tl.play();
            });
            delay += 70;
        }
    }

    // ------------------------------------------------------------------ //
    //  Recommandations filières + universités (API Groq, sans BDD)       //
    // ------------------------------------------------------------------ //

    /**
     * Pour chaque filière recommandée, on appelle UniversiteAIService
     * dans un thread séparé. L'UI se met à jour au fur et à mesure
     * que les résultats arrivent (Platform.runLater).
     *
     * AUCUNE base de données n'est utilisée ici.
     */
    private void buildRecommendations(List<Map<String, Object>> recommendations) {
        if (vboxRecommendations == null) return;
        vboxRecommendations.getChildren().clear();

        if (recommendations == null || recommendations.isEmpty()) {
            Label noRec = new Label("Aucune recommandation disponible.");
            noRec.setStyle("-fx-text-fill:#94A3B8; -fx-font-size:13px;");
            vboxRecommendations.getChildren().add(noRec);
            return;
        }

        for (Map<String, Object> rec : recommendations) {
            String filiereNom = (String) rec.get("filiereNom");
            int    percentage = (int)    rec.get("percentage");

            // ── Bloc filière ──
            VBox filiereBox = new VBox(10);
            filiereBox.setStyle(
                "-fx-background-color:#F8FAFC; -fx-background-radius:12; " +
                "-fx-border-color:#CBD5E1; -fx-border-radius:12; " +
                "-fx-border-width:1; -fx-padding:16 18;");

            // En-tête : nom filière + pourcentage
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label nomLbl = new Label("📚  " + filiereNom);
            nomLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label pctLbl = new Label(percentage + "% compatible");
            pctLbl.setStyle(
                "-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:white; " +
                "-fx-background-color:#6366F1; -fx-background-radius:20; -fx-padding:3 10;");
            header.getChildren().addAll(nomLbl, spacer, pctLbl);
            filiereBox.getChildren().add(header);

            Separator sep = new Separator();
            sep.setStyle("-fx-background-color:#E2E8F0;");
            filiereBox.getChildren().add(sep);

            // Spinner de chargement (affiché pendant l'appel API)
            HBox loadingBox = new HBox(8);
            loadingBox.setAlignment(Pos.CENTER_LEFT);
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(20, 20);
            spinner.setStyle("-fx-progress-color:#6366F1;");
            Label loadingLbl = new Label("Recherche des universités via IA Groq...");
            loadingLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8; -fx-font-style:italic;");
            loadingBox.getChildren().addAll(spinner, loadingLbl);
            filiereBox.getChildren().add(loadingBox);

            vboxRecommendations.getChildren().add(filiereBox);

            // ── Appel API Groq en arrière-plan ──
            final String finalNom    = filiereNom;
            final VBox   finalBox    = filiereBox;
            final HBox   finalLoading = loadingBox;

            Thread t = new Thread(() -> {
                // Appel DIRECT à l'API
                List<Map<String, String>> universites = aiService.getUniversitesPourFiliere(finalNom);

                Platform.runLater(() -> {
                    finalBox.getChildren().remove(finalLoading);

                    if (universites.isEmpty()) {
                        Label noData = new Label("⚠  Aucune université trouvée pour cette filière.");
                        noData.setStyle("-fx-font-size:12px; -fx-text-fill:#EF4444;");
                        finalBox.getChildren().add(noData);
                    } else {
                        // Titre sous-section
                        Label univTitle = new Label("🏛  Universités & Facultés disponibles :");
                        univTitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B; -fx-font-weight:bold;");
                        finalBox.getChildren().add(univTitle);

                        // Une carte par université
                        for (Map<String, String> u : universites) {
                            finalBox.getChildren().add(buildUniversiteCard(u));
                        }
                    }
                });
            }, "api-univ-" + filiereNom);
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Carte visuelle pour une université retournée par l'API.
     * Pas de BDD — données 100% issues de l'API Groq.
     */
    private VBox buildUniversiteCard(Map<String, String> u) {
        VBox card = new VBox(5);
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:8; " +
            "-fx-border-color:#6366F1; -fx-border-width:0 0 0 3; " +
            "-fx-padding:10 14; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),4,0,0,1);");

        // Nom de l'université
        String nom = u.getOrDefault("nom", "Université inconnue");
        Label nomLbl = new Label("🏛  " + nom);
        nomLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
        nomLbl.setWrapText(true);
        card.getChildren().add(nomLbl);

        // Méta-infos sur une ligne
        HBox metaBox = new HBox(10);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        String ville     = u.getOrDefault("ville", "");
        String type      = u.getOrDefault("type", "");
        String acces     = u.getOrDefault("acces", "");
        String insertion = u.getOrDefault("tauxInsertion", "");
        String frais     = u.getOrDefault("fraisAnnuels", "");
        String diplomes  = u.getOrDefault("diplomes", "");

        if (!ville.isEmpty())          metaBox.getChildren().add(badge("📍 " + ville,     "#EFF6FF", "#3B82F6"));
        if (!type.isEmpty())           metaBox.getChildren().add(badge("🏷 " + type,      "#F0FDF4", "#16A34A"));
        if (!acces.isEmpty())          metaBox.getChildren().add(badge("🎯 " + acces,     "#FFF7ED", "#EA580C"));
        if (!insertion.isEmpty() && !insertion.equals("0"))
                                       metaBox.getChildren().add(badge("💼 " + insertion + "% insertion", "#F5F3FF", "#7C3AED"));
        if (!frais.isEmpty() && !frais.equals("0"))
                                       metaBox.getChildren().add(badge("💰 " + frais + " DT/an", "#FFF1F2", "#E11D48"));

        if (!metaBox.getChildren().isEmpty()) card.getChildren().add(metaBox);

        // Description
        String desc = u.getOrDefault("description", "");
        if (!desc.isEmpty()) {
            Label descLbl = new Label(desc.length() > 130 ? desc.substring(0, 130) + "…" : desc);
            descLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#64748B;");
            descLbl.setWrapText(true);
            card.getChildren().add(descLbl);
        }

        // Diplômes
        if (!diplomes.isEmpty()) {
            Label dipLbl = new Label("🎓 Diplômes : " + diplomes);
            dipLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#64748B;");
            card.getChildren().add(dipLbl);
        }

        // Site web
        String site = u.getOrDefault("siteWeb", "");
        if (!site.isEmpty() && !site.equals("https://...") && !site.equals("https://site.tn")) {
            Label siteLbl = new Label("🔗 " + site);
            siteLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6366F1; -fx-underline:true;");
            card.getChildren().add(siteLbl);
        }

        return card;
    }

    /** Petit badge coloré pour les méta-infos. */
    private Label badge(String text, String bgColor, String textColor) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-font-size:10px; -fx-background-color:" + bgColor + "; " +
            "-fx-text-fill:" + textColor + "; -fx-background-radius:20; -fx-padding:2 8;");
        return l;
    }

    // ------------------------------------------------------------------ //
    //  Navigation                                                         //
    // ------------------------------------------------------------------ //

    @FXML private void handleRetake() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/QuizPlayer.fxml")));
            Stage stage = (Stage) btnRetake.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SmartPath — Quiz de Personnalité");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    @FXML private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
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
