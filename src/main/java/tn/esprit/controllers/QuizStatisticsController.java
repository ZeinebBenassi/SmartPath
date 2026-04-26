package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import tn.esprit.services.QuizStatisticsService;
import tn.esprit.services.QuizStatisticsService.FiliereStatEntry;

import java.net.URL;
import java.util.*;

/**
 * QuizStatisticsController — IDENTIQUE à admin/quiz/statistics.html.twig Symfony.
 *
 * Variables reçues du service (équivalent Twig) :
 *   total_results  → int totalResults
 *   filiere_stats  → List<FiliereStatEntry>  (filiere objet complet + recommendations count)
 *   profile_stats  → LinkedHashMap<String,Integer> trié DESC
 *
 * Rendu :
 *  1. Banner gradient   : total_results, filiere_stats|length, profile_stats|length
 *  2. 4 Insight cards   : top filière (filiere.nom), profil dominant, nb filières, nb profils
 *  3. Liste filières    : rang (🥇🥈🥉), filiere.icon ?? "🎓", filiere.nom, barre, count badge
 *  4. Liste profils     : badge coloré, barre, count "(Y%)"
 *  5. Bar chart         : filière.nom (tronqué 16), count, couleurs CHART_COLORS
 *  6. Donut chart       : barre empilée + légende "X (Y%)"
 */
public class QuizStatisticsController implements Initializable {

    // ── Banner ──
    @FXML private Label lblBannerTotal;
    @FXML private Label lblBannerMiniStats;

    // ── Insight cards dans le banner (mini-stats droite) ──
    @FXML private Label lblInsightFilieres;   // dans le banner
    @FXML private Label lblInsightProfils;    // dans le banner

    // ── 4 Insight cards dédiées ──
    @FXML private Label lblTopFiliereName;
    @FXML private Label lblTopFiliereCount;
    @FXML private Label lblTopProfilName;
    @FXML private Label lblTopProfilCount;
    @FXML private Label lblInsightFilieres2;   // card "Filières analysées"
    @FXML private Label lblInsightProfils2;    // card "Profils distincts"
    @FXML private Label lblInsightProfilsSub;  // "sur X quiz"

    // ── Liste filières ──
    @FXML private VBox  vboxFilieres;
    @FXML private Label lblBadgeFilieres;

    // ── Liste profils ──
    @FXML private VBox  vboxProfils;
    @FXML private Label lblBadgeProfils;

    // ── Charts ──
    @FXML private VBox  vboxBarChart;
    @FXML private VBox  vboxDonutChart;

    // ── Empty / Content ──
    @FXML private VBox  vboxEmpty;
    @FXML private VBox  vboxContent;

    private final QuizStatisticsService service = new QuizStatisticsService();

    // Palette identique Chart.js Symfony (backgroundColors / borderColors)
    private static final String[] CHART_COLORS = {
        "#6366f1", "#0ea5e9", "#10b981", "#f59e0b",
        "#ef4444", "#a855f7", "#ec4899", "#14b8a6",
        "#f97316", "#84cc16", "#06b6d4", "#fbbf24"
    };

    // Badges profils — couleurs identiques Symfony .profile-* CSS classes
    private static final Map<String, String[]> PROFILE_BADGE = new LinkedHashMap<>() {{
        put("Data Analyst",                new String[]{"#dbeafe", "#1e40af"});
        put("Développeur Full Stack",      new String[]{"#fef3c7", "#92400e"});
        put("UX/UI Designer",              new String[]{"#fce7f3", "#9d174d"});
        put("Ingénieur Système",           new String[]{"#d1fae5", "#065f46"});
        put("Data Scientist",              new String[]{"#ede9fe", "#5b21b6"});
        put("Expert IA/ML",                new String[]{"#fef9c3", "#713f12"});
        put("Administrateur Système",      new String[]{"#dbeafe", "#1e40af"});
        put("Ingénieur Réseau",            new String[]{"#d1fae5", "#065f46"});
        put("Expert Cybersécurité",        new String[]{"#fee2e2", "#991b1b"});
        put("Ingénieur Big Data",          new String[]{"#ede9fe", "#5b21b6"});
        put("Informaticien Polyvalent",    new String[]{"#f0fdf4", "#166534"});
    }};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStatistics();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  loadStatistics() — équivalent AdminQuizController::statistics()
    //  Symfony passe exactement 3 variables au template :
    //    total_results, filiere_stats, profile_stats
    // ─────────────────────────────────────────────────────────────────────

