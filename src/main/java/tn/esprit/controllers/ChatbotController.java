package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import tn.esprit.services.ChatbotService;

/**
 * Controller for the floating chatbot popup window (Chatbot.fxml).
 */
public class ChatbotController {

    @FXML private VBox    messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  inputField;
    @FXML private Button     sendButton;
    @FXML private Button     btnClose;

    private final ChatbotService chatbotService = new ChatbotService();

    @FXML
    public void initialize() {
        // Auto-scroll when new messages are added
        messagesContainer.heightProperty().addListener(
                (obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));

        // Allow Enter key to send
        inputField.setOnAction(e -> handleSend());

        // Welcome message
        addBotMessage("👋 Hi! I'm SmartPath Assistant.\nAsk me anything about programming, algorithms, databases, networking, AI, and more!");
    }

    @FXML
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        // Show user bubble immediately
        addUserMessage(text);
        inputField.clear();

        // Disable input while waiting
        setInputEnabled(false);

        // Typing indicator
        Label typing = addBotMessage("⏳ Typing...");

        // Call API on background thread
        chatbotService.askAsync(text).thenAccept(response ->
            Platform.runLater(() -> {
                messagesContainer.getChildren().remove(typing.getParent().getParent());
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

    /* ─────────────────────────────────────────── helpers ─────────── */

    private Label addBotMessage(String text) {
        Label lbl = createLabel(text, "#1e293b");
        HBox  row = bubbleRow(lbl, "#f1f5f9", "#e2e8f0", Pos.CENTER_LEFT);
        messagesContainer.getChildren().add(row);
        return lbl;
    }

    private void addUserMessage(String text) {
        Label lbl = createLabel(text, "#ffffff");
        HBox  row = bubbleRow(lbl, "#2563eb", "#1d4ed8", Pos.CENTER_RIGHT);
        messagesContainer.getChildren().add(row);
    }

    private Label createLabel(String text, String textColor) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(260);
        lbl.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        return lbl;
    }

    private HBox bubbleRow(Label content, String bgColor, String borderColor, Pos alignment) {
        VBox bubble = new VBox(content);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );
        bubble.setMaxWidth(280);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(4, 12, 4, 12));
        row.setAlignment(alignment);
        return row;
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }
}
