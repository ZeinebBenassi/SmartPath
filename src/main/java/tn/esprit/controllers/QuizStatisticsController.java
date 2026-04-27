package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import tn.esprit.services.QuizStatisticsService;
import tn.esprit.services.QuizStatisticsService.FiliereStatEntry;

import java.net.URL;
import java.util.*;

/**
 * QuizStatisticsController — IDENTIQUE à admin/quiz/statistics.html.twig Symfony.
 *
 * Variables Twig → Java :
 *   total_results  → int
 *   filiere_stats  → List<FiliereStatEntry>  (filiere objet + recommendations)
 *   profile_stats  → LinkedHashMap<String,Integer> trié DESC
 *
 * Charts :
 *   1. Bar Chart  vertical  — filières   (Canvas JavaFX)
 *   2. Donut Chart          — profils    (Canvas JavaFX, cutout 62%, légende bas)
 */
public class QuizStatisticsController implements Initializable {

    // ── Banner ──
    @FXML private Label lblBannerTotal;
    @FXML private Label lblBannerMiniStats;
    @FXML private Label lblInsightFilieres;
    @FXML private Label lblInsightProfils;

    // ── 4 Insight cards ──
    @FXML private Label lblTopFiliereName;
    @FXML private Label lblTopFiliereCount;
    @FXML private Label lblTopProfilName;
    @FXML private Label lblTopProfilCount;
    @FXML private Label lblInsightFilieres2;
    @FXML private Label lblInsightProfils2;
    @FXML private Label lblInsightProfilsSub;

    // ── Liste filières ──
    @FXML private VBox  vboxFilieres;
    @FXML private Label lblBadgeFilieres;

    // ── Liste profils ──
    @FXML private VBox  vboxProfils;
    @FXML private Label lblBadgeProfils;

    // ── Charts (conteneurs dans le FXML) ──
    @FXML private VBox vboxBarChart;
    @FXML private VBox vboxDonutChart;

    // ── Empty / Content ──
    @FXML private VBox vboxEmpty;
    @FXML private VBox vboxContent;

    private final QuizStatisticsService service = new QuizStatisticsService();

    // ── Palettes identiques Chart.js Symfony ──
    // backgroundColors  : rgba(X,.8)  → opacité 80 %
    // borderColors      : rgba(X,1)   → opacité 100 %
    private static final Color[] CHART_FILL = {
        Color.web("#6366f1", 0.8), Color.web("#0ea5e9", 0.8),
        Color.web("#10b981", 0.8), Color.web("#f59e0b", 0.8),
        Color.web("#ef4444", 0.8), Color.web("#a855f7", 0.8),
        Color.web("#ec4899", 0.8), Color.web("#14b8a6", 0.8),
        Color.web("#f97316", 0.8), Color.web("#84cc16", 0.8),
        Color.web("#06b6d4", 0.8), Color.web("#fbbf24", 0.8),
    };
    private static final Color[] CHART_BORDER = {
        Color.web("#6366f1"), Color.web("#0ea5e9"),
        Color.web("#10b981"), Color.web("#f59e0b"),
        Color.web("#ef4444"), Color.web("#a855f7"),
        Color.web("#ec4899"), Color.web("#14b8a6"),
        Color.web("#f97316"), Color.web("#84cc16"),
        Color.web("#06b6d4"), Color.web("#fbbf24"),
    };
    // Couleurs CSS hex (pour les labels JavaFX)
    private static final String[] CHART_HEX = {
        "#6366f1","#0ea5e9","#10b981","#f59e0b",
        "#ef4444","#a855f7","#ec4899","#14b8a6",
        "#f97316","#84cc16","#06b6d4","#fbbf24",
    };

