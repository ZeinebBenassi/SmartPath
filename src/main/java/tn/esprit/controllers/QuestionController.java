package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entity.Answer;
import tn.esprit.entity.Question;
import tn.esprit.services.QuestionService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * QuestionController — design miroir de Symfony questions/index.html.twig
 * Chaque carte reproduit :
 *   - bulle n° (violet/gris si inactif)
 *   - icône catégorie
 *   - titre + badges statut + catégorie
 *   - grille 2×2 réponses A B C D
 *   - footer actions : Toggle | Modifier | Supprimer
 */
public class QuestionController implements Initializable {

    /* ── FXML injectés ── */
    @FXML private VBox      vboxQuestions;
    @FXML private VBox      vboxEmpty;
    @FXML private TextField txtSearch;
    @FXML private Label     lblStats;       // badge « N actives » toolbar
    @FXML private Label     lblCount;       // badge « N total »  toolbar
    @FXML private Label     lblActiveCount; // badge « N Actives » en-tête liste
    @FXML private Label     lblTotalCount;  // badge « N Total »  en-tête liste
    @FXML private ComboBox<String> cbSort;

    private final QuestionService questionService = new QuestionService();
    private List<Question> allQuestions;

    /* ── Icônes par catégorie (identiques aux emojis Twig) ── */
    private static final Map<String, String> CAT_ICONS = new HashMap<>();
    static {
        CAT_ICONS.put("technique",    "🔧");
        CAT_ICONS.put("approche",     "💡");
        CAT_ICONS.put("creativite",   "🎨");
        CAT_ICONS.put("creativité",   "🎨");
        CAT_ICONS.put("analytique",   "🧠");
        CAT_ICONS.put("analyse",      "🧠");
        CAT_ICONS.put("mathematique", "📐");
        CAT_ICONS.put("mathématique", "📐");
        CAT_ICONS.put("securite",     "🔒");
        CAT_ICONS.put("sécurité",     "🔒");
        CAT_ICONS.put("reseaux",      "🌐");
        CAT_ICONS.put("réseaux",      "🌐");
        CAT_ICONS.put("donnees",      "📊");
        CAT_ICONS.put("données",      "📊");
        CAT_ICONS.put("algorithmique","⚙️");
        CAT_ICONS.put("algorithme",   "⚙️");
        CAT_ICONS.put("pratique",     "🛠️");
    }

    /* Couleurs A B C D — identiques aux classes .letter-X du CSS Symfony */
    private static final String[] LETTER_COLORS =
        {"#6366f1", "#2563eb", "#8b5cf6", "#06b6d4"};
    private static final String[] LETTERS = {"A", "B", "C", "D"};

