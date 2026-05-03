package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Lecon;
import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.services.feature_cours_et_quiz.LeconService;
import tn.esprit.services.CourseRatingService;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import tn.esprit.utils.feature_cours_et_quiz.AccessControl;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

public class MatiereCoursController implements NavigableController {
    private static final String PDF_PREFIX = "__PDF__:";

    @FXML private Label matiereTitle;
    @FXML private Label errorLabel;
    @FXML private ListView<Lecon> leconsList;
    @FXML private TextArea contenuArea;
    @FXML private VBox pdfDropZone;
    @FXML private Button openPdfBtn;
    @FXML private Label avgRatingLabel;
    @FXML private HBox ratingBox;
    @FXML private HBox starsContainer;

    private final LeconService leconService = new LeconService();
    private final CourseRatingService ratingService = new CourseRatingService();

    private AppShellController appShell;
    private Matiere matiere;
    private Role role = Role.ETUDIANT;
    private String selectedPdfPath;

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
        role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getType());
        
        if (openPdfBtn != null) { openPdfBtn.setVisible(false); openPdfBtn.setManaged(false); }
        if (ratingBox != null) ratingBox.setVisible(false);

        setupPdfDropZone();

        if (leconsList != null) {
            leconsList.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(Lecon item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitre());
                }
            });
            leconsList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> onLessonSelected(selected));
        }
    }

    @Override
    public void setAppShell(AppShellController appShell) { this.appShell = appShell; }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
        if (matiereTitle != null) matiereTitle.setText(matiere == null ? "📖 Cours" : "📖 Cours — " + matiere.getTitre());
        loadLecons();
    }

    @FXML public void handleBack() { if (appShell != null) appShell.showMatieres(); }

    private void loadLecons() {
        if (matiere == null || leconsList == null) return;
        try {
            List<Lecon> lecons = leconService.getByMatiereId(matiere.getId());
            leconsList.getItems().setAll(lecons);
            if (!lecons.isEmpty()) leconsList.getSelectionModel().select(0);
        } catch (SQLException e) { if (errorLabel != null) errorLabel.setText("Erreur: " + e.getMessage()); }
    }

    private void onLessonSelected(Lecon selected) {
        selectedPdfPath = null;
        if (openPdfBtn != null) { openPdfBtn.setVisible(false); openPdfBtn.setManaged(false); }
        resetStars();

        if (selected == null) { if (ratingBox != null) ratingBox.setVisible(false); return; }

        if (ratingBox != null) {
            ratingBox.setVisible(true);
            updateAvgRating(selected.getId());
            loadStudentRating(selected.getId());
        }

        if (starsContainer != null) {
            boolean canRate = (role == Role.ETUDIANT);
            starsContainer.setMouseTransparent(!canRate);
            starsContainer.setOpacity(canRate ? 1.0 : 0.75);
        }

        String contenu = selected.getContenu() == null ? "" : selected.getContenu();
        if (contenu.startsWith(PDF_PREFIX)) {
            selectedPdfPath = contenu.substring(PDF_PREFIX.length()).trim();
            contenuArea.setText("📄 Cours au format PDF\n\nCliquez sur 'Ouvrir PDF' pour le consulter.");
            if (openPdfBtn != null) { openPdfBtn.setVisible(true); openPdfBtn.setManaged(true); }
        } else {
            contenuArea.setText(contenu);
        }
    }

    private void updateAvgRating(int courseId) {
        try {
            double avg = ratingService.getAverageRating(courseId);
            avgRatingLabel.setText(String.format("Moyenne: %.1f/5", avg));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadStudentRating(int courseId) {
        int studentId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : 0;
        if (studentId == 0) return;
        try {
            int studentStars = ratingService.getStudentRating(courseId, studentId);
            highlightStars(studentStars);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void submitRating(int stars) {
        if (role != Role.ETUDIANT) {
            new Alert(Alert.AlertType.INFORMATION, "Seuls les étudiants peuvent laisser un feedback.").showAndWait();
            return;
        }
        Lecon selected = leconsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Feedback");
        dialog.setHeaderText("Votre avis sur ce cours (" + stars + "/5 stars)");
        dialog.setContentText("Commentaire :");
        Optional<String> result = dialog.showAndWait();
        String comment = result.orElse("");

        if (containsBadWords(comment)) {
            new Alert(Alert.AlertType.WARNING,
                    "Votre commentaire contient des mots interdits. Veuillez le reformuler.").showAndWait();
            return;
        }

        try {
            int studentId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : 0;
            ratingService.addRating(selected.getId(), studentId, stars, comment);
            updateAvgRating(selected.getId());
            highlightStars(stars);
            new Alert(Alert.AlertType.INFORMATION, "Évaluation enregistrée !").showAndWait();
        } catch (SQLException e) { new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait(); }
    }

    private void highlightStars(int stars) {
        for (int i = 0; i < 5; i++) {
            Label star = (Label) starsContainer.getChildren().get(i);
            if (i < stars) star.setStyle("-fx-text-fill: #FFD700; -fx-cursor: hand; -fx-font-size: 24;");
            else star.setStyle("-fx-text-fill: #BDC3C7; -fx-cursor: hand; -fx-font-size: 20;");
        }
    }

    private void resetStars() {
        for (javafx.scene.Node node : starsContainer.getChildren()) {
            node.setStyle("-fx-text-fill: #BDC3C7; -fx-cursor: hand; -fx-font-size: 20;");
        }
    }

    @FXML public void handleRate1() { submitRating(1); }
    @FXML public void handleRate2() { submitRating(2); }
    @FXML public void handleRate3() { submitRating(3); }
    @FXML public void handleRate4() { submitRating(4); }
    @FXML public void handleRate5() { submitRating(5); }

    private boolean containsBadWords(String comment) {
        if (comment == null || comment.isBlank()) {
            return false;
        }

        String normalized = comment.toLowerCase(Locale.ROOT);
        return normalized.contains("merde") || normalized.contains("putain");
    }

    private void setupPdfDropZone() {
        if (pdfDropZone == null) return;
        
        // Show for Admin and Prof
        boolean canUpload = AccessControl.canManageContent(role);
        pdfDropZone.setVisible(canUpload);
        pdfDropZone.setManaged(canUpload);
        
        if (!canUpload) return;

        pdfDropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != pdfDropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pdfDropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        handleNewPdfFile(file);
                        success = true;
                        break;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void handleNewPdfFile(File file) {
        try {
            Path destDir = Paths.get(System.getProperty("user.home"), "smartpath_cours");
            Files.createDirectories(destDir);
            Path destPath = destDir.resolve(System.currentTimeMillis() + "_" + file.getName());
            Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            String title = file.getName().replace(".pdf", "");
            int matiereId = (matiere != null) ? matiere.getId() : 0;
            int profId = (AppSession.getCurrentUser() != null) ? AppSession.getCurrentUser().getId() : 0;

            leconService.create(new Lecon(0, title, PDF_PREFIX + destPath.toString(), matiereId), profId);
            loadLecons();
            new Alert(Alert.AlertType.INFORMATION, "PDF ajouté avec succès !").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ajout du PDF: " + e.getMessage()).showAndWait();
        }
    }

    @FXML public void handleOpenPdf() {
        if (selectedPdfPath == null || selectedPdfPath.isBlank()) return;
        try {
            File file = new File(selectedPdfPath);
            if (file.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
            else new Alert(Alert.AlertType.WARNING, "Fichier introuvable.").showAndWait();
        } catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait(); }
    }
}