    // Badges profils (identiques Symfony .profile-* CSS)
    private static final Map<String, String[]> PROFILE_BADGE = new LinkedHashMap<>() {{
        put("Data Analyst",             new String[]{"#dbeafe","#1e40af"});
        put("Développeur Full Stack",   new String[]{"#fef3c7","#92400e"});
        put("UX/UI Designer",           new String[]{"#fce7f3","#9d174d"});
        put("Ingénieur Système",        new String[]{"#d1fae5","#065f46"});
        put("Data Scientist",           new String[]{"#ede9fe","#5b21b6"});
        put("Expert IA/ML",             new String[]{"#fef9c3","#713f12"});
        put("Administrateur Système",   new String[]{"#dbeafe","#1e40af"});
        put("Ingénieur Réseau",         new String[]{"#d1fae5","#065f46"});
        put("Expert Cybersécurité",     new String[]{"#fee2e2","#991b1b"});
        put("Ingénieur Big Data",       new String[]{"#ede9fe","#5b21b6"});
        put("Informaticien Polyvalent", new String[]{"#f0fdf4","#166534"});
    }};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStatistics();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  loadStatistics() — équivalent AdminQuizController::statistics()
    // ═════════════════════════════════════════════════════════════════════

    private void loadStatistics() {
        int                           totalResults = service.getTotalResults();
        List<FiliereStatEntry>        filiereStats = service.getFiliereStats();
        LinkedHashMap<String,Integer> profileStats = service.getProfileStats();

        // ── Banner ──
        setLabel(lblBannerTotal,     String.valueOf(totalResults));
        setLabel(lblBannerMiniStats, filiereStats.size() + " filières · " + profileStats.size() + " profils distincts");
        setLabel(lblInsightFilieres, String.valueOf(filiereStats.size()));
        setLabel(lblInsightProfils,  String.valueOf(profileStats.size()));

        // ── État vide ──
        boolean empty = (totalResults == 0);
        if (vboxEmpty   != null) { vboxEmpty.setVisible(empty);   vboxEmpty.setManaged(empty);   }
        if (vboxContent != null) { vboxContent.setVisible(!empty); vboxContent.setManaged(!empty); }
        if (empty) return;

        // ── Insight cards ──
        String topFiliereName  = filiereStats.isEmpty() ? "—" : filiereStats.get(0).filiere.getNom();
        int    topFiliereCount = filiereStats.isEmpty() ? 0   : filiereStats.get(0).recommendations;
        setLabel(lblTopFiliereName,  topFiliereName);
        setLabel(lblTopFiliereCount, topFiliereCount + " recommandation" + (topFiliereCount > 1 ? "s" : ""));

        String topProfilName  = profileStats.isEmpty() ? "—" : profileStats.keySet().iterator().next();
        int    topProfilCount = profileStats.isEmpty() ? 0   : profileStats.values().iterator().next();
        setLabel(lblTopProfilName,  topProfilName);
        setLabel(lblTopProfilCount, topProfilCount + " étudiant" + (topProfilCount > 1 ? "s" : ""));

        setLabel(lblInsightFilieres2,  String.valueOf(filiereStats.size()));
        setLabel(lblInsightProfils2,   String.valueOf(profileStats.size()));
        setLabel(lblInsightProfilsSub, "sur " + totalResults + " quiz");

        setLabel(lblBadgeFilieres, filiereStats.size() + " filière" + (filiereStats.size() > 1 ? "s" : ""));
        setLabel(lblBadgeProfils,  profileStats.size() + " profil"  + (profileStats.size()  > 1 ? "s" : ""));

        // ── Listes ──
        buildFiliereList(filiereStats);
        buildProfilList(profileStats, totalResults);

        // ── Charts ──
        buildBarChart(filiereStats, totalResults);
        buildDonutChart(profileStats, totalResults);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Liste filières
    // ═════════════════════════════════════════════════════════════════════

    private void buildFiliereList(List<FiliereStatEntry> stats) {
        if (vboxFilieres == null) return;
        vboxFilieres.getChildren().clear();

        if (stats.isEmpty() || stats.stream().allMatch(s -> s.recommendations == 0)) {
            Label lbl = new Label("📊  Aucune recommandation enregistrée");
            lbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-padding:20 0;");
            vboxFilieres.getChildren().add(lbl);
            return;
        }

        int maxRec = stats.stream().mapToInt(s -> s.recommendations).max().orElse(1);

        for (int i = 0; i < stats.size(); i++) {
            vboxFilieres.getChildren().add(buildFiliereRow(i + 1, stats.get(i), maxRec));
        }
    }

    private HBox buildFiliereRow(int rank, FiliereStatEntry entry, int maxRec) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:10 0; -fx-border-color:transparent transparent #f1f5f9 transparent; -fx-border-width:0 0 1 0;");

        // Rang — fsr-1/fsr-2/fsr-3/fsr-n identique Symfony
        String rankText = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
        String rankBg   = rank == 1 ? "#fef3c7" : rank == 2 ? "#f1f5f9" : rank == 3 ? "#fdecea" : "#e2e8f0";
        String rankFg   = rank == 1 ? "#92400e" : rank == 2 ? "#475569"  : rank == 3 ? "#9a3412" : "#64748b";
        Label rankLbl   = new Label(rankText);
        rankLbl.setStyle(
            "-fx-background-color:" + rankBg + ";" +
            "-fx-text-fill:" + rankFg + ";" +
            "-fx-background-radius:10;" +
            "-fx-padding:4 8;" +
            "-fx-font-size:" + (rank <= 3 ? "15" : "11") + "px;" +
            "-fx-font-weight:800;" +
            "-fx-min-width:36px; -fx-min-height:36px;" +
            "-fx-alignment:center;"
        );

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        String icon = (entry.filiere.getIcon() != null && !entry.filiere.getIcon().isBlank())
                      ? entry.filiere.getIcon() : "🎓";
        Label nomLbl = new Label(icon + "  " + entry.filiere.getNom());
        nomLbl.setStyle("-fx-font-weight:600; -fx-text-fill:#1e293b; -fx-font-size:12px;");

        double pct = maxRec > 0 ? (double) entry.recommendations / maxRec : 0;
        StackPane barBg = new StackPane();
        barBg.setStyle("-fx-background-color:#f1f5f9; -fx-background-radius:50px;");
        barBg.setPrefHeight(8);
        barBg.setMaxWidth(Double.MAX_VALUE);

        Region fill = new Region();
        fill.setStyle("-fx-background-color:linear-gradient(to right,#6366f1,#0ea5e9); -fx-background-radius:50px;");
        fill.setPrefHeight(8);
        fill.prefWidthProperty().bind(barBg.widthProperty().multiply(pct));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        barBg.getChildren().add(fill);

        info.getChildren().addAll(nomLbl, barBg);

        Label countLbl = new Label(String.valueOf(entry.recommendations));
        countLbl.setStyle(
            "-fx-background-color:#ddd6fe; -fx-text-fill:#6366f1;" +
            "-fx-background-radius:50px; -fx-padding:3 12;" +
            "-fx-font-size:12px; -fx-font-weight:700;" +
            "-fx-min-width:36px; -fx-alignment:center;"
        );

        row.getChildren().addAll(rankLbl, info, countLbl);
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Liste profils
    // ═════════════════════════════════════════════════════════════════════

    private void buildProfilList(LinkedHashMap<String, Integer> stats, int total) {
        if (vboxProfils == null) return;
        vboxProfils.getChildren().clear();

        if (stats.isEmpty()) {
            Label lbl = new Label("👤  Aucun profil enregistré");
            lbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-padding:20 0;");
            vboxProfils.getChildren().add(lbl);
            return;
        }

        int maxCount = stats.values().stream().mapToInt(i -> i).max().orElse(1);
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
            vboxProfils.getChildren().add(buildProfilRow(e.getKey(), e.getValue(), maxCount, total));
        }
    }

