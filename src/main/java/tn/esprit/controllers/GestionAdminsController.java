package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * CRUD dédié aux administrateurs.
 * Un admin est un User dont type = "admin" (pas de table enfant dédiée).
 */
public class GestionAdminsController {

    @FXML private TableView<User>           adminsTable;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colCIN;
    @FXML private TableColumn<User, String> colTelephone;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void>   colActions;

    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    private final UserService service = new UserService();
    private final ObservableList<User> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        if (searchField != null) searchField.setOnKeyReleased(e -> filterData());
        setupActionColumn();
        loadData();
    }

    private void setupColumns() {
        if (colNom       != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom    != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colEmail     != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colCIN       != null) colCIN.setCellValueFactory(new PropertyValueFactory<>("cin"));
        if (colTelephone != null) colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        if (colStatus    != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupActionColumn() {
        if (colActions == null) return;
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = makeBtn("✏ Modifier", "#2563eb");
            private final Button btnDel  = makeBtn("🗑 Supprimer", "#dc2626");
            private final HBox   box     = new HBox(6, btnEdit, btnDel);
            {
                box.setStyle("-fx-alignment: CENTER;");
                btnEdit.setOnAction(e -> openForm(getTableRow().getItem()));
                btnDel .setOnAction(e -> confirmDelete(getTableRow().getItem()));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : box);
            }
        });
    }

    private Button makeBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 6;"
                + " -fx-font-size: 11; -fx-padding: 4 10; -fx-cursor: hand;");
        return b;
    }

    public void loadData() {
        try {
            List<User> admins = service.findByType("admin");
            data.setAll(admins);
            if (adminsTable != null) adminsTable.setItems(data);
            updateCount();
        } catch (Exception e) {
            showError("Chargement", e.getMessage());
        }
    }

    private void filterData() {
        String q = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (q.isEmpty()) { if (adminsTable != null) adminsTable.setItems(data); updateCount(); return; }
        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User u : data) {
            if ((u.getNom()   != null && u.getNom()  .toLowerCase().contains(q)) ||
                (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                filtered.add(u);
        }
        if (adminsTable != null) adminsTable.setItems(filtered);
        updateCount();
    }

    private void updateCount() {
        if (countLabel != null && adminsTable != null)
            countLabel.setText(adminsTable.getItems().size() + " admin(s)");
    }

    @FXML
    public void handleAjouter() { openForm(null); }

    private void openForm(User admin) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/interfaces/UserForm.fxml"));
            Parent root = loader.load();
            UserFormController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle(admin == null ? "Ajouter un admin" : "Modifier l'admin");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            // Passe un GestionUsersController proxy pour rafraîchir
            if (admin == null) {
                ctrl.initializeForCreate(null);
                // Force type = admin
            } else {
                ctrl.initializeForEdit(admin, null);
            }
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void confirmDelete(User admin) {
        if (admin == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Supprimer admin");
        a.setHeaderText(null);
        a.setContentText("Supprimer l'administrateur " + admin.getNom() + " " + admin.getPrenom() + " ?");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                service.delete(admin.getId());
                showSuccess("L'administrateur a été supprimé.");
                loadData();
            } catch (Exception e) {
                showError("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    public void goBack() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (adminsTable != null) adminsTable.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String t, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
