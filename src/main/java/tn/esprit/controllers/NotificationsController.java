package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.esprit.entity.Notification;
import tn.esprit.services.NotificationService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller du panneau de notifications admin.
 * Affiché dans le contentArea du DashboardAdmin.
 */
public class NotificationsController implements Initializable {

    @FXML private VBox   vboxNotifications;
    @FXML private Label  lblCount;
    @FXML private Button btnMarkAll;

    private final NotificationService notifService = new NotificationService();

    /** Callback déclenché après "Tout marquer lu" pour rafraîchir la cloche. */
    private Consumer<Integer> onUnreadCountChanged;

    public void setOnUnreadCountChanged(Consumer<Integer> cb) {
        this.onUnreadCountChanged = cb;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadNotifications();
    }

    // ── Chargement ────────────────────────────────────────────────────────

    private void loadNotifications() {
        List<Notification> list = notifService.findAll();
        int unread = (int) list.stream().filter(n -> !n.isRead()).count();

        if (lblCount != null)
            lblCount.setText("Notifications (" + unread + " non lues)");

        vboxNotifications.getChildren().clear();

        if (list.isEmpty()) {
            Label empty = new Label("✅  Aucune notification pour le moment.");
            empty.setStyle("-fx-font-size:14px; -fx-text-fill:#94A3B8; -fx-padding:30 0;");
            vboxNotifications.getChildren().add(empty);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Notification n : list) {
            vboxNotifications.getChildren().add(buildCard(n, fmt));
        }
    }

    // ── Carte notification ────────────────────────────────────────────────

    private HBox buildCard(Notification n, DateTimeFormatter fmt) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));

        String bgColor = n.isRead() ? "#F8FAFC" : "#EFF6FF";
        String border  = n.isRead() ? "#E2E8F0" : "#BFDBFE";
        card.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-width:0 0 0 4;" +
            "-fx-border-radius:0 12 12 0;" +
            "-fx-background-radius:0 12 12 0;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);"
        );

        // Icône
        Label icon = new Label(n.isRead() ? "📋" : "🔔");
        icon.setStyle("-fx-font-size:22px;");

        // Contenu texte
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label msg = new Label(n.getMessage());
        msg.setStyle("-fx-font-size:13px; -fx-font-weight:" + (n.isRead() ? "normal" : "bold")
                + "; -fx-text-fill:#1E293B; -fx-wrap-text:true;");
        msg.setWrapText(true);

        Label time = new Label("🕒 " + (n.getCreatedAt() != null ? n.getCreatedAt().format(fmt) : "—"));
        time.setStyle("-fx-font-size:11px; -fx-text-fill:#94A3B8;");

        content.getChildren().addAll(msg, time);

        // Boutons action
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        if (!n.isRead()) {
            Button btnRead = new Button("✔ Lu");
            btnRead.setStyle(
                "-fx-background-color:#3B82F6; -fx-text-fill:white; -fx-font-size:11px;" +
                "-fx-background-radius:8; -fx-padding:5 12; -fx-cursor:hand;"
            );
            btnRead.setOnAction(e -> {
                notifService.markAsRead(n.getId());
                refreshAfterAction();
            });
            actions.getChildren().add(btnRead);
        }

        Button btnDel = new Button("🗑");
        btnDel.setStyle(
            "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626; -fx-font-size:11px;" +
            "-fx-background-radius:8; -fx-padding:5 10; -fx-cursor:hand;"
        );
        btnDel.setOnAction(e -> {
            notifService.delete(n.getId());
            refreshAfterAction();
        });
        actions.getChildren().add(btnDel);

        card.getChildren().addAll(icon, content, actions);
        return card;
    }

    // ── Actions ──────────────────────────────────────────────────────────

    @FXML
    private void handleMarkAll() {
        notifService.markAllAsRead();
        refreshAfterAction();
    }

    private void refreshAfterAction() {
        loadNotifications();
        if (onUnreadCountChanged != null)
            onUnreadCountChanged.accept(notifService.countUnread());
    }
}
