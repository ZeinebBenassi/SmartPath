package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entity.Answer;
import tn.esprit.entity.Question;
import services.QuestionService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionController implements Initializable {

    @FXML private VBox      vboxQuestions;
    @FXML private VBox      vboxEmpty;
    @FXML private TextField txtSearch;
    @FXML private Label     lblStats;
    @FXML private Label     lblCount;
    @FXML private ComboBox<String> cbSort;

    private final QuestionService questionService = new QuestionService();
    private List<Question> allQuestions;

    private static final Map<String, String> CAT_COLORS = Map.of(
        "analytique","#6366F1","creatif","#6366F1","algorithmique","#6366F1",
        "technique","#6366F1","reseaux","#6366F1","securite","#6366F1",
        "pratique","#6366F1","donnees","#6366F1"
    );
    private static final Map<String, String> CAT_ICONS = Map.of(
        "analytique","🧠","creatif","🎨","algorithmique","⚙️",
        "technique","🔧","reseaux","🌐","securite","🔒",
        "pratique","💡","donnees","📊"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (cbSort != null)
            cbSort.getItems().addAll("Texte A→Z","Texte Z→A","Actif d'abord","Inactif d'abord");
        loadData();
        if (txtSearch != null) setupSearch();
    }

    private void loadData() {
        try {
            allQuestions = questionService.afficher();
            if (lblStats != null) {
                try { lblStats.setText(questionService.countActiveQuestions() + " active(s)"); }
                catch (Exception ignored) { lblStats.setText(allQuestions.size() + " question(s)"); }
            }
            afficherQuestions(allQuestions);
        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    private void afficherQuestions(List<Question> questions) {
        vboxQuestions.getChildren().clear();
        vboxQuestions.getChildren().add(vboxEmpty);
        if (lblCount != null)
            lblCount.setText(questions.size() + " question" + (questions.size() > 1 ? "s" : ""));
        if (questions.isEmpty()) {
            vboxEmpty.setVisible(true); vboxEmpty.setManaged(true); return;
        }
        vboxEmpty.setVisible(false); vboxEmpty.setManaged(false);
        int n = 1;
        for (Question q : questions) vboxQuestions.getChildren().add(createQuestionCard(q, n++));
    }

    private VBox createQuestionCard(Question q, int numero) {
        String color = "#6366F1";
        String icon = CAT_ICONS.getOrDefault(
            q.getCategory() != null ? q.getCategory().toLowerCase() : "", "❓");

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                      "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-padding: 12 16; -fx-background-color: white; -fx-background-radius: 12 12 0 0;");

        Label lblNum = new Label(String.valueOf(numero));
        lblNum.setMinWidth(28); lblNum.setMaxWidth(28); lblNum.setMinHeight(28); lblNum.setMaxHeight(28);
        lblNum.setAlignment(Pos.CENTER);
        lblNum.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 50%; -fx-alignment: CENTER;");

        Label lblIcon = new Label(icon);
        lblIcon.setMinWidth(22); lblIcon.setMaxWidth(22);
        lblIcon.setStyle("-fx-font-size: 16px;");

        VBox centerBox = new VBox(3);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        Label lblText = new Label(q.getText());
        lblText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true;");
        lblText.setWrapText(true); lblText.setMaxWidth(Double.MAX_VALUE);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label lblCat = new Label(q.getCategory() != null ? q.getCategory() : "");
        lblCat.setStyle("-fx-background-color: " + color + "1A; -fx-text-fill: " + color + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 2 8;");

        Label lblStatut = new Label(q.isActive() ? "✅ Actif" : "⛔ Inactif");
        lblStatut.setStyle("-fx-background-color: " + (q.isActive() ? "#DCFCE7" : "#FEE2E2") + ";" +
                           "-fx-text-fill: " + (q.isActive() ? "#16A34A" : "#DC2626") + ";" +
                           "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 2 8;");
        badges.getChildren().addAll(lblCat, lblStatut);
        centerBox.getChildren().addAll(lblText, badges);

        Button btnEdit   = makeBtn("✏", "#EEF2FF", "#6366F1"); btnEdit.setOnAction(e -> handleEdit(q));
        Button btnToggle = makeBtn(q.isActive() ? "⏸" : "▶", "#FFF7ED", "#F59E0B"); btnToggle.setOnAction(e -> handleToggle(q));
        Button btnDelete = makeBtn("🗑", "#FEE2E2", "#EF4444"); btnDelete.setOnAction(e -> handleDelete(q));

        HBox actions = new HBox(5, btnEdit, btnToggle, btnDelete);
        actions.setAlignment(Pos.CENTER);

        header.getChildren().addAll(lblNum, lblIcon, centerBox, actions);

        if (q.getAnswers() != null && !q.getAnswers().isEmpty()) {
            Separator sep = new Separator();
            GridPane grid = new GridPane();
            grid.setHgap(8); grid.setVgap(6);
            grid.setStyle("-fx-padding: 10 16 14 16;");
            ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
            ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
            grid.getColumnConstraints().addAll(c1, c2);
            String[] letters = {"A","B","C","D"};
            List<Answer> answers = q.getAnswers();
            for (int i = 0; i < Math.min(answers.size(), 4); i++) {
                Answer a = answers.get(i);
                HBox ab = new HBox(6);
                ab.setAlignment(Pos.CENTER_LEFT);
                ab.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-padding: 6 10;" +
                            "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;");
                Label ll = new Label(letters[i]);
                ll.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 10px;" +
                            "-fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 1 5;");
                Label la = new Label(a.getText());
                la.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-wrap-text: true;");
                la.setWrapText(true); HBox.setHgrow(la, Priority.ALWAYS);
                Label lp = new Label("+" + a.getPoints() + "pt");
                lp.setStyle("-fx-font-size: 9px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                ab.getChildren().addAll(ll, la, lp);
                grid.add(ab, i % 2, i / 2);
            }
            card.getChildren().addAll(header, sep, grid);
        } else {
            card.getChildren().add(header);
        }
        return card;
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setMinWidth(32); btn.setMaxWidth(32); btn.setMinHeight(32); btn.setMaxHeight(32);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                     "-fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand;");
        return btn;
    }

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
        String search = txtSearch != null && txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        return allQuestions.stream().filter(q ->
            search.isEmpty()
            || (q.getText()     != null && q.getText().toLowerCase().contains(search))
            || (q.getCategory() != null && q.getCategory().toLowerCase().contains(search))
        ).collect(Collectors.toList());
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, o, n) -> afficherQuestions(getCurrentFiltered()));
    }

    @FXML private void handleAdd()  { openQuestionForm(null); }
    private void handleEdit(Question q) { openQuestionForm(q); }

    private void handleToggle(Question q) {
        try { questionService.toggleActive(q.getId()); loadData(); }
        catch (SQLException e) { showError("Erreur toggle : " + e.getMessage()); }
    }

    private void handleDelete(Question q) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la question ?");
        alert.setContentText("\"" + q.getText() + "\"\nCette action est irréversible.");
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

    /**
     * Navigation depuis la sidebar de QuestionView.fxml (standalone).
     * Quand QuestionContent.fxml est chargé dans le contentArea du Dashboard,
     * ces méthodes ne sont pas appelées (pas de sidebar).
     */
    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml")));
            if (vboxQuestions != null && vboxQuestions.getScene() != null) {
                vboxQuestions.getScene().setRoot(root);
            }
        } catch (IOException e) { showError("Navigation impossible : " + e.getMessage()); }
    }

    @FXML private void goToFilieres() {
        // Charge FiliereContent.fxml (sans sidebar)
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/FiliereContent.fxml")));
            if (vboxQuestions != null && vboxQuestions.getScene() != null) {
                // Si on est dans un contentArea (StackPane parent), on remplace le contenu
                javafx.scene.Node parent = vboxQuestions.getParent();
                while (parent != null && !(parent instanceof StackPane)) {
                    parent = parent.getParent();
                }
                if (parent instanceof StackPane) {
                    ((StackPane) parent).getChildren().setAll(root);
                } else {
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
        Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
}
