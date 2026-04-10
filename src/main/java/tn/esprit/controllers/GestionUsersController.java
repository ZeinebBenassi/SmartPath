package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.User;
import services.UserService;

import java.util.List;

public class GestionUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colDate;
    @FXML private TableColumn<User, Void>   colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label countLabel;

    private UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Colonnes
        if (colId     != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colNom    != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colEmail  != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colRole   != null) colRole.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Filtre rôle
        if (roleFilter != null) {
            roleFilter.setItems(FXCollections.observableArrayList("Tous", "admin", "prof", "etudiant"));
            roleFilter.getSelectionModel().selectFirst();
        }

        // Colonne actions avec boutons
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button btnDel = new Button("Supprimer");
                {
                    btnDel.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand;");
                    btnDel.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                                "Supprimer " + u.getNom() + " ?", ButtonType.YES, ButtonType.NO);
                        confirm.showAndWait().ifPresent(btn -> {
                            if (btn == ButtonType.YES) {
                                userService.delete(u.getId());
                                loadUsers();
                            }
                        });
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btnDel);
                }
            });
        }

        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> list = userService.findAll();
            allUsers.setAll(list);
            if (usersTable != null) usersTable.setItems(allUsers);
            if (countLabel != null) countLabel.setText(list.size() + " utilisateurs");
        } catch (Exception e) {
            System.out.println("Erreur chargement users : " + e.getMessage());
        }
    }

    @FXML public void handleSearch() {
        String q = searchField.getText().toLowerCase();
        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User u : allUsers) {
            if (u.getNom().toLowerCase().contains(q) ||
                u.getPrenom().toLowerCase().contains(q) ||
                u.getEmail().toLowerCase().contains(q)) {
                filtered.add(u);
            }
        }
        usersTable.setItems(filtered);
        if (countLabel != null) countLabel.setText(filtered.size() + " utilisateurs");
    }

    @FXML public void handleFilter() {
        if (roleFilter == null) return;
        String selected = roleFilter.getValue();
        if (selected == null || selected.equals("Tous")) {
            usersTable.setItems(allUsers);
        } else {
            ObservableList<User> filtered = FXCollections.observableArrayList();
            for (User u : allUsers) {
                if (selected.equals(u.getType())) filtered.add(u);
            }
            usersTable.setItems(filtered);
            if (countLabel != null) countLabel.setText(filtered.size() + " utilisateurs");
        }
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

