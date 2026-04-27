package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import tn.esprit.entity.ReleveNotes;
import tn.esprit.entity.User;
import tn.esprit.services.ReleveAnalyserService;
import tn.esprit.services.ReleveNotesService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller JavaFX pour la fonctionnalité "Relevé de Notes".
 * Équivalent du ReleveController Symfony.
 *
 * Gère 3 vues intégrées dans un seul FXML :
 *   - Vue Upload    : sélection et envoi du fichier
 *   - Vue Résultat  : affichage de l'analyse IA
 *   - Vue Historique: liste des analyses précédentes
 */
public class ReleveController {

    // ── Conteneurs des 3 vues ───────────────────────────────────────────
    @FXML private ScrollPane uploadView;
    @FXML private ScrollPane resultView;
    @FXML private VBox       historiqueView;

    // ── Vue Upload ───────────────────────────────────────────────────────
    @FXML private Label  labelFichierChoisi;
    @FXML private Button btnChoisirFichier;
    @FXML private Button btnAnalyser;
    @FXML private HBox   loadingBox;
    @FXML private Label  labelLoading;

    // ── Vue Résultat ─────────────────────────────────────────────────────
    @FXML private Label resMoyenne;
    @FXML private Label resMention;
    @FXML private Label resFiliereReco;
    @FXML private Label resConseil;
    @FXML private VBox  resPointsForts;
    @FXML private VBox  resPointsFaibles;
    @FXML private VBox  resScoresFilieres;
    @FXML private TableView<NoteRow>          notesTable;
    @FXML private TableColumn<NoteRow,String> colMatiere;
    @FXML private TableColumn<NoteRow,String> colNote;
    @FXML private TableColumn<NoteRow,String> colCoeff;
    @FXML private TableColumn<NoteRow,String> colDomaine;
    @FXML private TableColumn<NoteRow,String> colNiveau;

    // ── Vue Historique ───────────────────────────────────────────────────
    @FXML private VBox historiqueListBox;

    // ── Navigation ───────────────────────────────────────────────────────
    @FXML private Button btnNavUpload;
    @FXML private Button btnNavHistorique;

    // ── État ─────────────────────────────────────────────────────────────
    private static User currentUser;
    private File selectedFile;

    private final ReleveNotesService    releveService   = new ReleveNotesService();
    private final ReleveAnalyserService analyserService = new ReleveAnalyserService();

    public static void setCurrentUser(User u) { currentUser = u; }

    // ════════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        if (colMatiere != null) colMatiere.setCellValueFactory(new PropertyValueFactory<>("matiere"));
        if (colNote    != null) colNote   .setCellValueFactory(new PropertyValueFactory<>("note"));
        if (colCoeff   != null) colCoeff  .setCellValueFactory(new PropertyValueFactory<>("coeff"));
        if (colDomaine != null) colDomaine.setCellValueFactory(new PropertyValueFactory<>("domaine"));
        if (colNiveau  != null) colNiveau .setCellValueFactory(new PropertyValueFactory<>("niveau"));
        showUploadView();
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════════════

    private void showUploadView() {
        setVis(uploadView, true);
        setVis(resultView, false);
        setVis(historiqueView, false);
        setNavActive(btnNavUpload);
    }

    private void showResultView() {
        setVis(uploadView, false);
        setVis(resultView, true);
        setVis(historiqueView, false);
    }

    private void showHistoriqueView() {
        setVis(uploadView, false);
        setVis(resultView, false);
        setVis(historiqueView, true);
        setNavActive(btnNavHistorique);
        chargerHistorique();
    }

    private void setVis(javafx.scene.Node n, boolean v) {
        if (n != null) { n.setVisible(v); n.setManaged(v); }
    }