    private void loadStatistics() {
        int                           totalResults = service.getTotalResults();
        List<FiliereStatEntry>        filiereStats = service.getFiliereStats();
        LinkedHashMap<String,Integer> profileStats = service.getProfileStats();

        // ── Banner ──
        setLabel(lblBannerTotal,     String.valueOf(totalResults));
        setLabel(lblBannerMiniStats, filiereStats.size() + " filières · " + profileStats.size() + " profils distincts");

        // Mini-stats droite du banner (partagées avec insight cards)
        setLabel(lblInsightFilieres, String.valueOf(filiereStats.size()));
        setLabel(lblInsightProfils,  String.valueOf(profileStats.size()));

        // ── État vide (identique {% if total_results == 0 %} Symfony) ──
        boolean empty = (totalResults == 0);
        if (vboxEmpty   != null) { vboxEmpty.setVisible(empty);   vboxEmpty.setManaged(empty);   }
        if (vboxContent != null) { vboxContent.setVisible(!empty); vboxContent.setManaged(!empty); }
        if (empty) return;

        // ── Insight card "Filière #1" ──
        // Symfony : filiere_stats[0].filiere.nom, filiere_stats[0].recommendations
        String topFiliereName  = filiereStats.isEmpty() ? "—" : filiereStats.get(0).filiere.getNom();
        int    topFiliereCount = filiereStats.isEmpty() ? 0   : filiereStats.get(0).recommendations;
        setLabel(lblTopFiliereName,  topFiliereName);
        setLabel(lblTopFiliereCount, topFiliereCount + " recommandation" + (topFiliereCount > 1 ? "s" : ""));

        // ── Insight card "Profil dominant" ──
        // Symfony : profile_stats|keys|first, profile_stats|first
        String topProfilName  = profileStats.isEmpty() ? "—" : profileStats.keySet().iterator().next();
        int    topProfilCount = profileStats.isEmpty() ? 0   : profileStats.values().iterator().next();
        setLabel(lblTopProfilName,  topProfilName);
        setLabel(lblTopProfilCount, topProfilCount + " étudiant" + (topProfilCount > 1 ? "s" : ""));

        // ── Insight cards compteurs ──
        setLabel(lblInsightFilieres2,  String.valueOf(filiereStats.size()));
        setLabel(lblInsightProfils2,   String.valueOf(profileStats.size()));
        setLabel(lblInsightProfilsSub, "sur " + totalResults + " quiz");

        // ── Badges header ──
        setLabel(lblBadgeFilieres, filiereStats.size() + " filière" + (filiereStats.size() > 1 ? "s" : ""));
        setLabel(lblBadgeProfils,  profileStats.size() + " profil"  + (profileStats.size()  > 1 ? "s" : ""));

        // ── Listes ──
        buildFiliereList(filiereStats);
        buildProfilList(profileStats, totalResults);

        // ── Charts ──
        buildBarChart(filiereStats, totalResults);
        buildDonutChart(profileStats, totalResults);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Liste filières
    //  Symfony Twig : {% for stat in filiere_stats %}
    //    loop.index, stat.filiere.icon ?? '🎓', stat.filiere.nom, stat.recommendations
    // ─────────────────────────────────────────────────────────────────────

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
        if (maxRec == 0) maxRec = 1;

        for (int i = 0; i < stats.size(); i++) {
            vboxFilieres.getChildren().add(buildFiliereRow(i + 1, stats.get(i), maxRec));
        }
    }

    /**
     * Ligne filière — identique Symfony :
     *   loop.index → 1=🥇, 2=🥈, 3=🥉, n=chiffre
     *   stat.filiere.icon ?? '🎓'  stat.filiere.nom
     *   barre : stat.recommendations / max * 100%
     *   badge : stat.recommendations
     */
    private HBox buildFiliereRow(int rank, FiliereStatEntry entry, int maxRec) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:10 0; -fx-border-color:transparent transparent #f1f5f9 transparent; -fx-border-width:0 0 1 0;");

        // Rang
        String rankText = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
        String rankBg   = rank == 1 ? "#fef3c7" : rank == 2 ? "#f1f5f9" : rank == 3 ? "#fdecea" : "#f1f5f9";
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

        // Info : icône + nom + barre
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        // stat.filiere.icon ?? '🎓'  stat.filiere.nom
        String icon = (entry.filiere.getIcon() != null && !entry.filiere.getIcon().isBlank())
                      ? entry.filiere.getIcon() : "🎓";
        Label nomLbl = new Label(icon + "  " + entry.filiere.getNom());
        nomLbl.setStyle("-fx-font-weight:600; -fx-text-fill:#1e293b; -fx-font-size:12px;");
        nomLbl.setMaxWidth(Double.MAX_VALUE);

        // Barre : (stat.recommendations / max_recs) * 100%
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

        // Badge count — identique .filiere-stat-count Symfony
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

