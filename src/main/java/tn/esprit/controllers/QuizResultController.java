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

/**
 * QuizResultController
 *
 * APIs utilisées :
 *  - API 1 : Groq (LLM) → recommandations filières + universités
 *  - API 2 : Nominatim (OpenStreetMap) → géocodage université → carte
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
    //  Recommandations filières + universités  (API 1 : Groq)
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

            // ── Conteneur filière — design amélioré ──
            VBox filiereBox = new VBox(16);
            filiereBox.setStyle(
                    "-fx-background-color:#FFFFFF; -fx-background-radius:18; " +
                    "-fx-border-color:#E2E8F0; -fx-border-radius:18; -fx-border-width:1; " +
                    "-fx-padding:22 24;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),16,0,0,4);");

            // ── En-tête filière ──
            HBox header = new HBox(14);
            header.setAlignment(Pos.CENTER_LEFT);

            // Icône dans cercle coloré
            StackPane iconCircle = new StackPane();
            iconCircle.setPrefSize(46, 46);
            iconCircle.setMinSize(46, 46);
            iconCircle.setStyle("-fx-background-color:#EEF2FF; -fx-background-radius:50;");
            Label iconLbl = new Label("📚");
            iconLbl.setStyle("-fx-font-size:20px;");
            iconCircle.getChildren().add(iconLbl);

            VBox titleBox = new VBox(3);
            Label nomLbl = new Label(filiereNom);
            nomLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
            Label subLbl = new Label("Filière recommandée pour votre profil");
            subLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#94A3B8;");
            titleBox.getChildren().addAll(nomLbl, subLbl);
            HBox.setHgrow(titleBox, Priority.ALWAYS);

            // Badge % avec couleur dynamique selon score
            String badgeColor = percentage >= 70 ? "#10B981" : percentage >= 40 ? "#F59E0B" : "#6366F1";
            VBox pctBox = new VBox(1);
            pctBox.setAlignment(Pos.CENTER);
            pctBox.setStyle("-fx-background-color:" + badgeColor + "; -fx-background-radius:14; -fx-padding:10 18;");
            Label pctNum = new Label(percentage + "%");
            pctNum.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:white;");
            Label pctTxt = new Label("compatible");
            pctTxt.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.85);");
            pctBox.getChildren().addAll(pctNum, pctTxt);

            header.getChildren().addAll(iconCircle, titleBox, pctBox);
            filiereBox.getChildren().add(header);

            // Séparateur
            Region sep = new Region();
            sep.setPrefHeight(1);
            sep.setMaxWidth(Double.MAX_VALUE);
            sep.setStyle("-fx-background-color:#F1F5F9;");
            filiereBox.getChildren().add(sep);

            // ── Indicateur de chargement ──
            HBox loadingBox = new HBox(10);
            loadingBox.setAlignment(Pos.CENTER_LEFT);
            loadingBox.setPadding(new Insets(4, 0, 4, 0));
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(20, 20);
            spinner.setStyle("-fx-progress-color:#6366F1;");
            Label loadingLbl = new Label("Recherche des universités via IA Groq...");
            loadingLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8; -fx-font-style:italic;");
            loadingBox.getChildren().addAll(spinner, loadingLbl);
            filiereBox.getChildren().add(loadingBox);

            vboxRecommendations.getChildren().add(filiereBox);

            // ── Thread de récupération des universités ──
            Thread t = new Thread(() -> {
                List<Map<String, String>> universites = aiService.getUniversitesPourFiliere(filiereNom);
                Platform.runLater(() -> {
                    filiereBox.getChildren().remove(loadingBox);

                    if (universites.isEmpty()) {
                        Label noData = new Label("⚠  Aucune université trouvée pour cette filière.");
                        noData.setStyle("-fx-font-size:12px; -fx-text-fill:#EF4444;");
                        filiereBox.getChildren().add(noData);
                        return;
                    }

                    // Titre section avec compteur
                    HBox titreBox = new HBox(8);
                    titreBox.setAlignment(Pos.CENTER_LEFT);
                    Label iconU = new Label("🏛");
                    iconU.setStyle("-fx-font-size:13px;");
                    Label univTitle = new Label("Universités & Facultés disponibles");
                    univTitle.setStyle("-fx-font-size:12px; -fx-text-fill:#475569; -fx-font-weight:bold;");
                    Label countBadge = new Label(universites.size() + " établissements");
                    countBadge.setStyle(
                            "-fx-font-size:10px; -fx-text-fill:#6366F1; " +
                            "-fx-background-color:#EEF2FF; -fx-background-radius:20; -fx-padding:2 8;");
                    titreBox.getChildren().addAll(iconU, univTitle, countBadge);
                    filiereBox.getChildren().add(titreBox);

                    // ══ GRILLE 2 COLONNES FIXES — GridPane ══
                    GridPane grid = new GridPane();
                    grid.setHgap(14);
                    grid.setVgap(14);
                    grid.setMaxWidth(Double.MAX_VALUE);

                    ColumnConstraints col1 = new ColumnConstraints();
                    col1.setPercentWidth(50);
                    col1.setHgrow(Priority.ALWAYS);
                    col1.setFillWidth(true);
                    col1.setMinWidth(200);

                    ColumnConstraints col2 = new ColumnConstraints();
                    col2.setPercentWidth(50);
                    col2.setHgrow(Priority.ALWAYS);
                    col2.setFillWidth(true);
                    col2.setMinWidth(200);

                    grid.getColumnConstraints().addAll(col1, col2);

                    for (int i = 0; i < universites.size(); i++) {
                        VBox card = buildUniversiteCard(universites.get(i));
                        card.setMaxWidth(Double.MAX_VALUE);
                        GridPane.setFillWidth(card, true);
                        GridPane.setHgrow(card, Priority.ALWAYS);
                        grid.add(card, i % 2, i / 2);
                    }

                    filiereBox.getChildren().add(grid);
                });
            }, "groq-univ-" + filiereNom);
            t.setDaemon(true);
            t.start();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Card université
    // ═══════════════════════════════════════════════════════════════════

    private VBox buildUniversiteCard(Map<String, String> u) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color:#FAFBFF; -fx-background-radius:14; " +
                "-fx-border-color:#E2E8F0; -fx-border-radius:14; -fx-border-width:1; " +
                "-fx-padding:16 18;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.09),10,0,0,3);");

        String nom      = u.getOrDefault("nom",          "Université inconnue");
        String ville    = u.getOrDefault("ville",        "");
        String type     = u.getOrDefault("type",         "");
        String acces    = u.getOrDefault("acces",        "");
        String insert   = u.getOrDefault("tauxInsertion","");
        String frais    = u.getOrDefault("fraisAnnuels", "");
        String diplomes = u.getOrDefault("diplomes",     "");
        String desc     = u.getOrDefault("description",  "");
        String adresse  = u.getOrDefault("adresse",      "");

        // ── Nom université ──
        Label nomLbl = new Label("🏛  " + nom);
        nomLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1E293B;");
        nomLbl.setWrapText(true);
        nomLbl.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(nomLbl);

        // ── Badges info ──
        FlowPane badges = new FlowPane(5, 5);
        badges.setMaxWidth(Double.MAX_VALUE);
        if (!ville.isEmpty())
            badges.getChildren().add(badge("📍 " + ville,  "#EFF6FF","#3B82F6"));
        if (!type.isEmpty())
            badges.getChildren().add(badge("🏷 " + type,   "#F0FDF4","#16A34A"));
        if (!acces.isEmpty())
            badges.getChildren().add(badge("🎯 " + acces,  "#FFF7ED","#EA580C"));
        if (!insert.isEmpty() && !insert.equals("0"))
            badges.getChildren().add(badge("💼 " + insert + "% insertion","#F5F3FF","#7C3AED"));
        if (!frais.isEmpty() && !frais.equals("0"))
            badges.getChildren().add(badge("💰 " + frais + " DT/an","#FFF1F2","#E11D48"));
        if (!badges.getChildren().isEmpty())
            card.getChildren().add(badges);

        // ── Description ──
        if (!desc.isEmpty()) {
            Label d = new Label(desc.length() > 110 ? desc.substring(0, 110) + "…" : desc);
            d.setStyle("-fx-font-size:11px; -fx-text-fill:#64748B; -fx-line-spacing:2;");
            d.setWrapText(true);
            d.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(d);
        }

        // ── Diplômes dans encadré ──
        if (!diplomes.isEmpty()) {
            HBox diplBox = new HBox(5);
            diplBox.setAlignment(Pos.CENTER_LEFT);
            diplBox.setStyle("-fx-background-color:#F1F5F9; -fx-background-radius:8; -fx-padding:6 10;");
            Label diplLbl = new Label("🎓 " + diplomes);
            diplLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#475569; -fx-font-weight:bold;");
            diplLbl.setWrapText(true);
            diplBox.getChildren().add(diplLbl);
            card.getChildren().add(diplBox);
        }

        // Spacer
        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);
        card.getChildren().add(sp);

        // Séparateur
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color:#EEF2FF;");
        card.getChildren().add(divider);

        // ── Bouton unique : Voir la Map ──
        String mapQuery = !adresse.isEmpty()
                ? adresse + " " + ville + " Tunisie"
                : nom + " " + ville + " Tunisie";

        Button btnMap = new Button("📍 Voir la Map");
        btnMap.setMaxWidth(Double.MAX_VALUE);
        btnMap.setStyle(
                "-fx-background-color:linear-gradient(to right,#10b981,#34d399); " +
                "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px; " +
                "-fx-background-radius:10; -fx-padding:9 14; -fx-cursor:hand;");

        btnMap.setOnAction(e -> {
            btnMap.setDisable(true);
            btnMap.setText("⏳ Chargement...");
            Thread geo = new Thread(() -> {
                UniversiteAIService.GeoResult geo2 = aiService.geocoderUniversite(mapQuery);
                Platform.runLater(() -> {
                    btnMap.setDisable(false);
                    btnMap.setText("📍 Voir la Map");
                    ouvrirCarte(nom, ville, mapQuery, geo2);
                });
            }, "nominatim-" + nom);
            geo.setDaemon(true);
            geo.start();
        });

        card.getChildren().add(btnMap);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Carte — Tuiles OSM chargées côté Java
    // ═══════════════════════════════════════════════════════════════════

    private int lon2tile(double lon, int z) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << z));
    }

    private int lat2tile(double lat, int z) {
        double r = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * (1 << z));
    }

    private String tuileEnBase64(int z, int x, int y) {
        String[] subs = {"a", "b", "c"};
        String sub = subs[Math.abs(x + y) % 3];
        String urlStr = "https://" + sub + ".tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png";
        try {
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 SmartPathApp/1.0 (contact@smartpath.tn)");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            if (conn.getResponseCode() == 200) {
                byte[] bytes = conn.getInputStream().readAllBytes();
                return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void ouvrirCarte(String nom, String ville, String mapQuery,
                             UniversiteAIService.GeoResult geoResult) {
        try {
            String encoded  = URLEncoder.encode(mapQuery, StandardCharsets.UTF_8);
            String gmapsUrl = "https://www.google.com/maps/search/" + encoded;

            double lat   = (geoResult != null && geoResult.isValid()) ? geoResult.lat : 36.8065;
            double lon   = (geoResult != null && geoResult.isValid()) ? geoResult.lon : 10.1815;
            boolean found = geoResult != null && geoResult.isValid();
            int zoom = found ? 15 : 7;

            int tx0 = lon2tile(lon, zoom);
            int ty0 = lat2tile(lat, zoom);
            StringBuilder tilesJson = new StringBuilder("{");
            boolean first = true;
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    int nx = tx0 + dx, ny = ty0 + dy;
                    String b64 = tuileEnBase64(zoom, nx, ny);
                    if (b64 != null) {
                        if (!first) tilesJson.append(",");
                        tilesJson.append("\"").append(zoom).append("/")
                                .append(nx).append("/").append(ny)
                                .append("\":\"").append(b64).append("\"");
                        first = false;
                    }
                }
            }
            tilesJson.append("}");

            String nomJs   = nom.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "&quot;");
            String villeJs = ville.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "&quot;");
            String html = buildMapHtml(lat, lon, zoom, found, nomJs, villeJs, tilesJson.toString());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("📍 " + nom);
            stage.setWidth(940);
            stage.setHeight(700);

            VBox header = new VBox(2);
            header.setStyle("-fx-background-color:linear-gradient(to right,#10b981,#3b82f6);-fx-padding:14 20;");
            Label t1 = new Label("📍 " + nom);
            t1.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
            Label t2 = new Label(found
                    ? "📌 " + ville + "  •  Localisé via Nominatim (OpenStreetMap)"
                    : "⚠️ Position approchée — " + ville);
            t2.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
            header.getChildren().addAll(t1, t2);

            WebView webView = new WebView();
            webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            webView.getEngine().setJavaScriptEnabled(true);
            webView.getEngine().loadContent(html, "text/html");
            VBox.setVgrow(webView, Priority.ALWAYS);

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setStyle("-fx-padding:10 18;-fx-background-color:#F8FAFC;-fx-border-color:#E2E8F0;-fx-border-width:1 0 0 0;");
            Label coord = new Label(found ? String.format("🌐 %.4f, %.4f", lat, lon) : "🌐 non localisé");
            coord.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;");
            Button btnG = new Button("🗺 Ouvrir dans Google Maps");
            btnG.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;-fx-background-radius:10;-fx-padding:8 16;-fx-cursor:hand;");
            btnG.setOnAction(e -> ouvrirSiteWeb(gmapsUrl));
            Button btnF = new Button("✕ Fermer");
            btnF.setStyle("-fx-background-color:white;-fx-text-fill:#64748B;-fx-font-size:12px;-fx-background-radius:10;-fx-padding:8 16;-fx-border-color:#E2E8F0;-fx-border-radius:10;-fx-border-width:1;-fx-cursor:hand;");
            btnF.setOnAction(e -> stage.close());
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            footer.getChildren().addAll(coord, sp, btnG, btnF);

            VBox root = new VBox(header, webView, footer);
            VBox.setVgrow(webView, Priority.ALWAYS);
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.show();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la carte : " + ex.getMessage()).show();
        }
    }

    private String buildMapHtml(double lat, double lon, int zoom, boolean found,
                                String nomJs, String villeJs, String tilesJson) {
        String markerDisplay    = found ? "block" : "none";
        String notFoundDisplay  = found ? "none"  : "block";
        return "<!DOCTYPE html><html><head>"
                + "<meta charset='UTF-8'>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box;}"
                + "html,body{width:100%;height:100%;background:#e8e0d8;font-family:Arial,sans-serif;overflow:hidden;}"
                + "#bar{position:absolute;top:0;left:0;right:0;height:52px;z-index:200;"
                + "  display:flex;align-items:center;padding:0 14px;gap:10px;"
                + "  background:linear-gradient(135deg,#10b981,#3b82f6);"
                + "  box-shadow:0 2px 12px rgba(0,0,0,0.3);}"
                + "#inp{flex:1;padding:9px 18px;border-radius:24px;border:none;"
                + "  font-size:13px;outline:none;box-shadow:0 1px 4px rgba(0,0,0,0.15);}"
                + "#sbtn{padding:9px 20px;background:white;color:#10b981;"
                + "  border:none;border-radius:24px;cursor:pointer;font-weight:bold;"
                + "  font-size:12px;white-space:nowrap;transition:background .2s;}"
                + "#sbtn:hover{background:#f0fdf4;}"
                + "#mc{position:absolute;top:52px;left:0;cursor:grab;}"
                + "#mc.drag{cursor:grabbing;}"
                + "#mk{position:absolute;transform:translate(-50%,-100%);pointer-events:none;"
                + "  z-index:100;display:" + markerDisplay + ";transition:left .1s,top .1s;}"
                + "#pp{position:absolute;background:white;border:1px solid #d1d5db;"
                + "  border-radius:10px;padding:9px 13px;font-size:12px;z-index:110;"
                + "  box-shadow:0 4px 16px rgba(0,0,0,0.18);max-width:240px;"
                + "  display:" + markerDisplay + ";pointer-events:none;}"
                + "#zc{position:absolute;top:62px;right:12px;z-index:150;"
                + "  display:flex;flex-direction:column;gap:5px;}"
                + "#zc button{width:34px;height:34px;border:1px solid #d1d5db;"
                + "  background:white;border-radius:8px;cursor:pointer;font-size:18px;"
                + "  font-weight:bold;line-height:1;box-shadow:0 2px 6px rgba(0,0,0,0.18);"
                + "  transition:background .15s;}"
                + "#zc button:hover{background:#f0fdf4;}"
                + "#nf{position:absolute;top:62px;left:50%;transform:translateX(-50%);"
                + "  background:#FEF2F2;border:1px solid #FCA5A5;color:#DC2626;"
                + "  padding:9px 22px;border-radius:10px;font-size:12px;z-index:180;"
                + "  display:" + notFoundDisplay + ";pointer-events:none;}"
                + "#at{position:absolute;bottom:6px;right:10px;font-size:10px;"
                + "  color:#555;background:rgba(255,255,255,0.85);padding:2px 7px;"
                + "  border-radius:4px;z-index:100;}"
                + "#st{position:absolute;bottom:6px;left:10px;font-size:10px;"
                + "  color:#555;background:rgba(255,255,255,0.85);padding:2px 7px;"
                + "  border-radius:4px;z-index:100;}"
                + "</style></head><body>"
                + "<div id='bar'>"
                + "  <input id='inp' type='text' value='" + nomJs + " " + villeJs + "' placeholder='Rechercher...'/>"
                + "  <button id='sbtn' onclick='search()'>&#128269; Rechercher</button>"
                + "</div>"
                + "<canvas id='mc'></canvas>"
                + "<div id='mk'>"
                + "<svg width='30' height='40' viewBox='0 0 30 40' xmlns='http://www.w3.org/2000/svg'>"
                + "<path d='M15 0C6.7 0 0 6.7 0 15c0 11.3 15 25 15 25S30 26.3 30 15C30 6.7 23.3 0 15 0z' fill='#10b981' stroke='white' stroke-width='2'/>"
                + "<circle cx='15' cy='15' r='7' fill='white'/>"
                + "<circle cx='15' cy='15' r='4' fill='#10b981'/>"
                + "</svg></div>"
                + "<div id='pp'><strong>" + nomJs + "</strong><br/>"
                + "<span style='color:#6b7280;font-size:11px'>&#128205; " + villeJs + "</span></div>"
                + "<div id='zc'>"
                + "<button onclick='zoom(1)' title='Zoom +'>+</button>"
                + "<button onclick='zoom(-1)' title='Zoom -'>&#8722;</button>"
                + "</div>"
                + "<div id='nf'>&#9888; Position approch&#233;e — non localis&#233; exactement</div>"
                + "<div id='at'>&copy; OpenStreetMap contributors</div>"
                + "<div id='st'>Z:" + zoom + "</div>"
                + "<script>"
                + "var TILES=" + tilesJson + ";\n"
                + "var imgCache={};\n"
                + "var CLat=" + lat + ", CLon=" + lon + ";\n"
                + "var MLat=" + lat + ", MLon=" + lon + ";\n"
                + "var Z=" + zoom + ";\n"
                + "var TS=256;\n"
                + "var W=0, H=0;\n"
                + "var canvas=document.getElementById('mc');\n"
                + "var ctx=canvas.getContext('2d');\n"
                + "function l2t(lon,z){return Math.floor((lon+180)/360*(1<<z));}\n"
                + "function a2t(lat,z){var r=lat*Math.PI/180;return Math.floor((1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2*(1<<z));}\n"
                + "function t2l(x,z){return x/(1<<z)*360-180;}\n"
                + "function t2a(y,z){var n=Math.PI-2*Math.PI*y/(1<<z);return 180/Math.PI*Math.atan(.5*(Math.exp(n)-Math.exp(-n)));}\n"
                + "function pxInTile(lon,z){var tx=l2t(lon,z);return (lon-t2l(tx,z))/(t2l(tx+1,z)-t2l(tx,z))*TS;}\n"
                + "function pyInTile(lat,z){var ty=a2t(lat,z);return (lat-t2a(ty,z))/(t2a(ty+1,z)-t2a(ty,z))*TS;}\n"
                + "function draw(){\n"
                + "  ctx.clearRect(0,0,W,H);\n"
                + "  ctx.fillStyle='#e8e0d8'; ctx.fillRect(0,0,W,H);\n"
                + "  var tx0=l2t(CLon,Z), ty0=a2t(CLat,Z);\n"
                + "  var px0=pxInTile(CLon,Z), py0=pyInTile(CLat,Z);\n"
                + "  var ox=Math.floor(W/2-px0), oy=Math.floor(H/2-py0);\n"
                + "  var r=3;\n"
                + "  for(var dy=-r;dy<=r;dy++){for(var dx=-r;dx<=r;dx++){\n"
                + "    var nx=tx0+dx, ny=ty0+dy;\n"
                + "    var k=Z+'/'+nx+'/'+ny;\n"
                + "    var px=ox+dx*TS, py=oy+dy*TS;\n"
                + "    if(px+TS<0||px>W||py+TS<0||py>H) continue;\n"
                + "    if(TILES[k]){\n"
                + "      if(!imgCache[k]){\n"
                + "        var img=new Image();\n"
                + "        img.onload=function(){draw();};\n"
                + "        img.src=TILES[k];\n"
                + "        imgCache[k]=img;\n"
                + "      }\n"
                + "      if(imgCache[k]&&imgCache[k].complete&&imgCache[k].naturalWidth>0)\n"
                + "        ctx.drawImage(imgCache[k],px,py,TS,TS);\n"
                + "      else{drawPlaceholder(px,py,k);}\n"
                + "    } else {\n"
                + "      drawPlaceholder(px,py,k);\n"
                + "    }\n"
                + "  }}\n"
                + "  document.getElementById('st').textContent='Z:'+Z+' | '+CLat.toFixed(4)+', '+CLon.toFixed(4);\n"
                + "  updateMarker(tx0,ty0,ox,oy);\n"
                + "}\n"
                + "function drawPlaceholder(px,py,k){\n"
                + "  ctx.fillStyle='#d4cfc9'; ctx.fillRect(px,py,TS,TS);\n"
                + "  ctx.strokeStyle='#c5c0bb'; ctx.lineWidth=1; ctx.strokeRect(px,py,TS,TS);\n"
                + "  ctx.fillStyle='#999'; ctx.font='10px Arial';\n"
                + "  ctx.fillText(k,px+4,py+14);\n"
                + "}\n"
                + "function updateMarker(tx0,ty0,ox,oy){\n"
                + "  var mk=document.getElementById('mk');\n"
                + "  var pp=document.getElementById('pp');\n"
                + "  if(mk.style.display==='none') return;\n"
                + "  var mx=ox+(l2t(MLon,Z)-tx0)*TS+pxInTile(MLon,Z);\n"
                + "  var my=oy+(a2t(MLat,Z)-ty0)*TS+pyInTile(MLat,Z);\n"
                + "  mk.style.left=mx+'px'; mk.style.top=my+'px';\n"
                + "  pp.style.left=(mx+18)+'px'; pp.style.top=(my-50)+'px';\n"
                + "}\n"
                + "function resize(){\n"
                + "  W=window.innerWidth; H=window.innerHeight-52;\n"
                + "  canvas.width=W; canvas.height=H;\n"
                + "  canvas.style.width=W+'px'; canvas.style.height=H+'px';\n"
                + "  draw();\n"
                + "}\n"
                + "var drag=false, lx=0, ly=0;\n"
                + "canvas.addEventListener('mousedown',function(e){drag=true;lx=e.clientX;ly=e.clientY;canvas.classList.add('drag');});\n"
                + "window.addEventListener('mouseup',function(){drag=false;canvas.classList.remove('drag');});\n"
                + "window.addEventListener('mousemove',function(e){\n"
                + "  if(!drag) return;\n"
                + "  var dx=e.clientX-lx, dy=e.clientY-ly; lx=e.clientX; ly=e.clientY;\n"
                + "  var sc=360/(TS*(1<<Z));\n"
                + "  CLon-=dx*sc;\n"
                + "  var mr=CLat*Math.PI/180;\n"
                + "  var my=Math.log(Math.tan(Math.PI/4+mr/2))+dy*2*Math.PI/(TS*(1<<Z));\n"
                + "  CLat=2*Math.atan(Math.exp(my))*180/Math.PI-90;\n"
                + "  CLon=Math.max(-180,Math.min(180,CLon));\n"
                + "  CLat=Math.max(-85,Math.min(85,CLat));\n"
                + "  draw();\n"
                + "});\n"
                + "canvas.addEventListener('wheel',function(e){\n"
                + "  e.preventDefault(); zoom(e.deltaY<0?1:-1);\n"
                + "},{passive:false});\n"
                + "function zoom(d){\n"
                + "  var nz=Math.max(2,Math.min(18,Z+d)); if(nz===Z) return;\n"
                + "  Z=nz; draw();\n"
                + "}\n"
                + "function search(){\n"
                + "  var q=document.getElementById('inp').value.trim(); if(!q) return;\n"
                + "  document.getElementById('sbtn').textContent='...';\n"
                + "  var xhr=new XMLHttpRequest();\n"
                + "  xhr.open('GET','https://nominatim.openstreetmap.org/search?q='+encodeURIComponent(q)+'&format=json&limit=1&accept-language=fr',true);\n"
                + "  xhr.onload=function(){\n"
                + "    document.getElementById('sbtn').innerHTML='&#128269; Rechercher';\n"
                + "    if(xhr.status===200){\n"
                + "      var d=JSON.parse(xhr.responseText);\n"
                + "      if(d&&d.length>0){\n"
                + "        var lt=parseFloat(d[0].lat), ln=parseFloat(d[0].lon);\n"
                + "        CLat=lt; CLon=ln; MLat=lt; MLon=ln; Z=15;\n"
                + "        document.getElementById('mk').style.display='block';\n"
                + "        document.getElementById('pp').style.display='block';\n"
                + "        document.getElementById('pp').innerHTML='<strong>'+d[0].display_name.substring(0,55)+'...</strong>';\n"
                + "        document.getElementById('nf').style.display='none';\n"
                + "        draw();\n"
                + "      } else {\n"
                + "        document.getElementById('nf').style.display='block';\n"
                + "        document.getElementById('nf').textContent='&#9888; Introuvable : '+q;\n"
                + "      }\n"
                + "    }\n"
                + "  };\n"
                + "  xhr.onerror=function(){document.getElementById('sbtn').innerHTML='&#128269; Rechercher';};\n"
                + "  xhr.send();\n"
                + "}\n"
                + "document.getElementById('inp').addEventListener('keydown',function(e){if(e.key==='Enter')search();});\n"
                + "window.addEventListener('resize',resize);\n"
                + "resize();\n"
                + "</script></body></html>";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Utilitaires
    // ═══════════════════════════════════════════════════════════════════

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
                + ";-fx-text-fill:" + textColor + ";-fx-background-radius:20;-fx-padding:3 9;");
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