    private HBox buildProfilRow(String profil, int count, int maxCount, int total) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:8 0; -fx-border-color:transparent transparent #f1f5f9 transparent; -fx-border-width:0 0 1 0;");

        String[] badge = PROFILE_BADGE.getOrDefault(profil, new String[]{"#dbeafe","#1e40af"});

        Label nomLbl = new Label(profil);
        nomLbl.setStyle(
            "-fx-background-color:" + badge[0] + ";" +
            "-fx-text-fill:" + badge[1] + ";" +
            "-fx-background-radius:50px; -fx-padding:3 10;" +
            "-fx-font-size:11px; -fx-font-weight:700;" +
            "-fx-min-width:150px;"
        );

        double pct = maxCount > 0 ? (double) count / maxCount : 0;
        StackPane barBg = new StackPane();
        barBg.setStyle("-fx-background-color:#f1f5f9; -fx-background-radius:50px;");
        barBg.setPrefHeight(7);
        HBox.setHgrow(barBg, Priority.ALWAYS);

        Region fill = new Region();
        fill.setStyle("-fx-background-color:linear-gradient(to right,#10b981,#0ea5e9); -fx-background-radius:50px;");
        fill.setPrefHeight(7);
        fill.prefWidthProperty().bind(barBg.widthProperty().multiply(pct));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        barBg.getChildren().add(fill);