    // ─────────────────────────────────────────────────────────────────────
    //  Liste profils
    //  Symfony Twig : {% for profile, count in profile_stats %}
    //    badge coloré, barre, "count (pct%)"
    // ─────────────────────────────────────────────────────────────────────

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

        String[] badge = PROFILE_BADGE.getOrDefault(profil, new String[]{"#dbeafe", "#1e40af"});

        // Badge profil — identique Symfony .profile-X CSS
        Label nomLbl = new Label(profil);
        nomLbl.setStyle(
            "-fx-background-color:" + badge[0] + ";" +
            "-fx-text-fill:" + badge[1] + ";" +
            "-fx-background-radius:50px; -fx-padding:3 10;" +
            "-fx-font-size:11px; -fx-font-weight:700;" +
            "-fx-min-width:150px;"
        );

        // Barre
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

        // Count "(pct%)" — identique Symfony tooltip : "${c.label} : ${c.parsed} (${round}%)"
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

    // ─────────────────────────────────────────────────────────────────────
    //  Bar Chart filières — équivalent Chart.js "bar" Symfony
    //  Labels  : filiere.nom (tronqué 16 chars comme JS .slice(0,16))
    //  Data    : stat.recommendations
    //  Tooltip : "${c.parsed.y} rec. — ${Math.round(pct*100)}%"
    // ─────────────────────────────────────────────────────────────────────

