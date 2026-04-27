package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import tn.esprit.services.UserStatsDAO;

import java.util.Map;

/**
 * Contrôleur JavaFX — Statistiques des utilisateurs.
 *
 * Affiche :
 *   • Un LineChart  → évolution des inscriptions dans le temps
 *   • Un PieChart   → répartition par rôle (admin / prof / étudiant)
 *   • Des Labels    → chiffres clés (total, aujourd'hui)
 */
public class StatistiquesController {

    // ── Chiffres clés ────────────────────────────────────────────────────────
    @FXML private Label labelTotalUsers;
    @FXML private Label labelToday;
    @FXML private Label labelAdmins;
    @FXML private Label labelProfs;
    @FXML private Label labelEtudiants;

    // ── LineChart ─────────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number>  lineChart;
    @FXML private CategoryAxis               xAxis;
    @FXML private NumberAxis                 yAxis;

    // ── PieChart ──────────────────────────────────────────────────────────────
    @FXML private PieChart pieChart;

    // ── Toggle Jour / Mois ───────────────────────────────────────────────────
    @FXML private ToggleButton toggleJour;
    @FXML private ToggleButton toggleMois;

    // ── DAO ───────────────────────────────────────────────────────────────────
    private final UserStatsDAO statsDAO = new UserStatsDAO();

    // ─────────────────────────────────────────────────────────────────────────
    // Initialisation automatique par JavaFX
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // 1. Chiffres clés
        chargerChifflesCles();

        // 2. PieChart (répartition par rôle)
        chargerPieChart();

        // 3. LineChart — vue par mois par défaut
        chargerLineChartMois();

        // 4. Gestion des toggles
        if (toggleMois != null) toggleMois.setSelected(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chiffres clés
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerChifflesCles() {
        int total = statsDAO.getTotalUsers();
        int today = statsDAO.getRegistrationsToday();
        Map<String, Integer> parRole = statsDAO.getUsersByRole();

        if (labelTotalUsers != null) labelTotalUsers.setText(String.valueOf(total));
        if (labelToday      != null) labelToday.setText(String.valueOf(today));
        if (labelAdmins     != null) labelAdmins.setText(String.valueOf(parRole.getOrDefault("admin",    0)));
        if (labelProfs      != null) labelProfs.setText(String.valueOf(parRole.getOrDefault("prof",      0)));
        if (labelEtudiants  != null) labelEtudiants.setText(String.valueOf(parRole.getOrDefault("etudiant", 0)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PieChart — Répartition par rôle
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerPieChart() {
        Map<String, Integer> parRole = statsDAO.getUsersByRole();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        parRole.forEach((role, count) -> {
            if (count > 0) {
                // Libellé lisible : "Admins (2)", "Profs (15)", ...
                String label = capitaliser(role) + "s (" + count + ")";
                data.add(new PieChart.Data(label, count));
            }
        });

        if (pieChart != null) {
            pieChart.setData(data);
            pieChart.setTitle("Répartition par rôle");
            pieChart.setLegendVisible(true);
            pieChart.setLabelsVisible(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LineChart — Inscriptions par MOIS
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerLineChartMois() {
        Map<String, Integer> parMois = statsDAO.getRegistrationsByMonth();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Inscriptions / mois");

        parMois.forEach((mois, count) ->
                series.getData().add(new XYChart.Data<>(mois, count)));

        mettreAJourLineChart(series, "Évolution mensuelle des inscriptions (12 derniers mois)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LineChart — Inscriptions par JOUR
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerLineChartJour() {
        Map<String, Integer> parJour = statsDAO.getRegistrationsByDay();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Inscriptions / jour");

        parJour.forEach((jour, count) ->
                series.getData().add(new XYChart.Data<>(jour, count)));

        mettreAJourLineChart(series, "Évolution journalière des inscriptions (30 derniers jours)");
    }

    private void mettreAJourLineChart(XYChart.Series<String, Number> series, String titre) {
        if (lineChart == null) return;
        lineChart.getData().clear();
        lineChart.getData().add(series);
        lineChart.setTitle(titre);
        if (xAxis != null) xAxis.setLabel("Période");
        if (yAxis != null) yAxis.setLabel("Nombre d'inscrits");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestion des boutons Toggle
    // ─────────────────────────────────────────────────────────────────────────

    /** Appelé par le ToggleButton "Par jour" */
    @FXML
    public void afficherParJour() {
        chargerLineChartJour();
        if (toggleMois != null) toggleMois.setSelected(false);
    }

    /** Appelé par le ToggleButton "Par mois" */
    @FXML
    public void afficherParMois() {
        chargerLineChartMois();
        if (toggleJour != null) toggleJour.setSelected(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaire
    // ─────────────────────────────────────────────────────────────────────────

    private String capitaliser(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
