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
import tn.esprit.entity.Etudiant;
import tn.esprit.services.AdminEtudiantService;

import java.util.Optional;

public class GestionEtudiantsController {

    @FXML private TableView<Etudiant>           etudiantsTable;
    @FXML private TableColumn<Etudiant, String> colNom;
    @FXML private TableColumn<Etudiant, String> colPrenom;
    @FXML private TableColumn<Etudiant, String> colEmail;
    @FXML private TableColumn<Etudiant, String> colNiveau;
    @FXML private TableColumn<Etudiant, String> colCIN;
    @FXML private TableColumn<Etudiant, String> colTelephone;
    @FXML private TableColumn<Etudiant, String> colStatus;
    @FXML private TableColumn<Etudiant, Void>   colActions;
    @FXML private TextField                     searchField;
    @FXML private Label                         countLabel;

    private final AdminEtudiantService        service = new AdminEtudiantService();
    private final ObservableList<Etudiant>    data    = FXCollections.observableArrayList();

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
        if (colNiveau    != null) colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        if (colCIN       != null) colCIN.setCellValueFactory(new PropertyValueFactory<>("cin"));
        if (colTelephone != null) colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                    Etudiant e = (Etudiant) getTableRow().getItem();
                    String st = e.getStatus() != null ? e.getStatus() : "actif";
                    setText(st);
                    String bg   = "ban".equalsIgnoreCase(st) ? "#fee2e2" : "#dcfce7";
                    String color= "ban".equalsIgnoreCase(st) ? "#b91c1c" : "#15803d";
                    setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";-fx-font-weight:bold;-fx-background-radius:6;-fx-alignment:CENTER;");
                }
            });
        }
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
            if (etudiantsTable != null) etudiantsTable.setItems(data);
            updateCount();
        } catch (Exception e) { showError("Chargement", e.getMessage()); }
    }

    private void filterData() {
        String q = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (q.isEmpty()) { if (etudiantsTable != null) etudiantsTable.setItems(data); updateCount(); return; }
        ObservableList<Etudiant> f = FXCollections.observableArrayList();
        for (Etudiant e : data)
            if ((e.getNom()   !=null && e.getNom()   .toLowerCase().contains(q)) ||
                    (e.getPrenom()!=null && e.getPrenom().toLowerCase().contains(q)) ||
                    (e.getEmail() !=null && e.getEmail() .toLowerCase().contains(q))) f.add(e);
        if (etudiantsTable != null) etudiantsTable.setItems(f);
        updateCount();
    }

    private void updateCount() {
        if (countLabel!=null && etudiantsTable!=null)
            countLabel.setText(etudiantsTable.getItems().size() + " étudiant(s)");
    }

    @FXML public void handleAjouter() { openForm(null, true); }

    private void openForm(Etudiant etudiant, boolean creation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/interfaces/EtudiantForm.fxml"));
            Parent root = loader.load();
            EtudiantFormController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle(creation ? "Ajouter un étudiant" : "Modifier l'étudiant");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            if (creation) ctrl.initCreate(this);
            else          ctrl.initEdit(etudiant, this);
            stage.showAndWait();
        } catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    private void confirmDelete(Etudiant etudiant) {
        if (etudiant == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Supprimer"); a.setHeaderText(null);
        a.setContentText("Supprimer " + etudiant.getPrenom() + " " + etudiant.getNom() + " ?");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try { service.supprimer(etudiant.getId()); loadData(); }
            catch (Exception e) { showError("Erreur suppression", e.getMessage()); }
        }
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            if (etudiantsTable != null) etudiantsTable.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String t, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}