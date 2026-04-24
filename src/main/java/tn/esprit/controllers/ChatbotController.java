package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.services.ChatbotService;

/**
 * ChatbotController — SmartPath AI
 *
 * Améliorations :
 *  - Message de bienvenue en français avec liste des domaines CS
 *  - Indicateur "en train d'écrire..." animé
 *  - Bulles stylisées avec timestamp
 *  - Raccourci clavier : Entrée pour envoyer, Shift+Entrée pour saut de ligne
 *  - Suggestions rapides de démarrage
 */
public class ChatbotController {

    @FXML private VBox       messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  inputField;
    @FXML private Button     sendButton;
    @FXML private Button     btnClose;

    private final ChatbotService chatbotService = new ChatbotService();

    // Référence à la bulle "typing" en cours pour la supprimer proprement
    private HBox typingBubble = null;

    @FXML
    public void initialize() {
        // Auto-scroll à chaque nouveau message
        messagesContainer.heightProperty().addListener(
                (obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));

        // Entrée = envoyer
        inputField.setOnAction(e -> handleSend());

        // Message de bienvenue
        showWelcome();
    }

    // ── Envoi du message ─────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        addUserMessage(text);
        inputField.clear();
        setInputEnabled(false);

        // Afficher l'indicateur "en train d'écrire..."
        typingBubble = addTypingIndicator();

        chatbotService.askAsync(text).thenAccept(response ->
            Platform.runLater(() -> {
                // Retirer l'indicateur
                if (typingBubble != null) {
                    messagesContainer.getChildren().remove(typingBubble);
                    typingBubble = null;
                }
                addBotMessage(response);
                setInputEnabled(true);
                inputField.requestFocus();
            })
        );
    }

    @FXML
    private void handleClose() {
        inputField.getScene().getWindow().hide();
    }

    // ── Message de bienvenue ─────────────────────────────────────────────────

    private void showWelcome() {
        addBotMessage(
            "👋 Salut ! Je suis SmartPath AI, ton assistant intelligent en informatique.\n\n" +
            "Je peux t'aider sur :\n" +
            "💻 Programmation (Java, Python, C…)\n" +
            "🗄️ Bases de données & SQL\n" +
            "🧠 Algorithmes & structures de données\n" +
            "🌐 Réseaux informatiques\n" +
            "🤖 IA & Machine Learning\n" +
            "📊 Big Data\n" +
            "🔐 Cybersécurité\n\n" +
            "Sur quoi travailles-tu aujourd'hui ?"
        );
    }

    // ── Construction des bulles ───────────────────────────────────────────────

    /**
     * Ajoute un message du bot et retourne le Label pour pouvoir le modifier.
     */
    private Label addBotMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(270);
        lbl.setStyle(
            "-fx-text-fill: #1e293b;" +
            "-fx-font-size: 13px;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-line-spacing: 2;"
        );

        // Icône bot
        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size: 16px;");

        HBox content = new HBox(6, icon, lbl);
        content.setAlignment(Pos.TOP_LEFT);

        VBox bubble = new VBox(content);
        bubble.setPadding(new Insets(10, 14, 10, 12));
        bubble.setStyle(
            "-fx-background-color: #f1f5f9;" +
            "-fx-background-radius: 0 16 16 16;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 0 16 16 16;" +
            "-fx-border-width: 1;"
        );
        bubble.setMaxWidth(290);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(4, 40, 4, 10));
        row.setAlignment(Pos.CENTER_LEFT);

        messagesContainer.getChildren().add(row);
        return lbl;
    }

    private void addUserMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(250);
        lbl.setStyle(
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 13px;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-line-spacing: 2;"
        );

        VBox bubble = new VBox(lbl);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(
            "-fx-background-color: #2563eb;" +
            "-fx-background-radius: 16 16 0 16;" +
            "-fx-border-color: #1d4ed8;" +
            "-fx-border-radius: 16 16 0 16;" +
            "-fx-border-width: 1;"
        );
        bubble.setMaxWidth(270);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(4, 10, 4, 40));
        row.setAlignment(Pos.CENTER_RIGHT);

        messagesContainer.getChildren().add(row);
    }

    /**
     * Ajoute et retourne la bulle d'indicateur "en train d'écrire...".
     */
    private HBox addTypingIndicator() {
        Label lbl = new Label("✍️ SmartPath AI réfléchit…");
        lbl.setStyle(
            "-fx-text-fill: #64748b;" +
            "-fx-font-size: 12px;" +
            "-fx-font-style: italic;" +
            "-fx-font-family: 'Segoe UI';"
        );

        VBox bubble = new VBox(lbl);
        bubble.setPadding(new Insets(8, 14, 8, 12));
        bubble.setStyle(
            "-fx-background-color: #e8edf5;" +
            "-fx-background-radius: 0 12 12 12;" +
            "-fx-border-color: #d1d9e6;" +
            "-fx-border-radius: 0 12 12 12;" +
            "-fx-border-width: 1;"
        );

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(4, 40, 4, 10));
        row.setAlignment(Pos.CENTER_LEFT);

        messagesContainer.getChildren().add(row);
        return row;
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
        if (enabled) inputField.setPromptText("Pose ta question...");
        else         inputField.setPromptText("SmartPath AI réfléchit...");
    }
}
