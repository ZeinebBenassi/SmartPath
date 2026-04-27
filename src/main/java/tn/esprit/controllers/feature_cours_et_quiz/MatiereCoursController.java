package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Lecon;
import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.entity.feature_cours_et_quiz.Role;
import tn.esprit.services.feature_cours_et_quiz.LeconService;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;
import tn.esprit.utils.feature_cours_et_quiz.RoleUtils;
import tn.esprit.utils.feature_cours_et_quiz.AccessControl;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;

public class MatiereCoursController implements NavigableController {
    private static final String PDF_PREFIX = "__PDF__:";

    @FXML private Label matiereTitle;
    @FXML private Label errorLabel;
    @FXML private ListView<Lecon> leconsList;
    @FXML private TextArea contenuArea;
    @FXML private VBox pdfDropZone;
    @FXML private Button openPdfBtn;

    private final LeconService leconService = new LeconService();

    private AppShellController appShell;
    private Matiere matiere;
    private Role role = Role.ETUDIANT;
    private String selectedPdfPath;

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        role = RoleUtils.normalize(AppSession.getCurrentUser() == null ? null : AppSession.getCurrentUser().getType());

        if (contenuArea != null) {
            contenuArea.setEditable(false);
        }

        if (openPdfBtn != null) {
            openPdfBtn.setVisible(false);
            openPdfBtn.setManaged(false);
        }

        setupPdfDropZone();

        if (leconsList != null) {
            leconsList.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Lecon item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitre());
                }
            });

            leconsList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
                onLessonSelected(selected);
            });
        }
    }

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
        if (matiereTitle != null) {
            matiereTitle.setText(matiere == null ? "📖 Cours" : "📖 Cours — " + matiere.getTitre());
        }
        loadLecons();
    }

    @FXML
    public void handleBack() {
        if (appShell != null) {
            appShell.showMatieres();
        }
    }

    private void loadLecons() {
        if (matiere == null || leconsList == null) {
            return;
        }
        try {
            List<Lecon> lecons = leconService.getByMatiereId(matiere.getId());
            leconsList.getItems().setAll(lecons);
            if (!lecons.isEmpty()) {
                leconsList.getSelectionModel().select(0);
            } else if (contenuArea != null) {
                contenuArea.setText("Aucun cours disponible pour cette matière.");
            }
        } catch (SQLException e) {
            if (errorLabel != null) {
                errorLabel.setText("Erreur: " + e.getMessage());
            }
        }
    }

    private void onLessonSelected(Lecon selected) {
        selectedPdfPath = null;
        if (openPdfBtn != null) {
            openPdfBtn.setVisible(false);
            openPdfBtn.setManaged(false);
        }

        if (selected == null || contenuArea == null) {
            return;
        }

        String contenu = selected.getContenu() == null ? "" : selected.getContenu();
        if (contenu.startsWith(PDF_PREFIX)) {
            selectedPdfPath = contenu.substring(PDF_PREFIX.length()).trim();
            contenuArea.setText("📄 Cours au format PDF\n\nCliquez sur 'Ouvrir PDF' pour le consulter.");
            if (openPdfBtn != null) {
                openPdfBtn.setVisible(true);
                openPdfBtn.setManaged(true);
            }
            return;
        }

        contenuArea.setText(contenu);
    }

    private void setupPdfDropZone() {
        if (pdfDropZone == null) {
            return;
        }

        boolean canUploadPdf = AccessControl.canManageContent(role);
        pdfDropZone.setVisible(canUploadPdf);
        pdfDropZone.setManaged(canUploadPdf);

        if (!canUploadPdf) {
            return;
        }

        pdfDropZone.setOnDragOver(this::handlePdfDragOver);
        pdfDropZone.setOnDragEntered(e -> pdfDropZone.getStyleClass().add("drag-over"));
        pdfDropZone.setOnDragExited(e -> pdfDropZone.getStyleClass().remove("drag-over"));
        pdfDropZone.setOnDragDropped(this::handlePdfDropped);
    }

    private void handlePdfDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db != null && db.hasFiles() && hasPdf(db)) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handlePdfDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        try {
            if (db != null && db.hasFiles() && hasPdf(db)) {
                File pdf = firstPdf(db);
                if (pdf != null) {
                    Path stored = storePdf(pdf.toPath());
                    String title = stripExtension(pdf.getName());
                    int profId = AppSession.getCurrentUser() == null ? 0 : AppSession.getCurrentUser().getId();
                    leconService.create(new Lecon(0, title, PDF_PREFIX + stored.toString(), matiere == null ? 0 : matiere.getId()), profId);
                    loadLecons();
                    success = true;
                }
            }
        } catch (Exception ex) {
            if (errorLabel != null) {
                errorLabel.setText("Erreur: " + ex.getMessage());
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private static boolean hasPdf(Dragboard db) {
        for (File f : db.getFiles()) {
            if (f != null && f.getName() != null && f.getName().toLowerCase().endsWith(".pdf")) {
                return true;
            }
        }
        return false;
    }

    private static File firstPdf(Dragboard db) {
        for (File f : db.getFiles()) {
            if (f != null && f.getName() != null && f.getName().toLowerCase().endsWith(".pdf")) {
                return f;
            }
        }
        return null;
    }

    private static String stripExtension(String name) {
        if (name == null) return "Cours";
        int idx = name.lastIndexOf('.');
        if (idx <= 0) return name;
        return name.substring(0, idx);
    }

    private static Path storePdf(Path source) throws IOException {
        Path baseDir = Paths.get(System.getProperty("user.home"), ".smartpath", "cours");
        Files.createDirectories(baseDir);

        String fileName = source.getFileName() == null ? "cours.pdf" : source.getFileName().toString();
        String safeName = (System.currentTimeMillis() + "_" + fileName).replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dest = baseDir.resolve(safeName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    @FXML
    public void handleOpenPdf() {
        if (selectedPdfPath == null || selectedPdfPath.isBlank()) {
            return;
        }
        try {
            File file = new File(selectedPdfPath);
            if (!file.exists()) {
                new Alert(Alert.AlertType.WARNING, "Fichier introuvable: " + selectedPdfPath).showAndWait();
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                new Alert(Alert.AlertType.WARNING, "Ouverture de PDF non supportée sur ce système.").showAndWait();
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}