    /* ════════════════════════════════════════════════
       INITIALISATION
    ════════════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (cbSort != null)
            cbSort.getItems().addAll("Texte A→Z", "Texte Z→A", "Actif d'abord", "Inactif d'abord");
        loadData();
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, o, n) -> afficherQuestions(getCurrentFiltered()));
    }

    /* ════════════════════════════════════════════════
       CHARGEMENT DES DONNÉES
    ════════════════════════════════════════════════ */
    private void loadData() {
        try {
            allQuestions = questionService.afficher();
            int active = 0;
            try { active = questionService.countActiveQuestions(); } catch (Exception ignored) {}

            /* Badges toolbar */
            if (lblStats != null) lblStats.setText("✓ " + active + " actives");
            if (lblCount != null) lblCount.setText(allQuestions.size() + " total");

            /* Badges en-tête liste */
            if (lblActiveCount != null) lblActiveCount.setText("✓ " + active + " Actives");
            if (lblTotalCount  != null) lblTotalCount .setText(allQuestions.size() + " Total");

            afficherQuestions(allQuestions);
        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    /* ════════════════════════════════════════════════
       RENDU DE LA LISTE
    ════════════════════════════════════════════════ */
    private void afficherQuestions(List<Question> questions) {
        vboxQuestions.getChildren().clear();
        vboxQuestions.getChildren().add(vboxEmpty);

        if (questions.isEmpty()) {
            vboxEmpty.setVisible(true); vboxEmpty.setManaged(true);
            return;
        }
        vboxEmpty.setVisible(false); vboxEmpty.setManaged(false);

        int n = 1;
        for (Question q : questions) {
            VBox card = buildQuestionCard(q, n++);
            /* Séparateur entre cartes */
            if (n > 2) {
                Separator sep = new Separator();
                sep.setStyle("-fx-padding: 0; -fx-background-color: #e2e8f0;");
                vboxQuestions.getChildren().add(sep);
            }
            vboxQuestions.getChildren().add(card);
        }
    }

    /* ════════════════════════════════════════════════
       CONSTRUCTION D'UNE CARTE QUESTION
       (miroir exact du bloc .q-card du Twig)
    ════════════════════════════════════════════════ */
    private VBox buildQuestionCard(Question q, int numero) {
        boolean active = q.isActive();
        String icon = CAT_ICONS.getOrDefault(
            q.getCategory() != null ? q.getCategory().toLowerCase() : "", "❓");

        /* Card root */
        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-opacity: " + (active ? "1.0" : "0.65") + ";" +
            "-fx-border-color: " + (active ? "#e2e8f0" : "#e2e8f0") + ";" +
            "-fx-border-width: 0 0 0 0;" +   // pas de bordure interne, c'est le conteneur qui borde
            "-fx-background-radius: 0;"
        );

        /* ── Header : num | icon | titre + badges | actions ── */
        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        head.setStyle("-fx-padding: 10 14 7 14; -fx-background-color: white;");

        // Bulle numéro
        Label num = new Label(String.valueOf(numero));
        num.setMinSize(32, 32); num.setMaxSize(32, 32);
        num.setAlignment(Pos.CENTER);
        num.setStyle(
            "-fx-background-color: " + (active ? "#6366f1" : "#94a3b8") + ";" +
            "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-alignment: CENTER;");

        // Bulle icône catégorie
        Label ico = new Label(icon);
        ico.setMinSize(32, 32); ico.setMaxSize(32, 32);
        ico.setAlignment(Pos.CENTER);
        ico.setStyle(
            "-fx-background-color: #f1f5f9; -fx-font-size: 14px;" +
            "-fx-background-radius: 50; -fx-alignment: CENTER;");

        // Bloc info (titre + badges)
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(q.getText() != null ? q.getText() : "");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-wrap-text: true;");
        title.setWrapText(true); title.setMaxWidth(Double.MAX_VALUE);

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);

        // Badge statut
        Label bStatut = new Label(active ? "✅ Actif" : "🚫 Inactif");
        bStatut.setStyle(
            "-fx-background-color: " + (active ? "#d1fae5" : "#fee2e2") + ";" +
            "-fx-text-fill: " + (active ? "#065f46" : "#991b1b") + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-padding: 2 8;");

        // Badge catégorie
        Label bCat = new Label(q.getCategory() != null ? q.getCategory() : "");
        bCat.setStyle(
            "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6;" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-radius: 50; -fx-padding: 2 8;");

        // Nb réponses
        int nbRep = q.getAnswers() != null ? q.getAnswers().size() : 0;
        Label bRep = new Label(nbRep + " réponse" + (nbRep > 1 ? "s" : ""));
        bRep.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        meta.getChildren().addAll(bStatut, bCat, bRep);
        info.getChildren().addAll(title, meta);

        // Boutons d'action rapides (icônes) — à droite du header
        HBox actionsRight = new HBox(5);
        actionsRight.setAlignment(Pos.CENTER);

        Button btnToggle = makeIconBtn(
            active ? "⏸" : "▶",
            active ? "#d1fae5" : "#f1f5f9",
            active ? "#065f46" : "#475569",
            active ? "1px solid #6ee7b7" : "1px solid #e2e8f0");
        btnToggle.setTooltip(new Tooltip(active ? "Désactiver" : "Activer"));
        btnToggle.setOnAction(e -> handleToggle(q));

        Button btnEdit = makeIconBtn("✏️", "#eff6ff", "#2563eb", "none");
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> handleEdit(q));

        Button btnDel = makeIconBtn("🗑️", "#fee2e2", "#991b1b", "none");
        btnDel.setTooltip(new Tooltip("Supprimer"));
        btnDel.setOnAction(e -> handleDelete(q));

        actionsRight.getChildren().addAll(btnToggle, btnEdit, btnDel);
        head.getChildren().addAll(num, ico, info, actionsRight);

        /* ── Grille réponses A B C D (miroir .q-answers) ── */
        if (q.getAnswers() != null && !q.getAnswers().isEmpty()) {
            Separator sep = new Separator();
            sep.setStyle("-fx-padding: 0;");

            GridPane grid = new GridPane();
            grid.setHgap(8); grid.setVgap(7);
            grid.setPadding(new Insets(8, 14, 12, 14));
            grid.setStyle("-fx-background-color: white;");

            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().addAll(cc, new ColumnConstraints() {{ setPercentWidth(50); }});

            List<Answer> answers = q.getAnswers();
            for (int i = 0; i < Math.min(answers.size(), 4); i++) {
                Answer a = answers.get(i);

                HBox cell = new HBox(7);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setStyle(
                    "-fx-background-color: #f8fafc;" +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 9;" +
                    "-fx-background-radius: 9; -fx-padding: 6 10;");

                // Carré lettre coloré
                Label lLetter = new Label(LETTERS[i]);
                lLetter.setMinSize(24, 24); lLetter.setMaxSize(24, 24);
                lLetter.setAlignment(Pos.CENTER);
                lLetter.setStyle(
                    "-fx-background-color: " + LETTER_COLORS[i] + ";" +
                    "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 6; -fx-alignment: CENTER;");

                // Texte de la réponse
                Label lText = new Label(a.getText() != null ? a.getText() : "");
                lText.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151; -fx-font-weight: 500; -fx-wrap-text: true;");
                lText.setWrapText(true); HBox.setHgrow(lText, Priority.ALWAYS); lText.setMaxWidth(Double.MAX_VALUE);

                // Points
                Label lPts = new Label(a.getPoints() > 0 ? "+" + a.getPoints() + "pt" : "0pt");
                lPts.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (a.getPoints() > 0 ? "#10b981" : "#94a3b8") + ";");

                cell.getChildren().addAll(lLetter, lText, lPts);
                grid.add(cell, i % 2, i / 2);
            }
            card.getChildren().addAll(head, sep, grid);
        } else {
            card.getChildren().add(head);
        }

        /* ── Footer actions texte (miroir .q-footer) ── */
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
            "-fx-padding: 6 14;" +
            "-fx-background-color: #f8fafc;" +
            "-fx-border-color: #f1f5f9 transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;");

