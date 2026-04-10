package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entity.Filiere;
import services.FiliereService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class FiliereController implements Initializable {

    @FXML private FlowPane           flowCards;
    @FXML private VBox               vboxEmpty;
    @FXML private TextField          txtSearch;
    @FXML private Label              lblStats;
    @FXML private Label              lblCount;
    @FXML private ComboBox<String>   cbFilterCategorie;
    @FXML private ComboBox<String>   cbSort;

    private final FiliereService filiereService = new FiliereService();
    private List<Filiere> allFilieres;

    private static final Map<String, String> COLORS = Map.of(
        "informatique", "#6366F1", "mathematique", "#F59E0B",
        "sciences",     "#10B981", "langues",      "#3B82F6",
        "economie",     "#EF4444", "gestion",      "#8B5CF6"
    );
    private static final Map<String, String> ICONS = Map.of(
        "informatique", "💻", "mathematique", "📐",
        "sciences",     "🔬", "langues",      "🌍",
        "economie",     "📈", "gestion",      "🏢"
    );
    private static final List<String> NIVEAU_ORDER = List.of(
        "Bac","Bac+1","Bac+2","Bac+3","Bac+4","Bac+5",
        "Licence","Master","Doctorat","Débutant","Intermédiaire","Avancé","Expert"
    );

    // Largeur des cartes : calculée dynamiquement pour avoir 3 cartes par ligne
    private double cardWidth = 274.0;
    // Mémoriser la liste courante pour pouvoir redessiner quand la fenêtre est redimensionnée
    private List<Filiere> currentList = new ArrayList<>();

    private int niveauIndex(String n) {
        if (n == null) return Integer.MAX_VALUE;
        int i = NIVEAU_ORDER.indexOf(n.trim());
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (cbSort != null)
            cbSort.getItems().addAll("Nom A→Z","Nom Z→A","Niveau ↑","Niveau ↓","Catégorie A→Z","Catégorie Z→A");

        // Listener sur la largeur du FlowPane : redessine les cartes quand la taille change
        flowCards.widthProperty().addListener((obs, oldW, newW) -> {
            double fw = newW.doubleValue();
            if (fw > 100) {
                double newCW = (fw - 2 * flowCards.getHgap() - 4) / 3.0;
                if (Math.abs(newCW - cardWidth) > 2) {
                    cardWidth = newCW;
                    if (!currentList.isEmpty()) afficherCartes(currentList);
                }
            }
        });

        loadData();
        if (txtSearch != null) setupSearch();
    }

    private void loadData() {
        try {
            allFilieres = filiereService.afficher();
            if (cbFilterCategorie != null) {
                List<String> categories = allFilieres.stream()
                    .map(Filiere::getCategorie).filter(Objects::nonNull)
                    .distinct().sorted().collect(Collectors.toList());
                categories.add(0, "Tous");
                cbFilterCategorie.setItems(FXCollections.observableArrayList(categories));
                cbFilterCategorie.getSelectionModel().selectFirst();
            }
            afficherCartes(allFilieres);
        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    private void afficherCartes(List<Filiere> filieres) {
        currentList = filieres; // mémoriser pour le redimensionnement
        flowCards.getChildren().clear();
        // Calculer la largeur des cartes maintenant (FlowPane peut déjà avoir une taille)
        double fw = flowCards.getWidth();
        if (fw > 100) {
            cardWidth = (fw - 2 * flowCards.getHgap() - 4) / 3.0;
        }
        // Afficher le nombre une seule fois dans lblCount (barre de contrôles)
        if (lblCount  != null) lblCount.setText(filieres.size() + " filière" + (filieres.size() > 1 ? "s" : ""));
        // lblStats dans FiliereView.fxml (sidebar) affiche uniquement le chiffre
        if (lblStats != null) lblStats.setText(String.valueOf(filieres.size()));
        if (filieres.isEmpty()) {
            vboxEmpty.setVisible(true); vboxEmpty.setManaged(true); return;
        }
        vboxEmpty.setVisible(false); vboxEmpty.setManaged(false);
        for (Filiere f : filieres) flowCards.getChildren().add(createCard(f));
    }

    private VBox createCard(Filiere f) {
        double CW = cardWidth > 100 ? cardWidth : 274.0;
        String color = COLORS.getOrDefault(
            f.getCategorie() != null ? f.getCategorie().toLowerCase() : "", "#6366F1");
        String icon = ICONS.getOrDefault(
            f.getCategorie() != null ? f.getCategorie().toLowerCase() : "", "🎓");

        VBox card = new VBox(0);
        card.setPrefWidth(CW);
        card.setMaxWidth(CW);
        card.setMinWidth(CW);
        card.setMinHeight(340);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3); -fx-cursor: hand;");

        // Header image / icône
        StackPane header = new StackPane();
        header.setPrefHeight(140); header.setMinHeight(140); header.setMaxHeight(140);
        header.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 14 14 0 0;");

        boolean loaded = false;
        if (f.getImage() != null && !f.getImage().isEmpty()) {
            try {
                File imgFile = new File(f.getImage());
                if (imgFile.exists()) {
                    Image img = new Image(imgFile.toURI().toString(), CW, 140, false, true);
                    if (!img.isError()) {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(CW); iv.setFitHeight(140); iv.setPreserveRatio(false);
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(CW, 140);
                        clip.setArcWidth(28); clip.setArcHeight(28); iv.setClip(clip);
                        VBox overlay = new VBox();
                        overlay.setPrefSize(CW, 140);
                        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.18); -fx-background-radius: 14 14 0 0;");
                        header.getChildren().addAll(iv, overlay);
                        loaded = true;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (!loaded) {
            Label lbl = new Label(icon); lbl.setStyle("-fx-font-size: 46px;");
            header.getChildren().add(lbl);
        }

        Label lblNiv = new Label(f.getNiveau() != null ? f.getNiveau() : "");
        lblNiv.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 10;" +
                "-fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        StackPane.setAlignment(lblNiv, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(lblNiv, new javafx.geometry.Insets(0, 8, 8, 0));
        header.getChildren().add(lblNiv);

        // Body
        VBox body = new VBox(5); body.setStyle("-fx-padding: 10 12 6 12;");
        VBox.setVgrow(body, Priority.ALWAYS);

        Label lblNom = new Label(f.getNom() != null ? f.getNom() : "—");
        lblNom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true;");
        lblNom.setWrapText(true); lblNom.setMaxWidth(CW - 24);

        Label lblCat = new Label(f.getCategorie() != null ? f.getCategorie() : "");
        lblCat.setStyle("-fx-background-color: " + color + "1A; -fx-background-radius: 8;" +
                "-fx-padding: 2 7; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        body.getChildren().addAll(lblNom, lblCat);

        if (f.getDescription() != null && !f.getDescription().isEmpty()) {
            Label lblDesc = new Label(f.getDescription());
            lblDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-wrap-text: true;");
            lblDesc.setWrapText(true); lblDesc.setMaxWidth(CW - 24); lblDesc.setMaxHeight(36);
            body.getChildren().add(lblDesc);
        }

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        // Footer boutons
        HBox footer = new HBox(6); footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-padding: 8 12 10 12; -fx-border-color: #F1F5F9 transparent transparent transparent; -fx-border-width: 1;");

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(btnEdit, Priority.ALWAYS);
        btnEdit.setStyle("-fx-background-color: " + color + "1A; -fx-text-fill: " + color + ";" +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 0; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> handleEdit(f));

        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(btnDel, Priority.ALWAYS);
        btnDel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444;" +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 0; -fx-cursor: hand;");
        btnDel.setOnAction(e -> handleDelete(f));

        footer.getChildren().addAll(btnEdit, btnDel);
        card.getChildren().addAll(header, body, spacer, footer);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.20),18,0,0,6); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3); -fx-cursor: hand;"));
        return card;
    }

    @FXML private void handleSort() {
        if (cbSort == null || cbSort.getValue() == null) return;
        List<Filiere> sorted = new ArrayList<>(getCurrentFiltered());
        switch (cbSort.getValue()) {
            case "Nom A→Z"       -> sorted.sort(Comparator.comparing(f -> f.getNom() != null ? f.getNom().toLowerCase() : ""));
            case "Nom Z→A"       -> sorted.sort(Comparator.comparing((Filiere f) -> f.getNom() != null ? f.getNom().toLowerCase() : "").reversed());
            case "Niveau ↑"      -> sorted.sort(Comparator.comparingInt(f -> niveauIndex(f.getNiveau())));
            case "Niveau ↓"      -> sorted.sort(Comparator.comparingInt((Filiere f) -> niveauIndex(f.getNiveau())).reversed());
            case "Catégorie A→Z" -> sorted.sort(Comparator.comparing(f -> f.getCategorie() != null ? f.getCategorie().toLowerCase() : ""));
            case "Catégorie Z→A" -> sorted.sort(Comparator.comparing((Filiere f) -> f.getCategorie() != null ? f.getCategorie().toLowerCase() : "").reversed());
        }
        afficherCartes(sorted);
    }

    private List<Filiere> getCurrentFiltered() {
        String search = txtSearch != null && txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        String cat    = cbFilterCategorie != null ? cbFilterCategorie.getValue() : null;
        return allFilieres.stream().filter(f -> {
            boolean ms = search.isEmpty()
                || (f.getNom() != null && f.getNom().toLowerCase().contains(search))
                || (f.getCategorie() != null && f.getCategorie().toLowerCase().contains(search));
            boolean mc = cat == null || cat.equals("Tous") || cat.equals(f.getCategorie());
            return ms && mc;
        }).collect(Collectors.toList());
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, o, n) -> afficherCartes(getCurrentFiltered()));
    }

    @FXML private void handleFilter() { afficherCartes(getCurrentFiltered()); }
    @FXML private void handleAdd()    { openFiliereForm(null); }
    private void handleEdit(Filiere f){ openFiliereForm(f); }

    private void handleDelete(Filiere f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la filière ?");
        alert.setContentText("\"" + f.getNom() + "\"\nCette action est irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { filiereService.supprimer(f.getId()); loadData(); showInfo("Filière supprimée."); }
                catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
            }
        });
    }

    @FXML private void goToDashboard() { navigateScene("/tn/esprit/interfaces/DashboardAdmin.fxml"); }
    @FXML private void goToQuestions(){ navigateScene("/tn/esprit/interfaces/QuestionView.fxml"); }
    @FXML private void goToQuiz()     {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/QuizPlayer.fxml")));
            Stage stage = new Stage();
            stage.setTitle("SmartPath — Quiz de Personnalité");
            stage.setScene(new Scene(root));
            stage.setMinWidth(900); stage.setMinHeight(650);
            stage.show();
        } catch (IOException e) { showError("Impossible d'ouvrir le quiz : " + e.getMessage()); }
    }

    private void navigateScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            Stage stage = (Stage) flowCards.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { showError("Navigation impossible : " + e.getMessage()); }
    }

    private void openFiliereForm(Filiere filiere) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/interfaces/FiliereForm.fxml")));
            Parent root = loader.load();
            FiliereFormController ctrl = loader.getController();
            ctrl.initData(filiere, this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(filiere == null ? "Nouvelle Filière" : "Modifier la Filière");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
            loadData();
        } catch (IOException e) { showError("Impossible d'ouvrir le formulaire : " + e.getMessage()); }
    }

    public void refreshData() { loadData(); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
}