    private void buildBarChart(List<FiliereStatEntry> stats, int total) {
        if (vboxBarChart == null) return;
        vboxBarChart.getChildren().clear();

        // On affiche seulement les filières avec > 0 recommandations (comme Symfony)
        List<FiliereStatEntry> nonZero = stats.stream()
            .filter(s -> s.recommendations > 0)
            .toList();

        if (nonZero.isEmpty()) {
            Label lbl = new Label("Aucune donnée à afficher");
            lbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-padding:30 0;");
            vboxBarChart.getChildren().add(lbl);
            return;
        }

        int    maxVal  = nonZero.stream().mapToInt(s -> s.recommendations).max().orElse(1);
        double maxBarH = 200.0;

        // Axe Y (valeurs max → 0)
        VBox chartArea = new VBox(0);
        chartArea.setMaxWidth(Double.MAX_VALUE);

        // Zone barres + axe Y
        HBox withAxis = new HBox(0);
        withAxis.setAlignment(Pos.BOTTOM_LEFT);
        withAxis.setMaxWidth(Double.MAX_VALUE);

        // Axe Y labels (identique chart.js yAxis)
        VBox yAxis = new VBox();
        yAxis.setAlignment(Pos.TOP_RIGHT);
        yAxis.setPrefWidth(30);
        yAxis.setPrefHeight(maxBarH);
        for (int v = maxVal; v >= 0; v -= Math.max(1, maxVal / 4)) {
            Label yLbl = new Label(String.valueOf(v));
            yLbl.setStyle("-fx-font-size:9px; -fx-text-fill:#94a3b8;");
            yLbl.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(yLbl, Priority.ALWAYS);
            yAxis.getChildren().add(yLbl);
        }

        // Zone barres
        HBox barsBox = new HBox(8);
        barsBox.setAlignment(Pos.BOTTOM_LEFT);
        barsBox.setPrefHeight(maxBarH + 50);

        int colorIdx = 0;
        for (FiliereStatEntry entry : nonZero) {
            double h   = maxVal > 0 ? (double) entry.recommendations / maxVal * maxBarH : 0;
            String clr = CHART_COLORS[colorIdx % CHART_COLORS.length];
            int    pct = total > 0 ? entry.recommendations * 100 / total : 0;

            // Nom tronqué comme JS : l.length > 16 ? l.slice(0,16)+'…' : l
            String nom = entry.filiere.getNom();
            String shortNom = nom.length() > 16 ? nom.substring(0, 16) + "…" : nom;

            VBox barCol = new VBox(4);
            barCol.setAlignment(Pos.BOTTOM_CENTER);
            barCol.setPrefWidth(54);

            // Valeur + % au-dessus (tooltip Symfony : "X rec. — Y%")
            Label recLbl = new Label(entry.recommendations + " rec.");
            recLbl.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#1e293b;");

            Label pctLbl = new Label(pct + "%");
            pctLbl.setStyle("-fx-font-size:9px; -fx-text-fill:#64748b;");

            // Barre (borderRadius:10, borderSkipped:false → radius sur tous les côtés)
            Region bar = new Region();
            bar.setPrefWidth(40);
            bar.setPrefHeight(Math.max(4, h));
            bar.setStyle("-fx-background-color:" + clr + "; -fx-background-radius:6 6 0 0;");

            // Label axe X : icon + nom tronqué
            String icon = (entry.filiere.getIcon() != null && !entry.filiere.getIcon().isBlank())
                          ? entry.filiere.getIcon() + " " : "";
            Label xLbl = new Label(icon + shortNom);
            xLbl.setStyle("-fx-font-size:9px; -fx-text-fill:#64748b; -fx-wrap-text:true;");
            xLbl.setWrapText(true);
            xLbl.setMaxWidth(54);
            xLbl.setAlignment(Pos.TOP_CENTER);

            barCol.getChildren().addAll(pctLbl, recLbl, bar, xLbl);
            barsBox.getChildren().add(barCol);
            colorIdx++;
        }

        withAxis.getChildren().addAll(yAxis, barsBox);

        // Ligne de base (axe X)
        Region baseline = new Region();
        baseline.setPrefHeight(1);
        baseline.setMaxWidth(Double.MAX_VALUE);
        baseline.setStyle("-fx-background-color:#e2e8f0;");

        chartArea.getChildren().addAll(withAxis, baseline);
        vboxBarChart.getChildren().add(chartArea);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Donut Chart profils — équivalent Chart.js "doughnut" Symfony
    //  cutout:'62%' → barre empilée + total au centre
    //  Tooltip : "${c.label} : ${c.parsed} (${round*100}%)"
    //  position:'bottom' → légende en bas
    // ─────────────────────────────────────────────────────────────────────

    private void buildDonutChart(LinkedHashMap<String, Integer> stats, int total) {
        if (vboxDonutChart == null || stats.isEmpty()) return;
        vboxDonutChart.getChildren().clear();

        // Centre donut — total au milieu (identique Symfony centerText plugin)
        Label centerLbl = new Label(total + "\nquiz");
        centerLbl.setStyle(
            "-fx-font-size:24px; -fx-font-weight:900; -fx-text-fill:#1e293b;" +
            "-fx-alignment:center; -fx-text-alignment:center;"
        );
        centerLbl.setWrapText(true);
        centerLbl.setMaxWidth(Double.MAX_VALUE);

        // Barre empilée (donut aplati — équivalent des segments du doughnut)
        HBox stackBar = new HBox(0);
        stackBar.setPrefHeight(24);
        stackBar.setMaxWidth(Double.MAX_VALUE);

        List<Map.Entry<String,Integer>> entries = new ArrayList<>(stats.entrySet());
        int colorIdx = 0;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String,Integer> e = entries.get(i);
            double segPct = total > 0 ? (double) e.getValue() / total : 0;
            String color  = CHART_COLORS[colorIdx % CHART_COLORS.length];
            // Coins arrondis aux extrémités seulement
            String radius = i == 0
                ? (entries.size() == 1 ? "50px" : "50px 0 0 50px")
                : (i == entries.size()-1 ? "0 50px 50px 0" : "0");

            Region seg = new Region();
            HBox.setHgrow(seg, Priority.ALWAYS);
            seg.setPrefHeight(24);
            seg.maxWidthProperty().bind(stackBar.widthProperty().multiply(segPct));
            seg.setStyle("-fx-background-color:" + color + "; -fx-background-radius:" + radius + ";");
            stackBar.getChildren().add(seg);
            colorIdx++;
        }

        // Légende — position:'bottom' (identique Symfony legend.position:'bottom')
        // Format tooltip Symfony : "${c.label} : ${c.parsed} (${Math.round(pct*100)}%)"
        VBox legend = new VBox(6);
        legend.setStyle("-fx-padding:14 0 0 0;");
        colorIdx = 0;
        for (Map.Entry<String,Integer> e : stats.entrySet()) {
            int    pctInt = total > 0 ? e.getValue() * 100 / total : 0;
            String color  = CHART_COLORS[colorIdx % CHART_COLORS.length];

            HBox legendRow = new HBox(8);
            legendRow.setAlignment(Pos.CENTER_LEFT);

            // Point couleur
            Region dot = new Region();
            dot.setPrefWidth(12); dot.setPrefHeight(12);
            dot.setStyle("-fx-background-color:" + color + "; -fx-background-radius:3px;");

            Label profLbl = new Label(e.getKey());
            profLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#475569; -fx-font-weight:600;");
            HBox.setHgrow(profLbl, Priority.ALWAYS);

            // Tooltip format : "X (Y%)"
            Label cntLbl = new Label(e.getValue() + " (" + pctInt + "%)");
            cntLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8; -fx-font-weight:700;");

            legendRow.getChildren().addAll(dot, profLbl, cntLbl);
            legend.getChildren().add(legendRow);
            colorIdx++;
        }

        vboxDonutChart.getChildren().addAll(centerLbl, stackBar, legend);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Utilitaire
    // ─────────────────────────────────────────────────────────────────────

    private void setLabel(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }
}
