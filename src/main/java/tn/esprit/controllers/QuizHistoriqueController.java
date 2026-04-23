package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entity.QuizResult;
import tn.esprit.services.QuizHistoriqueService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * QuizHistoriqueController — Backoffice admin : historique des résultats du quiz.
 *
 * Équivalent Java de AdminQuizController::results() + results.html.twig Symfony.
 * Fonctionnalités :
 *  - Tableau de tous les passages quiz (étudiant, profil, filières, date)
 *  - Stats rapides (total, identifiés, anonymes, profils distincts)
 *  - Recherche en temps réel sur profil
 *  - Suppression individuelle avec confirmation
 *  - Suppression totale de l'historique avec confirmation
 */
public class QuizHistoriqueController implements Initializable {

    // ── Stats ──
    @FXML private Label lblTotal;
    @FXML private Label lblIdentifies;
    @FXML private Label lblAnonymes;
    @FXML private Label lblProfilsDistincts;

    // ── Recherche ──
    @FXML private TextField txtSearch;
    @FXML private Label     lblCount;

    // ── TableView ──
    @FXML private TableView<QuizResult>          table;
    @FXML private TableColumn<QuizResult, String> colIndex;
    @FXML private TableColumn<QuizResult, String> colDate;
    @FXML private TableColumn<QuizResult, String> colEtudiant;
    @FXML private TableColumn<QuizResult, String> colProfil;
    @FXML private TableColumn<QuizResult, String> colFilieres;
    @FXML private TableColumn<QuizResult, String> colActions;

    // ── Bouton vider ──
    @FXML private Button btnVider;

    private final QuizHistoriqueService service = new QuizHistoriqueService();
    private ObservableList<QuizResult>  allResults;
    private FilteredList<QuizResult>    filteredResults;

