package tn.esprit.controllers.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Question;
import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.services.feature_cours_et_quiz.QuestionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.File;

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import java.util.concurrent.CompletableFuture;
import java.awt.Desktop;

public class QuizPassController implements NavigableController {
    @FXML private Label quizTitle;
    @FXML private Label quizMeta;
    @FXML private Label errorLabel;
    @FXML private VBox questionsBox;
    @FXML private Button finishBtn;

    private final QuestionService questionService = new QuestionService();
    private final tn.esprit.services.ChatbotService chatbotService = new tn.esprit.services.ChatbotService();

    private AppShellController appShell;
    private Quiz quiz;
    private final List<ToggleGroup> toggleGroups = new ArrayList<>();

    @Override
    public void setAppShell(AppShellController appShell) {
        this.appShell = appShell;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        if (quizTitle != null) {
            quizTitle.setText(quiz == null ? "▶ Passer le quiz" : "▶ " + quiz.getTitre());
        }
        if (quizMeta != null && quiz != null) {
            quizMeta.setText("⏱ " + quiz.getDuree() + " min  •  Matière ID: " + quiz.getMatiereId());
        }
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        loadQuestions();
    }

    @FXML
    public void handleBack() {
        if (appShell != null) {
            appShell.showQuiz();
        }
    }

    @FXML
    public void handleFinish() {
        int total = toggleGroups.size();
        StringBuilder answersBuilder = new StringBuilder();
        
        for (int i = 0; i < total; i++) {
            ToggleGroup g = toggleGroups.get(i);
            String questionText = ((Label)((VBox)questionsBox.getChildren().get(i)).getChildren().get(0)).getText();
            RadioButton selected = (RadioButton) g.getSelectedToggle();
            String answer = selected != null ? selected.getText() : "Pas de réponse";
            answersBuilder.append("Question ").append(i+1).append(": ").append(questionText)
                         .append("\nRéponse de l'étudiant: ").append(answer).append("\n\n");
        }
        
        finishBtn.setDisable(true);
        finishBtn.setText("🤖 Correction par l'IA...");

        String prompt = "Tu es un professeur. Corrige ce quiz sur '" + quiz.getTitre() + "'. " +
                "Voici les réponses de l'étudiant :\n\n" + answersBuilder.toString() +
                "\n1. Donne un score global sur " + total + "." +
                "\n2. Explique brièvement les erreurs s'il y en a." +
                "\n3. Propose EXACTEMENT 3 liens de recommandation au format suivant :\n" +
                "YOUTUBE: [nom](lien)\n" +
                "UDEMY: [nom](lien)\n" +
                "COURSERA: [nom](lien)";

        chatbotService.askAsync(prompt)
            .thenAccept(response -> {
                javafx.application.Platform.runLater(() -> {
                    finishBtn.setDisable(false);
                    finishBtn.setText("✅ Terminer");

                    VBox resultBox = new VBox(20);
                    resultBox.getStyleClass().add("sp-card");
                    resultBox.setStyle("-fx-border-color: #2563EB; -fx-border-width: 2; -fx-padding: 30;");

                    Label resTitle = new Label("🤖 Correction de l'IA");
                    resTitle.getStyleClass().add("sp-card-title");
                    resTitle.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 22;");

                    // Split response to find links
                    String correctionText = response;
                    List<HyperlinkButton> buttons = extractLinks(response);
                    
                    // Remove links from text display
                    for (HyperlinkButton hb : buttons) {
                        correctionText = correctionText.replace(hb.originalLine, "");
                    }

                    Label correctionLabel = new Label(correctionText.trim());
                    correctionLabel.setWrapText(true);
                    correctionLabel.getStyleClass().add("sp-card-desc");

                    Label recTitle = new Label("📚 Recommandations personnalisées :");
                    recTitle.getStyleClass().add("sp-card-title");

                    FlowPane linksPane = new FlowPane(10, 10);
                    for (HyperlinkButton hb : buttons) {
                        String emoji = "";
                        if (hb.platform.equalsIgnoreCase("YOUTUBE")) emoji = "📺 ";
                        else if (hb.platform.equalsIgnoreCase("UDEMY")) emoji = "🎓 ";
                        else if (hb.platform.equalsIgnoreCase("COURSERA")) emoji = "📚 ";
                        
                        Button btn = new Button(emoji + hb.platform + ": " + hb.name);
                        btn.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
                        btn.setOnAction(e -> openLink(hb.url));
                        linksPane.getChildren().add(btn);
                    }

                    Button close = new Button("Fermer et quitter");
                    close.getStyleClass().addAll("sp-btn", "sp-btn-primary");
                    close.setMaxWidth(Double.MAX_VALUE);
                    close.setOnAction(e -> {
                        if (appShell != null) appShell.showQuiz();
                    });

                    Button downloadPdf = new Button("📥 Télécharger mon résultat (PDF)");
                    downloadPdf.getStyleClass().addAll("sp-btn", "sp-btn-secondary");
                    downloadPdf.setMaxWidth(Double.MAX_VALUE);
                    downloadPdf.setOnAction(e -> generateResultPdf(response, answersBuilder.toString()));

                    resultBox.getChildren().addAll(resTitle, correctionLabel, recTitle, linksPane, downloadPdf, close);
                    questionsBox.getChildren().add(0, resultBox);
                    
                    // Clear questions below to focus on result
                    // questionsBox.getChildren().remove(1, questionsBox.getChildren().size());
                    
                    // Scroll to top
                    questionsBox.getParent().layout();
                });
            });
    }

