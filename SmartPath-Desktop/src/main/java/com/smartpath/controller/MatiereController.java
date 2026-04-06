package com.smartpath.controller;

import com.smartpath.model.Matiere;
import com.smartpath.service.MatiereService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MatiereController implements Initializable {
    @FXML private TableView<Matiere> tableView;
    @FXML private TableColumn<Matiere, Integer> colId;
    @FXML private TableColumn<Matiere, String> colTitre;
    @FXML private TableColumn<Matiere, String> colDesc;
    @FXML private TextField txtTitre;
    @FXML private TextField txtDesc;

    private final MatiereService service = new MatiereService();
    private final ObservableList<Matiere> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        loadData();
        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) {
                        txtTitre.setText(selected.getTitre());
                        txtDesc.setText(selected.getDescription());
                    }
                });
    }

    private void loadData() {
        try { data.setAll(service.getAll()); tableView.setItems(data); }
        catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML public void handleAdd() {
        try {
            service.create(new Matiere(0, txtTitre.getText(), txtDesc.getText()));
            clear(); loadData();
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML public void handleUpdate() {
        Matiere m = tableView.getSelectionModel().getSelectedItem();
        if (m == null) return;
        try {
            m.setTitre(txtTitre.getText());
            m.setDescription(txtDesc.getText());
            service.update(m); clear(); loadData();
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML public void handleDelete() {
        Matiere m = tableView.getSelectionModel().getSelectedItem();
        if (m == null) return;
        try { service.delete(m.getId()); clear(); loadData(); }
        catch (SQLException e) { showError(e.getMessage()); }
    }

    private void clear() { txtTitre.clear(); txtDesc.clear(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
}