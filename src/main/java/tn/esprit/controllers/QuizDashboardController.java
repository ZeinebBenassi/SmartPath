package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.services.FiliereService;
import tn.esprit.services.QuestionService;
import tn.esprit.services.QuizHistoriqueService;
import tn.esprit.services.QuizStatisticsService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class QuizDashboardController {

    @FXML private Label lblTotalQuestions;
    @FXML private Label lblTotalFilieres;
    @FXML private Label lblTotalQuizPasses;
    @FXML private Label lblMoyenneJour;
    @FXML private VBox  vboxTopFilieres;

    private final QuestionService       questionService  = new QuestionService();
    private final FiliereService        filiereService   = new FiliereService();
    private final QuizHistoriqueService historiqueService = new QuizHistoriqueService();

    // Référence au DashboardAdmin pour naviguer dans le contentArea
    private DashboardAdminController dashboardCtrl;

    public void setDashboardController(DashboardAdminController ctrl) {
        this.dashboardCtrl = ctrl;
    }

    @FXML
    public void initialize() {
        loadStats();
        loadTopFilieres();
    }

    // ── Chargement des stats ───────────────────────────────────────────────

    private void loadStats() {
        try {
            int totalQ = questionService.afficher().size();
            lblTotalQuestions.setText(String.valueOf(totalQ));
        } catch (Exception e) { lblTotalQuestions.setText("0"); }

        try {
            int totalF = filiereService.afficher().size();
            lblTotalFilieres.setText(String.valueOf(totalF));
        } catch (Exception e) { lblTotalFilieres.setText("0"); }

        try {
            int totalQP = historiqueService.findAllOrderedByDate().size();
            lblTotalQuizPasses.setText(String.valueOf(totalQP));

            // Moyenne par jour (nb quiz / nb jours distincts)
            if (totalQP > 0) {
                double moyenne = computeMoyenneParJour();
                lblMoyenneJour.setText(String.format("%.1f", moyenne));
            } else {
                lblMoyenneJour.setText("0.0");
            }
        } catch (Exception e) {
            lblTotalQuizPasses.setText("0");
            lblMoyenneJour.setText("0.0");
        }
    }

    private double computeMoyenneParJour() {
        try {
            var results = historiqueService.findAllOrderedByDate();
            if (results.isEmpty()) return 0;
            // Compter les jours distincts
            var joursDistincts = new java.util.HashSet<String>();
            var sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            for (var r : results) {
                if (r.getCreatedAt() != null)
                    joursDistincts.add(sdf.format(r.getCreatedAt()));
            }
            int nbJours = joursDistincts.isEmpty() ? 1 : joursDistincts.size();
            return (double) results.size() / nbJours;
        } catch (Exception e) { return 0; }
    }

    // ── Top 5 filières ─────────────────────────────────────────────────────

    private void loadTopFilieres() {
        vboxTopFilieres.getChildren().clear();
        try {
            // Récupérer les recommandations depuis l'historique
            var results = historiqueService.findAllOrderedByDate();
            Map<String, Integer> counts = new LinkedHashMap<>();

            for (var r : results) {
                String json = r.getRecommendations();
                if (json == null || json.isBlank() || json.equals("[]")) continue;
                
                // Parser le JSON pour extraire les noms de filières
                String[] objects = json.split("\\{");
                for (String obj : objects) {
                    if (!obj.contains("filiereNom")) continue;
                    String nom = extractJsonValue(obj, "filiereNom");
                    if (nom != null && !nom.isEmpty())
                        counts.merge(nom, 1, Integer::sum);
                }
            }

            // Trier par count décroissant et prendre les 5 premiers
            var top5 = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .toList();

            String[] medals = {"🥇", "🥈", "🥉", "4️⃣", "5️⃣"};
            String[] colors  = {"#f59e0b", "#94a3b8", "#cd7c3a", "#6366f1", "#10b981"};

            for (int i = 0; i < top5.size(); i++) {
                var entry = top5.get(i);
                HBox row  = buildTopRow(i + 1, medals[i], entry.getKey(), entry.getValue(), colors[i]);
                vboxTopFilieres.getChildren().add(row);
            }

            if (top5.isEmpty()) {
                Label empty = new Label("Aucune donnée disponible pour l'instant.");
                empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-padding:20 24;");
                vboxTopFilieres.getChildren().add(empty);
            }

        } catch (Exception e) {
            Label err = new Label("Impossible de charger les statistiques.");
            err.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px; -fx-padding:16 24;");
            vboxTopFilieres.getChildren().add(err);
        }
    }

    private HBox buildTopRow(int rang, String medal, String filiere, int count, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
            "-fx-padding:14 24;" +
            "-fx-border-color:transparent transparent #f1f5f9 transparent;" +
            "-fx-border-width:0 0 1 0;");

        // Médaille + rang
        Label lMedal = new Label(medal + "  " + rang);
        lMedal.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + color + "; -fx-min-width:60;");

        // Nom filière
        Label lNom = new Label(filiere);
        lNom.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        HBox.setHgrow(lNom, javafx.scene.layout.Priority.ALWAYS);
        lNom.setMaxWidth(Double.MAX_VALUE);

        // Badge count
        Label lCount = new Label(count + " recommandation" + (count > 1 ? "s" : ""));
        lCount.setStyle(
            "-fx-background-color:#ddd6fe; -fx-text-fill:#6d28d9;" +
            "-fx-background-radius:50px; -fx-padding:3 12;" +
            "-fx-font-size:11px; -fx-font-weight:bold;");

        row.getChildren().addAll(lMedal, lNom, lCount);
        return row;
    }

    private String extractJsonValue(String fragment, String key) {
        String search = "\"" + key + "\":";
        int idx = fragment.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < fragment.length() && fragment.charAt(start) == ' ') start++;
        if (start >= fragment.length()) return null;
        char first = fragment.charAt(start);
        if (first == '"') {
            int end = fragment.indexOf('"', start + 1);
            return end > 0 ? fragment.substring(start + 1, end) : null;
        } else {
            int end = start;
            while (end < fragment.length() && fragment.charAt(end) != ',' && fragment.charAt(end) != '}') end++;
            return fragment.substring(start, end).trim();
        }
    }

    // ── Actions boutons ────────────────────────────────────────────────────

    @FXML public void voirQuestions() {
        navigateDashboard("/tn/esprit/interfaces/QuestionContent.fxml", "Quiz - Questions du Quiz");
    }

    @FXML public void nouvelleQuestion() {
        openQuestionForm();
    }

    @FXML public void voirFilieres() {
        navigateDashboard("/tn/esprit/interfaces/FiliereContent.fxml", "Quiz - Filières");
    }

    @FXML public void nouvelleFiliere() {
        openFiliereForm();
    }

    @FXML public void voirStatistiques() {
        navigateDashboard("/tn/esprit/interfaces/QuizStatistiques.fxml", "Quiz - Statistiques");
    }

    @FXML public void voirHistorique() {
        navigateDashboard("/tn/esprit/interfaces/QuizHistorique.fxml", "Quiz - Historique");
    }

    // ── Navigation via le contentArea du Dashboard ────────────────────────

    private void navigateDashboard(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            // Remonter jusqu'au StackPane contentArea via la scène
            if (vboxTopFilieres.getScene() != null) {
                javafx.scene.Node root = vboxTopFilieres.getScene().getRoot();
                // Chercher le contentArea dans la scène
                javafx.scene.Node content = root.lookup("#contentArea");
                if (content instanceof javafx.scene.layout.StackPane sp) {
                    sp.getChildren().setAll(view);
                    // Mettre à jour le titre via lookup
                    javafx.scene.Node titleNode = root.lookup("#pageTitle");
                    if (titleNode instanceof Label lbl) lbl.setText(title);
                    return;
                }
            }
            // Fallback : remplacer la scène entière
            vboxTopFilieres.getScene().setRoot(
                FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"))
            );
        } catch (Exception e) {
            System.out.println("QuizDashboard nav " + fxml + " : " + e.getMessage());
        }
    }

    private void openQuestionForm() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/QuestionForm.fxml")));
            Parent root = loader.load();
            QuestionFormController ctrl = loader.getController();
            ctrl.initData(null, null);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (vboxTopFilieres != null && vboxTopFilieres.getScene() != null)
                stage.initOwner(vboxTopFilieres.getScene().getWindow());
            stage.setTitle("Nouvelle Question");
            stage.setScene(new Scene(root));
            stage.setWidth(650);
            stage.setHeight(750);
            stage.setMinWidth(600);
            stage.setMinHeight(600);
            stage.showAndWait();

            loadStats();
            loadTopFilieres();
        } catch (Exception e) {
            System.out.println("Ouverture QuestionForm : " + e.getMessage());
        }
    }

    private void openFiliereForm() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/FiliereForm.fxml")));
            Parent root = loader.load();
            FiliereFormController ctrl = loader.getController();
            ctrl.initData(null, null);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (vboxTopFilieres != null && vboxTopFilieres.getScene() != null)
                stage.initOwner(vboxTopFilieres.getScene().getWindow());
            stage.setTitle("Nouvelle Filière");
            stage.setScene(new Scene(root));
            stage.setWidth(920);
            stage.setHeight(780);
            stage.setMinWidth(820);
            stage.setMinHeight(650);
            stage.setResizable(true);
            stage.showAndWait();

            loadStats();
            loadTopFilieres();
        } catch (Exception e) {
            System.out.println("Ouverture FiliereForm : " + e.getMessage());
        }
    }
}