    private void setNavActive(Button active) {
        String on  = "-fx-background-color:#eff6ff; -fx-text-fill:#2563eb; -fx-font-weight:bold; -fx-background-radius:8; -fx-border-color:#bfdbfe; -fx-border-radius:8; -fx-padding:8 20; -fx-cursor:hand;";
        String off = "-fx-background-color:transparent; -fx-text-fill:#5a6a8a; -fx-background-radius:8; -fx-padding:8 20; -fx-cursor:hand;";
        for (Button b : new Button[]{btnNavUpload, btnNavHistorique}) {
            if (b != null) b.setStyle(b == active ? on : off);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  ACTIONS FXML
    // ════════════════════════════════════════════════════════════════════

    @FXML
    public void choisirFichier() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir votre relevé de notes");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Relevé (PDF, Image)", "*.pdf","*.jpg","*.jpeg","*.png","*.webp"));
        File file = fc.showOpenDialog(btnChoisirFichier.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            if (labelFichierChoisi != null) labelFichierChoisi.setText("✅ " + file.getName());
            if (btnAnalyser != null) btnAnalyser.setDisable(false);
        }
    }

    @FXML
    public void lancerAnalyse() {
        if (selectedFile == null) { showAlert("Veuillez d'abord choisir un fichier."); return; }
        if (currentUser == null)  { showAlert("Erreur : aucun utilisateur connecté. Veuillez vous reconnecter."); return; }

        // ── Vérification de la taille du fichier (max 10 Mo) ──
        long tailleMax = 10L * 1024 * 1024; // 10 Mo
        if (selectedFile.length() > tailleMax) {
            showErrorAlert("❌ Fichier trop volumineux",
                "Le fichier dépasse la taille maximale autorisée (10 Mo).\n" +
                "Veuillez choisir un fichier plus léger.");
            return;
        }

        // ── Vérification de l'extension ──
        String nomFichier = selectedFile.getName().toLowerCase();
        boolean extensionValide = nomFichier.endsWith(".pdf") || nomFichier.endsWith(".jpg")
                || nomFichier.endsWith(".jpeg") || nomFichier.endsWith(".png")
                || nomFichier.endsWith(".webp");
        if (!extensionValide) {
            showErrorAlert("❌ Format non supporté",
                "Le fichier doit être au format PDF, JPG, PNG ou WEBP.\n" +
                "Veuillez choisir un fichier valide.");
            return;
        }

        if (loadingBox  != null) { loadingBox.setVisible(true);  loadingBox.setManaged(true);  }
        if (btnAnalyser != null)   btnAnalyser.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                String fileType = selectedFile.getName().toLowerCase().endsWith(".pdf") ? "pdf" : "image";
                Map<String, Object> analyse = analyserService.analyserFichier(selectedFile, fileType);

                ReleveNotes releve = new ReleveNotes();
                if (currentUser != null) releve.setEtudiantId(currentUser.getId());
                releve.setFichierPath(selectedFile.getAbsolutePath());
                releve.setFichierType(fileType);
                releve.setNotesDetectees(toJson(analyse.get("notesDetectees")));
                releve.setScoreParFiliere(toJson(analyse.get("scoreParFiliere")));
                releve.setFiliereRecommandee(str(analyse.get("filiereRecommandee")));
                releve.setAnalyseIA(toJson(analyse));
                releve.setMoyenneGenerale(toDouble(analyse.get("moyenneGenerale")));
                releveService.save(releve);

                Platform.runLater(() -> {
                    afficherResultats(analyse);
                    showResultView();
                    if (loadingBox  != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }
                    if (btnAnalyser != null)   btnAnalyser.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (loadingBox  != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }
                    if (btnAnalyser != null)   btnAnalyser.setDisable(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue.";
                    if (msg.contains("relevé de notes") || msg.contains("releve")) {
                        showErrorAlert("❌ Document invalide", msg);
                    } else {
                        showErrorAlert("⚠️ Erreur lors de l'analyse", msg);
                    }
                    e.printStackTrace();
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML public void goToUpload()     { showUploadView(); }
    @FXML public void goToHistorique() { showHistoriqueView(); }

    @FXML public void nouvelleAnalyse() {
        selectedFile = null;
        if (labelFichierChoisi != null) labelFichierChoisi.setText("Aucun fichier choisi");
        if (btnAnalyser != null) btnAnalyser.setDisable(true);
        showUploadView();
    }

    @FXML public void goBack() {
        try {
            DashboardEtudiantController.setCurrentUser(currentUser);
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            javafx.scene.Scene scene = uploadView != null ? uploadView.getScene() : null;
            if (scene == null && resultView != null) scene = resultView.getScene();
            if (scene == null && historiqueView != null) scene = historiqueView.getScene();
            if (scene != null) scene.setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════════════════
    //  AFFICHAGE DES RÉSULTATS
    // ════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void afficherResultats(Map<String, Object> analyse) {

        double moyenne = toDouble(analyse.get("moyenneGenerale"));
        if (resMoyenne != null) resMoyenne.setText(String.format("%.2f / 20", moyenne));

        String mention = mention(moyenne);
        if (resMention != null) {
            resMention.setText(mention);
            resMention.setStyle(resMention.getStyle() + "; -fx-text-fill:" + couleurMention(mention) + ";");
        }

        String filiereReco = str(analyse.get("filiereRecommandee"));
        if (resFiliereReco != null)
            resFiliereReco.setText(filiereReco.isBlank() ? "—" : "🏆 " + filiereReco);

        if (resConseil != null) resConseil.setText(str(analyse.get("conseil")));

        // Points forts
        if (resPointsForts != null) {
            resPointsForts.getChildren().clear();
            Label titre = new Label("💪 Points Forts");
            titre.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#059669;");
            resPointsForts.getChildren().add(titre);
            for (Object pt : (List<Object>) analyse.getOrDefault("pointsForts", List.of())) {
                Label l = new Label("✅  " + pt);
                l.setStyle("-fx-text-fill:#065f46; -fx-font-size:13; -fx-font-weight:600;");
                resPointsForts.getChildren().add(l);
            }
        }

        // Points faibles
        if (resPointsFaibles != null) {
            resPointsFaibles.getChildren().clear();
            Label titre = new Label("📈 À Améliorer");
            titre.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#dc2626;");
            resPointsFaibles.getChildren().add(titre);
            for (Object pt : (List<Object>) analyse.getOrDefault("pointsFaibles", List.of())) {
                Label l = new Label("⚡  " + pt);
                l.setStyle("-fx-text-fill:#7f1d1d; -fx-font-size:13; -fx-font-weight:600;");
                resPointsFaibles.getChildren().add(l);
            }
        }

        // Table des notes
        if (notesTable != null) {
            ObservableList<NoteRow> rows = FXCollections.observableArrayList();
            for (Object o : (List<Object>) analyse.getOrDefault("notesDetectees", List.of())) {
                if (o instanceof Map<?,?> n) {
                    double note    = toDouble(n.get("note"));
                    Object nmObj   = n.get("noteMax");
                    double noteMax = nmObj != null ? toDouble(nmObj) : 20.0;
                    Object coObj   = n.get("coefficient");
                    double coeff   = coObj != null ? toDouble(coObj) : 1.0;
                    Object domObj  = n.get("domaine");
                    String domaine = domObj != null ? str(domObj) : "—";
                    double pct     = noteMax > 0 ? (note / noteMax * 100) : 0;
                    String niveau  = pct >= 70 ? "✅ Bon" : pct >= 50 ? "⚠️ Moyen" : "❌ Faible";
                    rows.add(new NoteRow(
                            str(n.get("matiereReleve")),
                            String.format("%.1f / %.0f", note, noteMax),
                            "× " + (int) coeff,
                            domaine,
                            niveau));
                }
            }
            notesTable.setItems(rows);
        }

        // Scores par filière
        if (resScoresFilieres != null) {
            resScoresFilieres.getChildren().clear();
            List<Object> scores = new ArrayList<>((List<Object>) analyse.getOrDefault("scoreParFiliere", List.of()));
            scores.sort((a, b) -> Double.compare(
                    (a instanceof Map<?,?> ma) ? toDouble(ma.get("score")) : 0,
                    (b instanceof Map<?,?> mb) ? toDouble(mb.get("score")) : 0) * -1);
            int rank = 1;
            for (Object o : scores) {
                if (o instanceof Map<?,?> f)
                    resScoresFilieres.getChildren().add(buildFiliereCard(f, rank++));
            }
        }
    }

    /** Carte visuelle d'une filière avec barre de progression. */
    private VBox buildFiliereCard(Map<?,?> f, int rank) {
        double  score      = toDouble(f.get("score"));
        boolean compatible = Boolean.TRUE.equals(f.get("compatible"));
        String  nom        = str(f.get("filiereNom"));
        Object explObj  = f.get("explication");
    String  expl       = explObj != null ? str(explObj) : "";
        String  medal      = rank == 1 ? "🏆" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "📚";
        String  couleur    = score >= 75 ? "#059669" : score >= 50 ? "#d97706" : "#dc2626";

        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; " +
                      "-fx-border-color:" + (compatible ? "#a7f3d0" : "#fecaca") + "; " +
                      "-fx-border-radius:12; -fx-border-width:1.5;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lMedal  = new Label(medal);
        Label lNom    = new Label(nom);
        lNom.setStyle("-fx-font-weight:700; -fx-font-size:14; -fx-text-fill:#1e40af;");
        Label lCompat = new Label(compatible ? "✅ Compatible" : "❌ Non compatible");
        lCompat.setStyle("-fx-font-size:11; -fx-font-weight:700; " +
                         "-fx-text-fill:" + (compatible ? "#065f46" : "#991b1b") + "; " +
                         "-fx-background-color:" + (compatible ? "#d1fae5" : "#fee2e2") + "; " +
                         "-fx-background-radius:20; -fx-padding:2 8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lScore = new Label(String.format("%.1f%%", score));
        lScore.setStyle("-fx-font-weight:800; -fx-font-size:16; -fx-text-fill:" + couleur + ";");
        header.getChildren().addAll(lMedal, lNom, lCompat, spacer, lScore);

        // Barre de progression
        StackPane barBg = new StackPane();
        barBg.setStyle("-fx-background-color:rgba(96,165,250,0.12); -fx-background-radius:8;");
        barBg.setPrefHeight(12);
        barBg.setMaxHeight(12);
        Rectangle barFill = new Rectangle();
        barFill.setHeight(12);
        barFill.widthProperty().bind(barBg.widthProperty().multiply(score / 100.0));
        barFill.setArcWidth(8); barFill.setArcHeight(8);
        if (score >= 75)      barFill.setFill(Color.web("#10b981"));
        else if (score >= 50) barFill.setFill(Color.web("#f59e0b"));
        else                  barFill.setFill(Color.web("#ef4444"));
        barBg.setAlignment(Pos.CENTER_LEFT);
        barBg.getChildren().add(barFill);

        card.getChildren().addAll(header, barBg);
        if (!expl.isBlank()) {
            Label lExpl = new Label("💬 " + expl);
            lExpl.setStyle("-fx-text-fill:#60a5fa; -fx-font-size:12;");
            lExpl.setWrapText(true);
            card.getChildren().add(lExpl);
        }
        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    //  HISTORIQUE
    // ════════════════════════════════════════════════════════════════════

    private void chargerHistorique() {
        if (historiqueListBox == null) return;
        historiqueListBox.getChildren().clear();

        if (currentUser == null) {
            historiqueListBox.getChildren().add(new Label("⚠️ Utilisateur non connecté"));
            return;
        }

        List<ReleveNotes> releves = releveService.findByEtudiantId(currentUser.getId());

        if (releves.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label ico = new Label("📄"); ico.setStyle("-fx-font-size:48;");
            Label msg = new Label("Aucune analyse effectuée");
            msg.setStyle("-fx-font-size:18; -fx-font-weight:700; -fx-text-fill:#1e40af;");
            Label sub = new Label("Uploadez votre premier relevé pour commencer");
            sub.setStyle("-fx-text-fill:#60a5fa; -fx-font-size:13;");
            Button btn = new Button("📄 Analyser mon relevé");
            btn.setStyle("-fx-background-color:linear-gradient(to right,#3b82f6,#60a5fa); -fx-text-fill:white; -fx-font-weight:700; -fx-background-radius:10; -fx-padding:10 24; -fx-cursor:hand;");
            btn.setOnAction(e -> showUploadView());
            empty.getChildren().addAll(ico, msg, sub, btn);
            historiqueListBox.getChildren().add(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
        for (ReleveNotes r : releves)
            historiqueListBox.getChildren().add(buildHistoriqueCard(r, sdf));
    }

    private VBox buildHistoriqueCard(ReleveNotes r, SimpleDateFormat sdf) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color:white; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(96,165,250,0.3); -fx-border-radius:14; -fx-border-width:1.5; " +
                      "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.1),8,0,0,3);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lIco = new Label("pdf".equals(r.getFichierType()) ? "📄" : "🖼️");
        lIco.setStyle("-fx-font-size:22;");

        VBox info = new VBox(3);
        String dateStr = r.getCreatedAt() != null ? sdf.format(r.getCreatedAt()) : "Date inconnue";
        Label lDate = new Label("Analyse du " + dateStr);
        lDate.setStyle("-fx-font-weight:700; -fx-font-size:14; -fx-text-fill:#1e40af;");
        info.getChildren().add(lDate);

        if (r.getFiliereRecommandee() != null && !r.getFiliereRecommandee().isBlank()) {
            Label lF = new Label("🏆 Filière : " + r.getFiliereRecommandee());
            lF.setStyle("-fx-text-fill:#059669; -fx-font-weight:600; -fx-font-size:12;");
            info.getChildren().add(lF);
        }
        if (r.getMoyenneGenerale() > 0) {
            Label lM = new Label(String.format("📊 Moyenne : %.2f / 20", r.getMoyenneGenerale()));
            lM.setStyle("-fx-text-fill:#3b82f6; -fx-font-weight:600; -fx-font-size:12;");
            info.getChildren().add(lM);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoir = new Button("Voir →");
        btnVoir.setStyle("-fx-background-color:linear-gradient(to right,#3b82f6,#60a5fa); -fx-text-fill:white; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:8 16; -fx-cursor:hand;");
        btnVoir.setOnAction(e -> {
            if (r.getAnalyseIA() == null) { showAlert("Analyse non disponible."); return; }
            try {
                Map<String, Object> analyse = analyserService.parseJson(r.getAnalyseIA());
                afficherResultats(analyse);
                showResultView();
            } catch (Exception ex) { showAlert("Erreur : " + ex.getMessage()); }
        });

        header.getChildren().addAll(lIco, info, spacer, btnVoir);
        card.getChildren().add(header);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showErrorAlert(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("SmartPath – Erreur");
        a.setHeaderText(titre);
        a.setContentText(msg);
        // Style personnalisé pour rendre le message lisible
        a.getDialogPane().setStyle(
            "-fx-font-size: 13px; -fx-font-family: 'Segoe UI';"  
        );
        a.showAndWait();
    }

    private String mention(double m) {
        if (m >= 16) return "Très bien";
        if (m >= 14) return "Bien";
        if (m >= 12) return "Assez bien";
        if (m >= 10) return "Passable";
        return "Insuffisant";
    }

    private String couleurMention(String m) {
        return switch (m) {
            case "Très bien"  -> "#059669";
            case "Bien"       -> "#2563eb";
            case "Assez bien" -> "#d97706";
            case "Passable"   -> "#f59e0b";
            default           -> "#dc2626";
        };
    }

    private double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }

    @SuppressWarnings("unchecked")
    private String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            return sb.append("]").toString();
        }
        if (o instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?,?> e : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\":").append(toJson(e.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        }
        if (o instanceof String s) return "\"" + s.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        if (o instanceof Boolean b) return b.toString();
        return o.toString();
    }

    // ── Data class pour la TableView ─────────────────────────────────────

    public static class NoteRow {
        private final String matiere, note, coeff, domaine, niveau;

        public NoteRow(String matiere, String note, String coeff, String domaine, String niveau) {
            this.matiere = matiere; this.note = note; this.coeff = coeff;
            this.domaine = domaine; this.niveau = niveau;
        }

        public String getMatiere()  { return matiere; }
        public String getNote()     { return note; }
        public String getCoeff()    { return coeff; }
        public String getDomaine()  { return domaine; }
        public String getNiveau()   { return niveau; }
    }
}
