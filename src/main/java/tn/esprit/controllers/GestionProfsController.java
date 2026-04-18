package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entity.Prof;
import tn.esprit.services.AdminProfService;

import java.util.Optional;

public class GestionProfsController {

    @FXML private TableView<Prof>           profsTable;
    @FXML private TableColumn<Prof, String> colNom;
    @FXML private TableColumn<Prof, String> colPrenom;
    @FXML private TableColumn<Prof, String> colEmail;
    @FXML private TableColumn<Prof, String> colSpecialite;
    @FXML private TableColumn<Prof, String> colCIN;
    @FXML private TableColumn<Prof, String> colTelephone;
    @FXML private TableColumn<Prof, Void>   colActions;
    @FXML private TextField                 searchField;
    @FXML private Label                     countLabel;

    private final AdminProfService        service = new AdminProfService();
    private final ObservableList<Prof>    data    = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        if (searchField != null) searchField.setOnKeyReleased(e -> filterData());
        setupActionColumn();
        loadData();
    }

    private void setupColumns() {
        if (colNom        != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom     != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colEmail      != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colSpecialite != null) colSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));
        if (colCIN        != null) colCIN.setCellValueFactory(new PropertyValueFactory<>("cin"));
        if (colTelephone  != null) colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
    }

    private void setupActionColumn() {
        if (colActions == null) return;
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = makeBtn("✏ Modifier",  "#2563eb");
            private final Button btnDel  = makeBtn("🗑 Supprimer", "#dc2626");
            private final HBox   box     = new HBox(6, btnEdit, btnDel);
            { box.setStyle("-fx-alignment:CENTER;");
                btnEdit.setOnAction(e -> openForm(getTableRow().getItem(), false));
                btnDel .setOnAction(e -> confirmDelete(getTableRow().getItem())); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow()==null || getTableRow().getItem()==null ? null : box);
            }
        });
    }

    private Button makeBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11;-fx-padding:4 10;-fx-cursor:hand;");
        return b;
    }

    public void loadData() {
        try {
            data.setAll(service.getAll());
            if (profsTable != null) profsTable.setItems(data);
            updateCount();
        } catch (Exception e) { showError("Chargement", e.getMessage()); }
    }

    private void filterData() {
        String q = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (q.isEmpty()) { if (profsTable != null) profsTable.setItems(data); updateCount(); return; }
        ObservableList<Prof> f = FXCollections.observableArrayList();
        for (Prof p : data)
            if ((p.getNom()       !=null && p.getNom()      .toLowerCase().contains(q)) ||
                    (p.getPrenom()    !=null && p.getPrenom()   .toLowerCase().contains(q)) ||
                    (p.getEmail()     !=null && p.getEmail()    .toLowerCase().contains(q)) ||
                    (p.getSpecialite()!=null && p.getSpecialite().toLowerCase().contains(q))) f.add(p);
        if (profsTable != null) profsTable.setItems(f);
        updateCount();
    }

    private void updateCount() {
        if (countLabel!=null && profsTable!=null)
            countLabel.setText(profsTable.getItems().size() + " professeur(s)");
    }

    @FXML public void handleAjouter() { openForm(null, true); }

    private void openForm(Prof prof, boolean creation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/interfaces/ProfForm.fxml"));
            Parent root = loader.load();
            ProfFormController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle(creation ? "Ajouter un professeur" : "Modifier le professeur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            if (creation) ctrl.initCreate(this);
            else          ctrl.initEdit(prof, this);
            stage.showAndWait();
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    private void confirmDelete(Prof prof) {
        if (prof == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Supprimer"); a.setHeaderText(null);
        a.setContentText("Supprimer " + prof.getPrenom() + " " + prof.getNom() + " ?");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try { service.supprimer(prof.getId()); loadData(); }
            catch (Exception e) { showError("Erreur suppression", e.getMessage()); }
        }
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (profsTable != null) profsTable.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String t, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}