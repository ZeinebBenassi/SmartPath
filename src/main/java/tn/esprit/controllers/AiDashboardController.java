package tn.esprit.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tn.esprit.entity.Grade;
import tn.esprit.entity.User;
import tn.esprit.services.AiRecommendationService;
import tn.esprit.services.PdfTranslationService;
import tn.esprit.utils.MyDatabase;
import tn.esprit.utils.feature_cours_et_quiz.AppSession;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AiDashboardController {

    @FXML private TableView<Grade> gradesTable;
    @FXML private TableColumn<Grade, String> colSubject;
    @FXML private TableColumn<Grade, Double> colScore;
    @FXML private TextArea aiOutputArea;
    @FXML private TextField pdfPathField;
    @FXML private ComboBox<String> langCombo;
    @FXML private Label transStatusLabel;
    @FXML private Label gradeCountLabel;
    @FXML private Label linkTopicLabel;
    @FXML private Button youtubeBtn;
    @FXML private Button udemyBtn;
    @FXML private Button courseraBtn;

    private final AiRecommendationService aiService = new AiRecommendationService();
    private final PdfTranslationService pdfService = new PdfTranslationService();
    private final ObservableList<Grade> gradesList = FXCollections.observableArrayList();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private File selectedPdf;
    private String currentLearningTopic = "study skills";

    @FXML
    public void initialize() {
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        gradesTable.setItems(gradesList);

        langCombo.setItems(FXCollections.observableArrayList("English", "French", "Arabic"));
        langCombo.getSelectionModel().selectFirst();

        gradesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.getSubject() != null && !newValue.getSubject().isBlank()) {
                currentLearningTopic = newValue.getSubject().trim();
                refreshLearningLinksUi();
            }
        });
        
        loadGradesFromDb();
        refreshLearningLinksUi();
    }

    @FXML
    public void loadGradesFromDb() {
        User current = AppSession.getCurrentUser();
        // Fallback to static user from dashboard if AppSession is empty
        if (current == null) current = DashboardEtudiantController.getCurrentUser();
        
        if (current == null) {
            System.err.println("[AI Dashboard] Critical Error: User not found in any session.");
            return;
        }

        gradesList.clear();
        String sql = "SELECT scores FROM quiz_result WHERE etudiant_id = ? ORDER BY created_at DESC LIMIT 10";
        
        try (Connection cnx = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            
            ps.setInt(1, current.getId());
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String rawScores = rs.getString("scores");
                gradesList.addAll(parseScores(rawScores, current.getId()));
            }
            gradeCountLabel.setText(gradesList.size() + " Grades Loaded");
            chooseLearningTopic();
            refreshLearningLinksUi();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleAiAnalyze() {
        if (gradesList.isEmpty()) { showAlert("No grades found. Pass a quiz first."); return; }
        aiOutputArea.setText("Asking Advisor Grok AI...");
        new Thread(() -> {
            try {
                String res = aiService.getRecommendations(new ArrayList<>(gradesList));
                javafx.application.Platform.runLater(() -> {
                    aiOutputArea.setText(res);
                    chooseLearningTopic();
                    refreshLearningLinksUi();
                });
            } catch (Exception e) { javafx.application.Platform.runLater(() -> aiOutputArea.setText("Error: " + e.getMessage())); }
        }).start();
    }

    @FXML
    public void handleOpenYouTube() {
        openLearningLink("https://www.youtube.com/results?search_query=", currentLearningTopic + " tutorial");
    }

    @FXML
    public void handleOpenUdemy() {
        openLearningLink("https://www.udemy.com/courses/search/?q=", currentLearningTopic + " course");
    }

    @FXML
    public void handleOpenCoursera() {
        openLearningLink("https://www.coursera.org/search?query=", currentLearningTopic + " course");
    }

    @FXML
    public void handleSelectPdf() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        selectedPdf = chooser.showOpenDialog(gradesTable.getScene().getWindow());
        if (selectedPdf != null) {
            pdfPathField.setText(selectedPdf.getAbsolutePath());
            transStatusLabel.setText("Ready to translate selected PDF.");
        }
    }

    @FXML
    public void handleTranslate() {
        if (selectedPdf == null) {
            showAlert("Please choose a PDF first.");
            return;
        }

        String lang = langCombo.getValue();
        if (lang == null || lang.isBlank()) {
            showAlert("Please choose a target language.");
            return;
        }

        transStatusLabel.setText("Translating to " + lang + "...");
        new Thread(() -> {
            try {
                String text = pdfService.extractText(selectedPdf);
                Path downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");
                if (!Files.exists(downloadDir)) {
                    downloadDir = Paths.get(System.getProperty("user.home"), "Desktop");
                }
                Files.createDirectories(downloadDir);

                String fileName = "translated_" + stripPdfExtension(selectedPdf.getName()) + "_" + lang + ".pdf";
                Path outputPath = downloadDir.resolve(fileName);
                File savedFile = pdfService.translateAndSave(text, lang, outputPath.toString());
                javafx.application.Platform.runLater(() -> {
                    transStatusLabel.setText("Saved: " + savedFile.getAbsolutePath());
                    showAlert("Translated PDF saved to: " + savedFile.getAbsolutePath());
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    transStatusLabel.setText("Error: " + e.getMessage());
                    showAlert("Translation failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showAlert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }

    private void chooseLearningTopic() {
        if (gradesList == null || gradesList.isEmpty()) {
            currentLearningTopic = "study skills";
            return;
        }

        Grade selected = gradesTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getSubject() != null && !selected.getSubject().isBlank()) {
            currentLearningTopic = selected.getSubject().trim();
            return;
        }

        Grade weakest = gradesList.stream()
                .min(Comparator.comparingDouble(Grade::getScore))
                .orElse(null);
        if (weakest != null && weakest.getSubject() != null && !weakest.getSubject().isBlank()) {
            currentLearningTopic = weakest.getSubject().trim();
        }
    }

    private void refreshLearningLinksUi() {
        String topic = (currentLearningTopic == null || currentLearningTopic.isBlank())
                ? "study skills"
                : currentLearningTopic;

        if (linkTopicLabel != null) {
            linkTopicLabel.setText("Topic: " + topic);
        }

        boolean hasGrades = gradesList != null && !gradesList.isEmpty();
        if (youtubeBtn != null) youtubeBtn.setDisable(!hasGrades);
        if (udemyBtn != null) udemyBtn.setDisable(!hasGrades);
        if (courseraBtn != null) courseraBtn.setDisable(!hasGrades);
    }

    private void openLearningLink(String baseUrl, String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create(baseUrl + encoded);
            if (!Desktop.isDesktopSupported()) {
                showAlert("Desktop browser is not supported on this system.");
                return;
            }
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
            showAlert("Could not open link: " + e.getMessage());
        }
    }

    private String stripPdfExtension(String name) {
        if (name == null) {
            return "document";
        }
        return name.toLowerCase().endsWith(".pdf") ? name.substring(0, name.length() - 4) : name;
    }

    private List<Grade> parseScores(String rawScores, int userId) {
        List<Grade> parsed = new ArrayList<>();
        if (rawScores == null || rawScores.isBlank()) {
            return parsed;
        }

        String trimmed = rawScores.trim();
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.isObject()) {
                node.fields().forEachRemaining(entry -> addGrade(parsed, entry.getKey(), entry.getValue().asDouble(), userId));
                return parsed;
            }
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isObject()) {
                        String subject = item.path("subject").asText(item.path("name").asText("Item"));
                        double score = item.path("score").asDouble(item.path("value").asDouble(0.0));
                        addGrade(parsed, subject, score, userId);
                    }
                }
                return parsed;
            }
        } catch (IOException ignored) {
            // Fall back to the legacy format below.
        }

        String legacy = trimmed.replace("{", "").replace("}", "").replace("[", "").replace("]", "");
        for (String pair : legacy.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                try {
                    String subject = parts[0].replace('"', ' ').trim();
                    double score = Double.parseDouble(parts[1].replace('"', ' ').trim());
                    addGrade(parsed, subject, score, userId);
                } catch (Exception ignored) {
                }
            }
        }

        return parsed;
    }

    private void addGrade(List<Grade> grades, String subject, double score, int userId) {
        if (subject == null || subject.isBlank()) {
            return;
        }
        grades.add(new Grade(subject.trim(), score, userId));
    }
}