    // Couleurs profil (identique à Symfony)
    private static final Map<String, String[]> PROFILE_STYLES = new LinkedHashMap<>() {{
        put("Data Analyst",           new String[]{"#dbeafe", "#1e40af"});
        put("Développeur Full Stack", new String[]{"#fef3c7", "#92400e"});
        put("UX/UI Designer",         new String[]{"#fce7f3", "#9d174d"});
        put("Ingénieur Système",      new String[]{"#d1fae5", "#065f46"});
        put("Data Scientist",         new String[]{"#ede9fe", "#5b21b6"});
        put("Expert IA/ML",           new String[]{"#fef9c3", "#713f12"});
        put("Administrateur Système", new String[]{"#dbeafe", "#1e40af"});
        put("Ingénieur Réseau",       new String[]{"#d1fae5", "#065f46"});
        put("Expert Cybersécurité",   new String[]{"#fee2e2", "#991b1b"});
        put("Ingénieur Big Data",     new String[]{"#ede9fe", "#5b21b6"});
    }};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadData();
        setupSearch();
    }

    // ------------------------------------------------------------------ //
    //  Chargement des données                                             //
    // ------------------------------------------------------------------ //

    private void loadData() {
        List<QuizResult> results = service.findAllOrderedByDate();
        allResults     = FXCollections.observableArrayList(results);
        filteredResults = new FilteredList<>(allResults, p -> true);
        table.setItems(filteredResults);
        updateStats(results);
        updateCount();
    }

    private void updateStats(List<QuizResult> results) {
        int total     = results.size();
        int identified = (int) results.stream().filter(r -> r.getEtudiantId() > 1).count();
        int anonymous  = total - identified;
        Set<String> profiles = new HashSet<>();
        results.forEach(r -> { if (r.getProfileType() != null) profiles.add(r.getProfileType()); });

        if (lblTotal           != null) lblTotal.setText(String.valueOf(total));
        if (lblIdentifies      != null) lblIdentifies.setText(String.valueOf(identified));
        if (lblAnonymes        != null) lblAnonymes.setText(String.valueOf(anonymous));
        if (lblProfilsDistincts!= null) lblProfilsDistincts.setText(String.valueOf(profiles.size()));
        if (btnVider           != null) btnVider.setDisable(total == 0);
    }

    private void updateCount() {
        int count = filteredResults.size();
        if (lblCount != null)
            lblCount.setText(count + " résultat" + (count > 1 ? "s" : ""));
    }

    // ------------------------------------------------------------------ //
    //  Configuration des colonnes                                         //
    // ------------------------------------------------------------------ //

    private void setupColumns() {
        // ── Index ──
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(String.valueOf(getIndex() + 1));
                lbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11px; -fx-font-weight:700;");
                setGraphic(lbl); setText(null);
            }
        });

        // ── Date ──
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                QuizResult r = (QuizResult) getTableRow().getItem();
                if (r.getCreatedAt() == null) { setGraphic(null); return; }
                SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm");
                VBox box = new VBox(2);
                Label date = new Label(sdf1.format(r.getCreatedAt()));
                date.setStyle("-fx-font-weight:700; -fx-text-fill:#1e293b; -fx-font-size:12px;");
                Label time = new Label(sdf2.format(r.getCreatedAt()));
                time.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:10px;");
                box.getChildren().addAll(date, time);
                setGraphic(box); setText(null);
            }
        });

        // ── Étudiant ──
        colEtudiant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                QuizResult r = (QuizResult) getTableRow().getItem();
                String nom = service.getNomEtudiant(r.getEtudiantId());
                if (nom != null && !nom.isEmpty()) {
                    Label lbl = new Label("👤 " + nom);
                    lbl.setStyle("-fx-font-weight:700; -fx-text-fill:#1e293b; -fx-font-size:12px;");
                    setGraphic(lbl);
                } else {
                    Label lbl = new Label("🕵️ Anonyme");
                    lbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11px; -fx-font-style:italic;");
                    setGraphic(lbl);
                }
                setText(null);
            }
        });

        // ── Profil ──
        colProfil.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                QuizResult r = (QuizResult) getTableRow().getItem();
                String profile = r.getProfileType() != null ? r.getProfileType() : "—";
                String[] style = PROFILE_STYLES.getOrDefault(profile, new String[]{"#dbeafe", "#1e40af"});
                Label badge = new Label(profile);
                badge.setStyle(
                    "-fx-background-color:" + style[0] + ";" +
                    "-fx-text-fill:" + style[1] + ";" +
                    "-fx-background-radius:50px;" +
                    "-fx-padding:3 10;" +
                    "-fx-font-size:11px;" +
                    "-fx-font-weight:700;"
                );
                setGraphic(badge); setText(null);
            }
        });

        // ── Filières recommandées ──
        colFilieres.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                QuizResult r = (QuizResult) getTableRow().getItem();
                VBox box = new VBox(3);
                List<String[]> recs = parseRecommendations(r.getRecommendations());
                if (recs.isEmpty()) {
                    Label lbl = new Label("—");
                    lbl.setStyle("-fx-text-fill:#94a3b8;");
                    box.getChildren().add(lbl);
                } else {
                    // Afficher max 3 filières comme dans Symfony (|slice(0,3))
                    int count = Math.min(3, recs.size());
                    for (int i = 0; i < count; i++) {
                        String[] rec = recs.get(i);
                        HBox row = new HBox(5);
                        row.setAlignment(Pos.CENTER_LEFT);
                        // Mini barre de score
                        StackPane barBg = new StackPane();
                        barBg.setStyle("-fx-background-color:#f1f5f9; -fx-background-radius:10;");
                        barBg.setPrefWidth(60); barBg.setPrefHeight(5);
                        Region fill = new Region();
                        int pct = 0;
                        try { pct = Integer.parseInt(rec[1]); } catch (Exception ignored) {}
                        fill.setPrefWidth(60.0 * pct / 100.0);
                        fill.setPrefHeight(5);
                        fill.setStyle("-fx-background-color:linear-gradient(to right,#6366f1,#0ea5e9); -fx-background-radius:10;");
                        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
                        barBg.getChildren().add(fill);
                        Label filiereLbl = new Label("🎓 " + rec[0]);
                        filiereLbl.setStyle(
                            "-fx-background-color:#d1fae5; -fx-text-fill:#065f46;" +
                            "-fx-background-radius:50px; -fx-padding:1 7;" +
                            "-fx-font-size:10px; -fx-font-weight:600;"
                        );
                        Label pctLbl = new Label(rec[1] + "%");
                        pctLbl.setStyle("-fx-text-fill:#3b82f6; -fx-font-weight:800; -fx-font-size:10px;");
                        row.getChildren().addAll(barBg, filiereLbl, pctLbl);
                        box.getChildren().add(row);
                    }
                    if (recs.size() > 3) {
                        Label more = new Label("+" + (recs.size() - 3) + " autres...");
                        more.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:10px;");
                        box.getChildren().add(more);
                    }
                }
                setGraphic(box); setText(null);
            }
        });

        // ── Actions (Voir + Supprimer) ──
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                QuizResult r = (QuizResult) getTableRow().getItem();

                Button btnVoir = new Button("👁 Voir");
                btnVoir.setStyle(
                    "-fx-background-color:#3b82f6; -fx-text-fill:white;" +
                    "-fx-background-radius:8; -fx-padding:4 10;" +
                    "-fx-font-size:11px; -fx-font-weight:700; -fx-cursor:hand;"
                );
                btnVoir.setOnAction(e -> showDetail(r));

                Button btnDel = new Button("🗑");
                btnDel.setStyle(
                    "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                    "-fx-background-radius:8; -fx-padding:4 9;" +
                    "-fx-font-size:11px; -fx-font-weight:700; -fx-cursor:hand;" +
                    "-fx-border-color:#fca5a5; -fx-border-radius:8; -fx-border-width:1;"
                );
                btnDel.setOnAction(e -> deleteOne(r));

                HBox box = new HBox(5, btnVoir, btnDel);
                box.setAlignment(Pos.CENTER);
                setGraphic(box); setText(null);
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Recherche en temps réel (identique au JS Symfony)                  //
    // ------------------------------------------------------------------ //

    private void setupSearch() {
        if (txtSearch == null) return;
        txtSearch.textProperty().addListener((obs, old, val) -> {
            String term = val.toLowerCase().trim();
            filteredResults.setPredicate(r -> {
                if (term.isEmpty()) return true;
                String profil = r.getProfileType() != null ? r.getProfileType().toLowerCase() : "";
                String nom    = service.getNomEtudiant(r.getEtudiantId());
                String nomStr = nom != null ? nom.toLowerCase() : "anonyme";
                return profil.contains(term) || nomStr.contains(term);
            });
            updateCount();
        });
    }

    // ------------------------------------------------------------------ //
    //  Suppression individuelle                                            //
    // ------------------------------------------------------------------ //

    private void deleteOne(QuizResult r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer ce résultat");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce résultat de quiz ? Cette action est irréversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = service.deleteById(r.getId());
                if (ok) {
                    allResults.remove(r);
                    updateStats(new ArrayList<>(allResults));
                    updateCount();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
                }
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Suppression totale (bouton "Vider l'historique")                   //
    // ------------------------------------------------------------------ //

    @FXML
    private void handleViderHistorique() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vider l'historique");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer TOUT l'historique du quiz ? Cette action est irréversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = service.deleteAll();
                if (ok) {
                    allResults.clear();
                    updateStats(new ArrayList<>());
                    updateCount();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
                }
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Voir le détail d'un résultat (popup)                               //
    // ------------------------------------------------------------------ //

    private void showDetail(QuizResult r) {
        Alert detail = new Alert(Alert.AlertType.INFORMATION);
        detail.setTitle("Détail du résultat #" + r.getId());
        detail.setHeaderText("Profil : " + r.getProfileType());

        StringBuilder sb = new StringBuilder();
        String nom = service.getNomEtudiant(r.getEtudiantId());
        sb.append("Étudiant : ").append(nom != null ? nom : "Anonyme").append("\n");
        if (r.getCreatedAt() != null)
            sb.append("Date     : ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getCreatedAt())).append("\n\n");

        sb.append("── Scores ──\n");
        List<String[]> scores = parseKeyValueJson(r.getScores());
        for (String[] s : scores)
            sb.append("  ").append(s[0]).append(" : ").append(s[1]).append("\n");

        sb.append("\n── Filières recommandées ──\n");
        List<String[]> recs = parseRecommendations(r.getRecommendations());
        if (recs.isEmpty()) sb.append("  Aucune\n");
        else for (String[] rec : recs)
            sb.append("  🎓 ").append(rec[0]).append(" — ").append(rec[1]).append("%\n");

        detail.setContentText(sb.toString());
        detail.getDialogPane().setPrefWidth(480);
        detail.showAndWait();
    }

    // ------------------------------------------------------------------ //
    //  Parsing JSON minimaliste                                            //
    // ------------------------------------------------------------------ //

    /**
     * Parse le JSON des recommandations stocké en BDD.
     * Format : [{"filiereNom":"X","percentage":70},...]
     * Retourne une liste de [nom, percentage].
     */
    private List<String[]> parseRecommendations(String json) {
        List<String[]> list = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) return list;
        // Cherche filiereNom et percentage dans chaque objet
        String[] objects = json.split("\\{");
        for (String obj : objects) {
            if (!obj.contains("filiereNom")) continue;
            String nom = extractJsonValue(obj, "filiereNom");
            String pct = extractJsonValue(obj, "percentage");
            if (nom == null || nom.isEmpty()) nom = extractJsonValue(obj, "filiere");
            if (nom != null && !nom.isEmpty())
                list.add(new String[]{ nom, pct != null ? pct : "0" });
        }
        return list;
    }

    /**
     * Parse le JSON des scores : {"analytique":10,"pratique":5,...}
     * Retourne une liste de [trait, valeur].
     */
    private List<String[]> parseKeyValueJson(String json) {
        List<String[]> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;
        json = json.replaceAll("[{}]", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String k = kv[0].trim().replaceAll("\"", "");
                String v = kv[1].trim().replaceAll("\"", "");
                list.add(new String[]{k, v});
            }
        }
        return list;
    }

    /** Extrait la valeur d'une clé dans un fragment JSON simple. */
    private String extractJsonValue(String fragment, String key) {
        String search = "\"" + key + "\":";
        int idx = fragment.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        // Sauter les espaces
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
}
