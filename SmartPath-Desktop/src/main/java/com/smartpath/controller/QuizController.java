package com.smartpath.controller;

import com.smartpath.model.Quiz;
import com.smartpath.service.QuizService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class QuizController implements Initializable {
    @FXML private TableView<Quiz> tableView;
    @FXML private TableColumn<Quiz, Integer> colId;
    @FXML private TableColumn<Quiz, String>  colTitre;
    @FXML private TableColumn<Quiz, String>  colContenu;
    @FXML private TableColumn<Quiz, Integer> colDuree;
    @FXML private TextField txtTitre;
    @FXML private TextField txtContenu;
    @FXML private TextField txtDuree;
    @FXML private TextField txtMatiereId;

    private final QuizService service = new QuizService();
    private final ObservableList<Quiz> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        loadData();
        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) {
                        txtTitre.setText(selected.getTitre());
                        txtContenu.setText(selected.getContenu());
                        txtDuree.setText(String.valueOf(selected.getDuree()));
                        txtMatiereId.setText(String.valueOf(selected.getMatiereId()));
                    }
                });
    }

    private void loadData() {
        try { data.setAll(service.getAll()); tableView.setItems(data); }
        catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML public void handleAdd() {
        try {
            Quiz q = new Quiz(0,
                    txtTitre.getText(),
                    txtContenu.getText(),
                    Integer.parseInt(txtDuree.getText().isEmpty() ? "0" : txtDuree.getText()),
                    Integer.parseInt(txtMatiereId.getText().isEmpty() ? "0" : txtMatiereId.getText())
            );
            service.create(q);
            clear(); loadData();
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML public void handleUpdate() {
        Quiz q = tableView.getSelectionModel().getSelectedItem();
        if (q == null) { showError("Selectionnez un quiz!"); return; }
        try {
            q.setTitre(txtTitre.getText());
            q.setContenu(txtContenu.getText());
            q.setDuree(Integer.parseInt(txtDuree.getText().isEmpty() ? "0" : txtDuree.getText()));
            q.setMatiereId(Integer.parseInt(txtMatiereId.getText().isEmpty() ? "0" : txtMatiereId.getText()));
            service.update(q);
            clear(); loadData();
        } catch (SQLException e) { showError(e.getMessage()); }
    }

    @FXML public void handleDelete() {
        Quiz q = tableView.getSelectionModel().getSelectedItem();
        if (q == null) { showError("Selectionnez un quiz!"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer le quiz: " + q.getTitre() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try { service.delete(q.getId()); clear(); loadData(); }
                catch (SQLException e) { showError(e.getMessage()); }
            }
        });
    }

    private void clear() {
        txtTitre.clear(); txtContenu.clear();
        txtDuree.clear(); txtMatiereId.clear();
    }
    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}