        int pctInt = total > 0 ? count * 100 / total : 0;
        Label countLbl = new Label(count + " (" + pctInt + "%)");
        countLbl.setStyle(
            "-fx-background-color:#d1fae5; -fx-text-fill:#065f46;" +
            "-fx-background-radius:50px; -fx-padding:3 10;" +
            "-fx-font-size:11px; -fx-font-weight:700;"
        );

        row.getChildren().addAll(nomLbl, barBg, countLbl);
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Bar Chart — Canvas JavaFX (identique Chart.js "bar" Symfony)
    //  - type:'bar', borderRadius:10, borderSkipped:false
    //  - axe Y : beginAtZero, stepSize:1, grid:#f1f5f9
    //  - axe X : labels tronqués 16 chars, maxRotation:35
    //  - tooltip : "X rec. — Y%"
    // ═════════════════════════════════════════════════════════════════════

    private void buildBarChart(List<FiliereStatEntry> stats, int total) {
        if (vboxBarChart == null) return;
        vboxBarChart.getChildren().clear();

        List<FiliereStatEntry> nonZero = stats.stream()
            .filter(s -> s.recommendations > 0).toList();

        if (nonZero.isEmpty()) {
            Label lbl = new Label("Aucune donnée à afficher");
            lbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-padding:30 0;");
            vboxBarChart.getChildren().add(lbl);
            return;
        }

        // Dimensions — identiques chart-container-lg (height:320px)
        double canvasW = 520;
        double canvasH = 280;
        double padL    = 38;  // espace axe Y
        double padR    = 12;
        double padT    = 20;
        double padB    = 70;  // espace labels X (maxRotation:35)
        double plotW   = canvasW - padL - padR;
        double plotH   = canvasH - padT - padB;

        int maxVal = nonZero.stream().mapToInt(s -> s.recommendations).max().orElse(1);

        Canvas canvas = new Canvas(canvasW, canvasH);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // ── Grille horizontale (grid:{color:'#f1f5f9'}) ──
        gc.setStroke(Color.web("#f1f5f9"));
        gc.setLineWidth(1);
        int steps = Math.min(maxVal, 6);
        for (int s = 0; s <= steps; s++) {
            double y = padT + plotH - (double) s / steps * plotH;
            gc.strokeLine(padL, y, padL + plotW, y);

            // Valeurs axe Y (ticks:{stepSize:1, font:{size:11}})
            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(Font.font("Segoe UI", 10));
            gc.setTextAlign(TextAlignment.RIGHT);
            int tickVal = (int) Math.round((double) s / steps * maxVal);
            gc.fillText(String.valueOf(tickVal), padL - 4, y + 4);
        }

        // ── Barres ──
        double barW    = Math.min(48, (plotW / nonZero.size()) - 6);
        double barStep = plotW / nonZero.size();
        double radius  = 6; // borderRadius:10 → ~6px réel

        for (int i = 0; i < nonZero.size(); i++) {
            FiliereStatEntry entry = nonZero.get(i);
            Color fillColor   = CHART_FILL  [i % CHART_FILL.length];
            Color borderColor = CHART_BORDER[i % CHART_BORDER.length];

            double barH = plotH * ((double) entry.recommendations / maxVal);
            double x    = padL + barStep * i + (barStep - barW) / 2.0;
            double y    = padT + plotH - barH;

            // Remplissage avec coins arrondis en haut (borderRadius:10, borderSkipped:false)
            gc.setFill(fillColor);
            drawRoundedTop(gc, x, y, barW, barH, radius);

            // Bordure (borderWidth:2)
            gc.setStroke(borderColor);
            gc.setLineWidth(1.5);
            drawRoundedTopStroke(gc, x, y, barW, barH, radius);

            // Label valeur au-dessus de la barre
            int pct = total > 0 ? entry.recommendations * 100 / total : 0;
            gc.setFill(Color.web("#1e293b"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(entry.recommendations), x + barW / 2, y - 8);
            gc.setFill(Color.web("#64748b"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.fillText(pct + "%", x + barW / 2, y - 18);

            // Label axe X tronqué 16 chars — ticks callback Symfony
            String nom = entry.filiere.getNom();
            String icon = (entry.filiere.getIcon() != null && !entry.filiere.getIcon().isBlank())
                          ? entry.filiere.getIcon() + " " : "";
            String shortNom = nom.length() > 16 ? nom.substring(0, 16) + "…" : nom;
            gc.setFill(Color.web("#64748b"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.setTextAlign(TextAlignment.CENTER);

            // Rotation simulée : on découpe le label en 2 lignes si trop long
            gc.save();
            double labelX = x + barW / 2;
            double labelY = padT + plotH + 14;
            gc.translate(labelX, labelY);
            // maxRotation:35 → on écrit à 35°
            gc.rotate(35);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(icon + shortNom, 0, 0);
            gc.restore();
        }

        // ── Ligne de base axe X ──
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + plotH, padL + plotW, padT + plotH);

        // ── Axe Y vertical ──
        gc.strokeLine(padL, padT, padL, padT + plotH);

        // Conteneur responsive
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setMaxWidth(Double.MAX_VALUE);
        canvasPane.setAlignment(Pos.CENTER_LEFT);
        vboxBarChart.getChildren().add(canvasPane);
    }

    /** Remplit un rectangle avec coins arrondis en haut uniquement (comme CSS border-radius:6 6 0 0) */
    private void drawRoundedTop(GraphicsContext gc, double x, double y, double w, double h, double r) {
        r = Math.min(r, w / 2);
        r = Math.min(r, h);
        gc.beginPath();
        gc.moveTo(x + r, y);
        gc.lineTo(x + w - r, y);
        gc.arcTo(x + w, y, x + w, y + r, r);
        gc.lineTo(x + w, y + h);
        gc.lineTo(x, y + h);
        gc.lineTo(x, y + r);
        gc.arcTo(x, y, x + r, y, r);
        gc.closePath();
        gc.fill();
    }

    private void drawRoundedTopStroke(GraphicsContext gc, double x, double y, double w, double h, double r) {
        r = Math.min(r, w / 2);
        r = Math.min(r, h);
        gc.beginPath();
        gc.moveTo(x + r, y);
        gc.lineTo(x + w - r, y);
        gc.arcTo(x + w, y, x + w, y + r, r);
        gc.lineTo(x + w, y + h);
        gc.lineTo(x, y + h);
        gc.lineTo(x, y + r);
        gc.arcTo(x, y, x + r, y, r);
        gc.closePath();
        gc.stroke();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Donut Chart — Canvas JavaFX (identique Chart.js "doughnut" Symfony)
    //  - cutout:'62%'   → trou central = 62 % du rayon
    //  - hoverOffset:12 → (statique en JavaFX)
    //  - legend : position:'bottom', font:{size:10}, padding:10, boxWidth:14
    //  - tooltip format : "${c.label} : ${c.parsed} (${round*100}%)"
    //  - centerText plugin : total + "quiz" au centre (blanc sur gradient)
    // ═════════════════════════════════════════════════════════════════════

    private void buildDonutChart(LinkedHashMap<String, Integer> stats, int total) {
        if (vboxDonutChart == null || stats.isEmpty()) return;
        vboxDonutChart.getChildren().clear();

        // Dimensions — chart-container-donut : height:300px, max-width:380px
        double size    = 240;     // diamètre du canvas
        double cx      = size / 2;
        double cy      = size / 2;
        double outerR  = size / 2 - 8;       // rayon extérieur
        double innerR  = outerR * 0.62;       // cutout:'62%'

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        List<Map.Entry<String,Integer>> entries = new ArrayList<>(stats.entrySet());

        // ── Segments du donut ──
        double startAngle = -Math.PI / 2;  // début en haut (comme Chart.js)
        int colorIdx = 0;
        for (Map.Entry<String, Integer> e : entries) {
            double sweep = total > 0 ? (double) e.getValue() / total * 2 * Math.PI : 0;

            // Remplissage du secteur (arc extérieur + arc intérieur)
            gc.setFill(CHART_FILL[colorIdx % CHART_FILL.length]);
            drawArcSegment(gc, cx, cy, innerR, outerR, startAngle, sweep, true);

            // Bordure (borderWidth:2, borderColor)
            gc.setStroke(CHART_BORDER[colorIdx % CHART_BORDER.length]);
            gc.setLineWidth(2);
            drawArcSegment(gc, cx, cy, innerR, outerR, startAngle, sweep, false);

            startAngle += sweep;
            colorIdx++;
        }

        // ── Cercle intérieur blanc (cutout) — efface le centre ──
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        // ── Texte central (centerText plugin Symfony) : total + "quiz" ──
        gc.setFill(Color.web("#1e293b"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.valueOf(total), cx, cy - 4);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        gc.setFill(Color.web("#64748b"));
        gc.fillText("quiz", cx, cy + 16);

        // ── Légende en bas (position:'bottom', font:{size:10}, boxWidth:14) ──
        VBox legend = new VBox(5);
        legend.setStyle("-fx-padding:10 0 0 0;");
        legend.setMaxWidth(Double.MAX_VALUE);
        colorIdx = 0;
        for (Map.Entry<String, Integer> e : entries) {
            int pctInt = total > 0 ? e.getValue() * 100 / total : 0;
            String hexColor = CHART_HEX[colorIdx % CHART_HEX.length];

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            // boxWidth:14 → carré de couleur 14×14
            Region dot = new Region();
            dot.setPrefWidth(14); dot.setPrefHeight(14);
            dot.setMinWidth(14);  dot.setMinHeight(14);
            dot.setStyle("-fx-background-color:" + hexColor + "; -fx-background-radius:3px;");

            // label profil (font:{size:10})
            Label profLbl = new Label(e.getKey());
            profLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#475569; -fx-font-weight:600;");
            HBox.setHgrow(profLbl, Priority.ALWAYS);

            // tooltip format : "X (Y%)"
            Label cntLbl = new Label(e.getValue() + " (" + pctInt + "%)");
            cntLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8; -fx-font-weight:700;");

            row.getChildren().addAll(dot, profLbl, cntLbl);
            legend.getChildren().add(row);
            colorIdx++;
        }

        // Centrage du canvas
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setAlignment(Pos.CENTER);
        canvasPane.setMaxWidth(Double.MAX_VALUE);

        vboxDonutChart.getChildren().addAll(canvasPane, legend);
    }

    /**
     * Dessine un segment de couronne circulaire (arc de donut).
     * @param fill true = gc.fill(), false = gc.stroke()
     */
    private void drawArcSegment(GraphicsContext gc,
                                 double cx, double cy,
                                 double innerR, double outerR,
                                 double startAngle, double sweep,
                                 boolean fill) {
        if (sweep <= 0) return;

        // Point de départ sur l'arc extérieur
        double x1 = cx + outerR * Math.cos(startAngle);
        double y1 = cy + outerR * Math.sin(startAngle);
        // Point de fin sur l'arc extérieur
        double endAngle = startAngle + sweep;
        double x2 = cx + outerR * Math.cos(endAngle);
        double y2 = cy + outerR * Math.sin(endAngle);
        // Point de départ sur l'arc intérieur (sens inverse)
        double x3 = cx + innerR * Math.cos(endAngle);
        double y3 = cy + innerR * Math.sin(endAngle);
        // Point de fin sur l'arc intérieur
        double x4 = cx + innerR * Math.cos(startAngle);
        double y4 = cy + innerR * Math.sin(startAngle);

        gc.beginPath();
        gc.moveTo(x1, y1);

        // Arc extérieur (sens trigonométrique)
        arcPath(gc, cx, cy, outerR, startAngle, sweep);

        // Ligne vers l'arc intérieur
        gc.lineTo(x3, y3);

        // Arc intérieur (sens inverse)
        arcPath(gc, cx, cy, innerR, endAngle, -sweep);

        gc.lineTo(x4, y4);
        gc.closePath();

        if (fill) gc.fill();
        else gc.stroke();
    }

    /**
     * Ajoute des points d'un arc circulaire au path courant.
     * Découpe en petits segments pour une courbe lisse.
     */
    private void arcPath(GraphicsContext gc,
                          double cx, double cy, double r,
                          double startAngle, double sweep) {
        int segments = Math.max(12, (int) Math.abs(sweep * 20));
        double step  = sweep / segments;
        for (int i = 1; i <= segments; i++) {
            double a = startAngle + step * i;
            gc.lineTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Utilitaire
    // ═════════════════════════════════════════════════════════════════════

    private void setLabel(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }
}