        Button ftToggle = makeFooterBtn(
            active ? "👁 Désactiver" : "👁 Activer",
            active ? "#d1fae5" : "#f1f5f9",
            active ? "#065f46" : "#475569",
            active ? "1px solid #6ee7b7" : "1px solid #e2e8f0");
        ftToggle.setOnAction(e -> handleToggle(q));

        Button ftEdit = makeFooterBtn("✏️ Modifier", "#eff6ff", "#2563eb", "none");
        ftEdit.setOnAction(e -> handleEdit(q));

        Button ftDel = makeFooterBtn("🗑️ Supprimer", "#fee2e2", "#991b1b", "none");
        ftDel.setOnAction(e -> handleDelete(q));

        footer.getChildren().addAll(ftToggle, ftEdit, ftDel);
        card.getChildren().add(footer);

        return card;
    }

    /* ════════════════════════════════════════════════
       HELPERS BOUTONS
    ════════════════════════════════════════════════ */
    private Button makeIconBtn(String text, String bg, String fg, String border) {
        Button b = new Button(text);
        b.setMinSize(30, 30); b.setMaxSize(30, 30);
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 12px; -fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            (border.equals("none") ? "" : "-fx-border-color: " + border.replace("1px solid ","") + "; -fx-border-radius: 8;"));
        return b;
    }

    private Button makeFooterBtn(String text, String bg, String fg, String border) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-padding: 5 12; -fx-cursor: hand;" +
            (border.equals("none") ? "" : "-fx-border-color: " + border.replace("1px solid ","") + "; -fx-border-radius: 8;"));
        return b;
    }

    /* ════════════════════════════════════════════════
       ACTIONS
    ════════════════════════════════════════════════ */
    @FXML private void handleSort() {
        if (cbSort == null || cbSort.getValue() == null) return;
        List<Question> sorted = new ArrayList<>(getCurrentFiltered());
        switch (cbSort.getValue()) {
            case "Texte A→Z"       -> sorted.sort(Comparator.comparing(q -> q.getText() != null ? q.getText().toLowerCase() : ""));
            case "Texte Z→A"       -> sorted.sort(Comparator.comparing((Question q) -> q.getText() != null ? q.getText().toLowerCase() : "").reversed());
            case "Actif d'abord"   -> sorted.sort(Comparator.comparing(Question::isActive).reversed());
            case "Inactif d'abord" -> sorted.sort(Comparator.comparing(Question::isActive));
        }
        afficherQuestions(sorted);
    }

    private List<Question> getCurrentFiltered() {
        String s = txtSearch != null && txtSearch.getText() != null
            ? txtSearch.getText().toLowerCase() : "";
        return allQuestions.stream().filter(q ->
            s.isEmpty()
            || (q.getText()     != null && q.getText().toLowerCase().contains(s))
            || (q.getCategory() != null && q.getCategory().toLowerCase().contains(s))
        ).collect(Collectors.toList());
    }

    @FXML private void handleAdd()            { openQuestionForm(null); }
    private void      handleEdit(Question q)  { openQuestionForm(q);    }

    private void handleToggle(Question q) {
        try { questionService.toggleActive(q.getId()); loadData(); }
        catch (SQLException e) { showError("Erreur toggle : " + e.getMessage()); }
    }

    private void handleDelete(Question q) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("⚠️ Supprimer cette question ?");
        String txt = q.getText() != null ? q.getText() : "";
        alert.setContentText("\"" + (txt.length() > 70 ? txt.substring(0,70)+"…" : txt) +
            "\"\n\nCette action est irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { questionService.supprimer(q.getId()); loadData(); }
                catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
            }
        });
    }

    @FXML private void handleStartQuiz() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/QuizPlayer.fxml")));
            Stage stage = new Stage();
            stage.setTitle("SmartPath — Quiz de Personnalité");
            stage.setScene(new Scene(root));
            stage.setMinWidth(900); stage.setMinHeight(650);
            stage.show();
        } catch (IOException e) { showError("Impossible : " + e.getMessage()); }
    }

    @FXML private void handleStartQuizFromMenu() { handleStartQuiz(); }

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml")));
            if (vboxQuestions != null && vboxQuestions.getScene() != null)
                vboxQuestions.getScene().setRoot(root);
        } catch (IOException e) { showError("Navigation impossible : " + e.getMessage()); }
    }

    @FXML private void goToFilieres() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/FiliereContent.fxml")));
            if (vboxQuestions != null && vboxQuestions.getScene() != null) {
                javafx.scene.Node parent = vboxQuestions.getParent();
                while (parent != null && !(parent instanceof StackPane))
                    parent = parent.getParent();
                if (parent instanceof StackPane)
                    ((StackPane) parent).getChildren().setAll(root);
                else {
                    Stage stage = (Stage) vboxQuestions.getScene().getWindow();
                    stage.setScene(new Scene(root));
                }
            }
        } catch (IOException e) { showError("Navigation impossible : " + e.getMessage()); }
    }

    private void openQuestionForm(Question question) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/QuestionForm.fxml")));
            Parent root = loader.load();
            QuestionFormController ctrl = loader.getController();
            ctrl.initData(question, this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(question == null ? "Nouvelle Question" : "Modifier la Question");
            stage.setScene(new Scene(root));
            stage.setWidth(650); stage.setHeight(750);
            stage.setMinWidth(600); stage.setMinHeight(600);
            stage.showAndWait();
            loadData();
        } catch (IOException e) { showError("Impossible : " + e.getMessage()); }
    }

    public void refreshData() { loadData(); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg); a.show();
    }
}
