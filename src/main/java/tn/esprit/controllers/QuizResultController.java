package tn.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entity.QuizResult;
import tn.esprit.services.UniversiteAIService;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    // ═══════════════════════════════════════════════════════════════════
    //  Initialisation
    // ═══════════════════════════════════════════════════════════════════

    public void initData(QuizResult result,
                         Map<String, Integer> scores,
                         String profileType,
                         List<Map<String, Object>> recommendations) {
        String[] info = PROFILE_INFO.getOrDefault(profileType,
                new String[]{"🎓", "Vous avez un profil équilibré en informatique."});
        lblProfileEmoji.setText(info[0]);
        lblProfileType.setText(profileType);
        lblProfileDesc.setText(info[1]);
        buildScoreBars(scores);
        buildRecommendations(recommendations);
    }

    public void initData(QuizResult result, Map<String, Integer> scores, String profileType) {
        initData(result, scores, profileType, new ArrayList<>());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Barres de scores animées
    // ═══════════════════════════════════════════════════════════════════

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

            final double targetPct  = pct;
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

    // ═══════════════════════════════════════════════════════════════════
    //  Recommandations filières + universités
    // ═══════════════════════════════════════════════════════════════════

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

            VBox filiereBox = new VBox(10);
            filiereBox.setStyle(
                "-fx-background-color:#F8FAFC; -fx-background-radius:12; " +
                "-fx-border-color:#CBD5E1; -fx-border-radius:12; " +
                "-fx-border-width:1; -fx-padding:16 18;");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label nomLbl = new Label("📚  " + filiereNom);
            nomLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label pctLbl = new Label(percentage + "% compatible");
            pctLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:white; " +
                "-fx-background-color:#6366F1; -fx-background-radius:20; -fx-padding:3 10;");
            header.getChildren().addAll(nomLbl, spacer, pctLbl);
            filiereBox.getChildren().add(header);

            filiereBox.getChildren().add(new Separator());

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

            Thread t = new Thread(() -> {
                List<Map<String, String>> universites = aiService.getUniversitesPourFiliere(filiereNom);
                Platform.runLater(() -> {
                    filiereBox.getChildren().remove(loadingBox);
                    if (universites.isEmpty()) {
                        Label noData = new Label("⚠  Aucune université trouvée pour cette filière.");
                        noData.setStyle("-fx-font-size:12px; -fx-text-fill:#EF4444;");
                        filiereBox.getChildren().add(noData);
                    } else {
                        Label univTitle = new Label("🏛  Universités & Facultés disponibles :");
                        univTitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B; -fx-font-weight:bold;");
                        filiereBox.getChildren().add(univTitle);
                        for (Map<String, String> u : universites)
                            filiereBox.getChildren().add(buildUniversiteCard(u));
                    }
                });
            }, "api-univ-" + filiereNom);
            t.setDaemon(true);
            t.start();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Carte université avec bouton 📍 Voir sur la carte
    // ═══════════════════════════════════════════════════════════════════

    private VBox buildUniversiteCard(Map<String, String> u) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-border-color:#6366F1; -fx-border-width:0 0 0 3; " +
            "-fx-padding:12 14; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);");

        String nom      = u.getOrDefault("nom",          "Université inconnue");
        String ville    = u.getOrDefault("ville",        "");
        String type     = u.getOrDefault("type",         "");
        String acces    = u.getOrDefault("acces",        "");
        String insert   = u.getOrDefault("tauxInsertion","");
        String frais    = u.getOrDefault("fraisAnnuels", "");
        String diplomes = u.getOrDefault("diplomes",     "");
        String desc     = u.getOrDefault("description",  "");
        String site     = u.getOrDefault("siteWeb",      "");
        String adresse  = u.getOrDefault("adresse",      "");

        Label nomLbl = new Label("🏛  " + nom);
        nomLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
        nomLbl.setWrapText(true);
        card.getChildren().add(nomLbl);

        FlowPane badges = new FlowPane(6, 4);
        if (!ville.isEmpty())  badges.getChildren().add(badge("📍 " + ville,  "#EFF6FF","#3B82F6"));
        if (!type.isEmpty())   badges.getChildren().add(badge("🏷 " + type,   "#F0FDF4","#16A34A"));
        if (!acces.isEmpty())  badges.getChildren().add(badge("🎯 " + acces,  "#FFF7ED","#EA580C"));
        if (!insert.isEmpty() && !insert.equals("0"))
            badges.getChildren().add(badge("💼 " + insert + "% insertion","#F5F3FF","#7C3AED"));
        if (!frais.isEmpty() && !frais.equals("0"))
            badges.getChildren().add(badge("💰 " + frais + " DT/an","#FFF1F2","#E11D48"));
        if (!badges.getChildren().isEmpty()) card.getChildren().add(badges);

        if (!desc.isEmpty()) {
            Label d = new Label(desc.length() > 130 ? desc.substring(0, 130) + "…" : desc);
            d.setStyle("-fx-font-size:11px; -fx-text-fill:#64748B;");
            d.setWrapText(true);
            card.getChildren().add(d);
        }
        if (!diplomes.isEmpty()) {
            Label d = new Label("🎓 Diplômes : " + diplomes);
            d.setStyle("-fx-font-size:11px; -fx-text-fill:#64748B;");
            d.setWrapText(true);
            card.getChildren().add(d);
        }

        // ── Boutons ──
        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(4, 0, 0, 0));

        // 📍 Voir sur la carte
        Button btnMap = new Button("📍 Voir sur la carte");
        btnMap.setStyle("-fx-background-color:linear-gradient(to right,#10b981,#34d399); " +
            "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:11px; " +
            "-fx-background-radius:20; -fx-padding:6 14; -fx-cursor:hand;");
        String mapQuery = !adresse.isEmpty()
            ? adresse + " " + ville
            : nom + " " + ville + " Tunisie";
        btnMap.setOnAction(e -> ouvrirCarteModal(nom, ville, mapQuery));
        btnRow.getChildren().add(btnMap);

        // 🌐 Site web
        if (!site.isEmpty() && !site.startsWith("https://...")) {
            Button btnSite = new Button("🌐 Site web");
            btnSite.setStyle("-fx-background-color:#EFF6FF; -fx-text-fill:#3B82F6; " +
                "-fx-font-size:11px; -fx-font-weight:bold; " +
                "-fx-background-radius:20; -fx-padding:6 14; -fx-cursor:hand; " +
                "-fx-border-color:#BFDBFE; -fx-border-radius:20; -fx-border-width:1;");
            btnSite.setOnAction(e -> ouvrirSiteWeb(site));
            btnRow.getChildren().add(btnSite);
        }

        card.getChildren().add(btnRow);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Modal Carte avec WebView + iframe HTML (solution à l'erreur iframe)
    // ═══════════════════════════════════════════════════════════════════

    private void ouvrirCarteModal(String nom, String ville, String mapQuery) {
        try {
            String encoded  = URLEncoder.encode(mapQuery, StandardCharsets.UTF_8);
            String gmapsUrl = "https://www.google.com/maps/search/" + encoded;

            // ── Construire un HTML complet avec un vrai <iframe> ──
            // C'est la seule façon de contourner l'erreur
            // "Google Maps Embed API must be used in an iframe"
            // loadContent() injecte le HTML directement dans le moteur WebKit
            // qui l'exécute comme un vrai navigateur avec iframe support.
            String iframeSrc = "https://maps.google.com/maps?q=" + encoded + "&output=embed&z=15";
            String html = "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset='UTF-8'>"
                + "<style>"
                + "  html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;}"
                + "  iframe{width:100%;height:100%;border:none;display:block;}"
                + "</style>"
                + "</head><body>"
                + "<iframe src='" + iframeSrc + "' allowfullscreen></iframe>"
                + "</body></html>";

            // ── Stage modal ──
            Stage mapStage = new Stage();
            mapStage.initModality(Modality.APPLICATION_MODAL);
            mapStage.setTitle("📍 " + nom);
            mapStage.setWidth(860);
            mapStage.setHeight(640);

            // ── Header dégradé vert→bleu (identique Symfony) ──
            VBox headerBox = new VBox(3);
            headerBox.setStyle("-fx-background-color:linear-gradient(to right,#10b981,#3b82f6);"
                + "-fx-padding:14 20;");
            Label titleLbl = new Label("📍 " + nom);
            titleLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
            Label subLbl   = new Label(ville.isEmpty() ? "" : "📌 " + ville);
            subLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);");
            headerBox.getChildren().addAll(titleLbl, subLbl);

            // ── WebView avec User-Agent Chrome pour que Google Maps accepte ──
            WebView webView = new WebView();
            webView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/124.0.0.0 Safari/537.36");
            // loadContent injecte le HTML directement → vrai iframe reconnu
            webView.getEngine().loadContent(html, "text/html");
            VBox.setVgrow(webView, Priority.ALWAYS);

            // ── Footer ──
            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setStyle("-fx-padding:10 18;-fx-background-color:#F8FAFC;"
                + "-fx-border-color:#E2E8F0;-fx-border-width:1 0 0 0;");

            Button btnGmaps = new Button("🗺️ Ouvrir dans Google Maps");
            btnGmaps.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:12px;"
                + "-fx-background-radius:10;-fx-padding:8 16;-fx-cursor:hand;");
            btnGmaps.setOnAction(e -> ouvrirSiteWeb(gmapsUrl));

            Button btnFermer = new Button("✕ Fermer");
            btnFermer.setStyle("-fx-background-color:white;-fx-text-fill:#64748B;"
                + "-fx-font-size:12px;-fx-background-radius:10;-fx-padding:8 16;"
                + "-fx-border-color:#E2E8F0;-fx-border-radius:10;-fx-border-width:1;-fx-cursor:hand;");
            btnFermer.setOnAction(e -> mapStage.close());

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            footer.getChildren().addAll(btnGmaps, sp, btnFermer);

            // ── Assembler ──
            VBox root = new VBox(headerBox, webView, footer);
            VBox.setVgrow(webView, Priority.ALWAYS);
            mapStage.setScene(new Scene(root));
            mapStage.setResizable(true);
            mapStage.show();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                "Impossible d'ouvrir la carte : " + ex.getMessage()).show();
        }
    }

    private void ouvrirSiteWeb(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir : " + url).show();
        }
    }

    private Label badge(String text, String bgColor, String textColor) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px;-fx-background-color:" + bgColor
            + ";-fx-text-fill:" + textColor + ";-fx-background-radius:20;-fx-padding:2 8;");
        return l;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Navigation
    // ═══════════════════════════════════════════════════════════════════

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