    private List<HyperlinkButton> extractLinks(String text) {
        List<HyperlinkButton> links = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String upperLine = line.toUpperCase();
            if (upperLine.contains("YOUTUBE:") || upperLine.contains("UDEMY:") || upperLine.contains("COURSERA:")) {
                try {
                    String platform = line.split(":")[0].trim();
                    String name;
                    String url;

                    if (line.contains("[") && line.contains("]")) {
                        // Standard Markdown format: PLATFORM: [name](url)
                        name = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                        url = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
                    } else if (line.contains("(") && line.contains(")")) {
                        // AI sometimes outputs: PLATFORM: name (url)
                        name = line.substring(line.indexOf(":") + 1, line.indexOf("(")).trim();
                        url = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                    } else {
                        // Fallback: PLATFORM: name url
                        String rest = line.substring(line.indexOf(":") + 1).trim();
                        int lastSpace = rest.lastIndexOf(" ");
                        if (lastSpace != -1) {
                            name = rest.substring(0, lastSpace).trim();
                            url = rest.substring(lastSpace).trim();
                        } else {
                            name = platform;
                            url = rest;
                        }
                    }
                    
                    if (url.startsWith("http")) {
                        links.add(new HyperlinkButton(platform, name, url, line));
                    }
                } catch (Exception ignored) {}
            }
        }
        return links;
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le lien: " + e.getMessage()).showAndWait();
        }
    }

    private record HyperlinkButton(String platform, String name, String url, String originalLine) {}

    private void generateResultPdf(String aiCorrection, String studentAnswers) {
        Document document = new Document();
        try {
            String fileName = "Resultat_Quiz_" + quiz.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.BLUE);
            Paragraph title = new Paragraph("SmartPath - Résultat du Quiz", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Quiz Info
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
            document.add(new Paragraph("Quiz : " + quiz.getTitre(), subTitleFont));
            document.add(new Paragraph("Date : " + new java.util.Date().toString()));
            document.add(new Paragraph("Matière ID : " + quiz.getMatiereId()));
            document.add(new Paragraph("\n" + "─".repeat(50) + "\n"));

            // AI Correction
            document.add(new Paragraph("🤖 Correction de l'IA :", subTitleFont));
            document.add(new Paragraph(aiCorrection));
            document.add(new Paragraph("\n" + "─".repeat(50) + "\n"));

            // Detailed Answers
            document.add(new Paragraph("📝 Vos réponses détaillées :", subTitleFont));
            document.add(new Paragraph(studentAnswers));

            document.close();
            new Alert(Alert.AlertType.INFORMATION, "PDF généré avec succès : " + fileName).showAndWait();
            
            // Open the file
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(fileName));
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la génération du PDF : " + e.getMessage()).showAndWait();
        }
    }

    private void loadQuestions() {
        if (questionsBox == null) {
            return;
        }
        questionsBox.getChildren().clear();
        toggleGroups.clear();

        if (quiz == null) {
            return;
        }

        try {
            List<Question> questions = questionService.getByTestId(quiz.getId());
            if (questions.isEmpty()) {
                // ... same as before ...
                VBox empty = new VBox(10);
                empty.getStyleClass().add("sp-card");
                Label title = new Label("Aucune question");
                title.getStyleClass().add("sp-card-title");
                Label desc = new Label("Ce quiz ne contient pas encore de questions.");
                desc.getStyleClass().add("sp-card-desc");
                desc.setWrapText(true);
                empty.getChildren().addAll(title, desc);
                questionsBox.getChildren().add(empty);
                return;
            }

            int i = 1;
            for (Question q : questions) {
                VBox card = new VBox(10);
                card.getStyleClass().add("sp-card");

                Label qTitle = new Label("Q" + i + ". " + (q.getText() == null ? "" : q.getText()));
                qTitle.getStyleClass().add("sp-card-title");
                qTitle.setWrapText(true);

                ToggleGroup group = new ToggleGroup();
                RadioButton rbVrai = new RadioButton("Vrai");
                rbVrai.setToggleGroup(group);
                rbVrai.getStyleClass().add("sp-card-meta");
                
                RadioButton rbFaux = new RadioButton("Faux");
                rbFaux.setToggleGroup(group);
                rbFaux.getStyleClass().add("sp-card-meta");

                HBox options = new HBox(20, rbVrai, rbFaux);
                
                toggleGroups.add(group);
                card.getChildren().addAll(qTitle, options);
                questionsBox.getChildren().add(card);
                i++;
            }
        } catch (SQLException e) {
            if (errorLabel != null) {
                errorLabel.setText("Erreur: " + e.getMessage());
            }
        }
    }
}
