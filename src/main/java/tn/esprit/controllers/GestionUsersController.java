package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import tn.esprit.entity.User;
import tn.esprit.services.UserService;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class GestionUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colDate;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label countLabel;
    @FXML private Button btnAjouter;

    private UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        setupActionButtons();
        loadUsers();
    }

    private void setupColumns() {
        if (colNom != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colEmail != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colRole != null) colRole.setCellValueFactory(new PropertyValueFactory<>("type"));

        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<User, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().isEmpty()) {
                        setText(null); setStyle("");
                    } else {
                        User user = getTableView().getItems().get(getIndex());
                        String status = user.getStatus();
                        if (status == null || status.trim().isEmpty()) status = "actif";
                        setText("ban".equalsIgnoreCase(status) ? "ban" : "actif");
                        setStyle("");
                    }
                }
            });
        }

        if (colDate != null) {
            colDate.setCellFactory(col -> new TableCell<User, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().isEmpty()) {
                        setText(null);
                    } else {
                        User user = getTableView().getItems().get(getIndex());
                        setText(user.getCreatedAt() != null ? dateFormat.format(user.getCreatedAt()) : "N/A");
                    }
                }
            });
        }
    }

    private void setupFilters() {
        if (roleFilter != null) {
            roleFilter.setItems(FXCollections.observableArrayList("Tous", "admin", "prof", "etudiant"));
            roleFilter.getSelectionModel().selectFirst();
            roleFilter.setOnAction(e -> applyFiltersAndSearch());
        }
        if (searchField != null) {
            searchField.setOnKeyReleased(e -> applyFiltersAndSearch());
        }
        setupColumnSorting();
    }

    private void setupColumnSorting() {
        if (colNom != null) colNom.setSortable(true);
        if (colPrenom != null) colPrenom.setSortable(true);
        if (colEmail != null) colEmail.setSortable(true);
        if (colRole != null) colRole.setSortable(true);
        if (usersTable != null) usersTable.getSortOrder().add(colNom);
    }

    private void setupActionButtons() {
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<User, Void>() {
                private final Button btnEdit = createIconButton();
                private final Button btnDel  = createIconButton();
                private final Button btnBan  = createIconButton();
                private final HBox hBox = new HBox(4, btnEdit, btnBan, btnDel);

                private Button createIconButton() {
                    Button btn = new Button();
                    btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
                    btn.setPrefWidth(32); btn.setPrefHeight(32);
                    btn.setMinWidth(32);  btn.setMinHeight(32);
                    btn.setMaxWidth(32);  btn.setMaxHeight(32);
                    return btn;
                }

                {
                    try {
                        Image editImg = new Image(getClass().getResourceAsStream("/images/modifier.png"));
                        ImageView editView = new ImageView(editImg);
                        editView.setFitHeight(16); editView.setFitWidth(16);
                        btnEdit.setGraphic(editView);
                        btnEdit.setTooltip(new Tooltip("Modifier"));
                    } catch (Exception e) { btnEdit.setText("M"); }

                    btnDel.setText("X");
                    btnDel.setTooltip(new Tooltip("Supprimer"));

                    btnEdit.setOnAction(e -> handleEdit(getTableRow().getItem()));
                    btnBan.setOnAction(e -> handleToggleBan(getTableRow().getItem()));
                    btnDel.setOnAction(e -> handleDelete(getTableRow().getItem()));

                    hBox.setStyle("-fx-alignment: CENTER; -fx-spacing: 4;");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    User user = getTableRow().getItem();
                    if (user != null) {
                        String status = user.getStatus();
                        if ("ban".equalsIgnoreCase(status)) {
                            try {
                                Image img = new Image(getClass().getResourceAsStream("/images/block.png"));
                                ImageView iv = new ImageView(img);
                                iv.setFitHeight(16); iv.setFitWidth(16);
                                btnBan.setGraphic(iv);
                            } catch (Exception e) { btnBan.setText("U"); }
                            btnBan.setTooltip(new Tooltip("Reactiver ce compte"));
                        } else {
                            try {
                                Image img = new Image(getClass().getResourceAsStream("/images/block.png"));
                                ImageView iv = new ImageView(img);
                                iv.setFitHeight(16); iv.setFitWidth(16);
                                btnBan.setGraphic(iv);
                            } catch (Exception e) { btnBan.setText("B"); }
                            btnBan.setTooltip(new Tooltip("Bannir ce compte"));
                        }
                    }
                    setGraphic(hBox);
                }
            });
        }
    }

    private void handleToggleBan(User user) {
        if (user == null) return;
        String newStatus = "ban".equalsIgnoreCase(user.getStatus()) ? "actif" : "ban";
        boolean isBanning = newStatus.equals("ban");

        Dialog<ButtonType> confirm = new Dialog<>();
        confirm.setTitle(isBanning ? "Bannir" : "Réactiver");
        confirm.setHeaderText(null);

        VBox content = new VBox();
        content.setSpacing(15);
        content.setStyle("-fx-padding: 25; -fx-background-color: white;");

        String titleBg = isBanning ? "#f59e0b" : "#10b981";
        String bgColor = isBanning ? "#fef3c7" : "#ecfdf5";
        Label userDetailsLabel = new Label(user.getNom() + " " + user.getPrenom() + "\n" + user.getEmail());
        userDetailsLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #1a2340; -fx-padding: 12; -fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        content.getChildren().add(userDetailsLabel);

        confirm.getDialogPane().setContent(content);
        confirm.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        confirm.getDialogPane().setStyle("-fx-padding: 15; -fx-background-color: #f4f6fb;");

        Button actionBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (actionBtn != null) {
            actionBtn.setText(isBanning ? "Bannir" : "Réactiver");
            actionBtn.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 12 32; -fx-background-color: " + titleBg + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        }
        if (cancelBtn != null) {
            cancelBtn.setText("Annuler");
            cancelBtn.setStyle("-fx-font-size: 12; -fx-padding: 12 32; -fx-background-color: #e2e8f4; -fx-text-fill: #1a2340; -fx-background-radius: 8; -fx-cursor: hand;");
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (userService.updateStatus(user.getId(), newStatus)) {
                    showSuccess(isBanning ? "Compte Banni" : "Compte Réactivé",
                        isBanning ? "L'utilisateur " + user.getNom() + " a été banni."
                                  : "L'utilisateur " + user.getNom() + " a été réactivé.");
                    loadUsers();
                    usersTable.refresh();
                } else {
                    showError("Erreur", "Impossible de mettre à jour le statut");
                }
            } catch (Exception e) {
                showError("Erreur", "Erreur lors de la mise à jour: " + e.getMessage());
            }
        }
    }

    public void loadUsers() {
        try {
            List<User> list = userService.findAll();
            allUsers.setAll(list);
            if (usersTable != null) { usersTable.setItems(allUsers); usersTable.sort(); }
            updateCount();
        } catch (Exception e) {
            showError("Erreur de chargement", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    private void applyFiltersAndSearch() {
        try {
            String searchQuery = searchField != null ? searchField.getText() : "";
            String typeFilter  = roleFilter  != null ? roleFilter.getValue()  : "Tous";
            List<User> filtered;

            if ((searchQuery == null || searchQuery.isEmpty()) && ("Tous".equals(typeFilter) || typeFilter == null)) {
                filtered = userService.findAll();
            } else if (searchQuery == null || searchQuery.isEmpty()) {
                filtered = userService.findByType(typeFilter);
            } else if ("Tous".equals(typeFilter) || typeFilter == null) {
                filtered = userService.search(searchQuery);
            } else {
                List<User> byType = userService.findByType(typeFilter);
                String q = searchQuery.toLowerCase();
                filtered = new java.util.ArrayList<>();
                for (User u : byType) {
                    if (u.getNom().toLowerCase().contains(q) ||
                        u.getPrenom().toLowerCase().contains(q) ||
                        u.getEmail().toLowerCase().contains(q)) {
                        filtered.add(u);
                    }
                }
            }

            ObservableList<User> observableFiltered = FXCollections.observableArrayList(filtered);
            if (usersTable != null) { usersTable.setItems(observableFiltered); usersTable.sort(); }
            updateCount();
        } catch (Exception e) {
            showError("Erreur de filtrage", e.getMessage());
        }
    }

    private void updateCount() {
        if (countLabel != null && usersTable != null) {
            int count = usersTable.getItems().size();
            countLabel.setText(count + " utilisateur" + (count > 1 ? "s" : ""));
        }
    }

    @FXML
    public void handleAjouter() { showUserDialog(null); }

    private void handleEdit(User user) {
        if (user == null) return;
        showUserDialog(user);
    }

    private void handleDelete(User user) {
        if (user == null) return;

        Dialog<ButtonType> confirm = new Dialog<>();
        confirm.setTitle("Supprimer"); confirm.setHeaderText(null);

        VBox content = new VBox();
        content.setSpacing(15);
        content.setStyle("-fx-padding: 25; -fx-background-color: white;");

        Label userDetailsLabel = new Label(user.getNom() + " " + user.getPrenom() + "\n" + user.getEmail());
        userDetailsLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #1a2340; -fx-padding: 12; -fx-background-color: #fee2e2; -fx-background-radius: 8;");
        content.getChildren().add(userDetailsLabel);

        confirm.getDialogPane().setContent(content);
        confirm.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        confirm.getDialogPane().setStyle("-fx-padding: 15; -fx-background-color: #f4f6fb;");

        Button deleteBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (deleteBtn != null) {
            deleteBtn.setText("Supprimer");
            deleteBtn.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 12 32; -fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        }
        if (cancelBtn != null) {
            cancelBtn.setText("Annuler");
            cancelBtn.setStyle("-fx-font-size: 12; -fx-padding: 12 32; -fx-background-color: #e2e8f4; -fx-text-fill: #1a2340; -fx-background-radius: 8; -fx-cursor: hand;");
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user.getId());
                showSuccess("Suppression réussie", "L'utilisateur a été supprimé avec succès !");
                loadUsers();
            } catch (Exception e) {
                showError("Erreur de suppression", "Impossible de supprimer l'utilisateur : " + e.getMessage());
            }
        }
    }

    private void showUserDialog(User userToEdit) {
        try {
            // Charger le FXML UserForm
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/interfaces/UserForm.fxml"));
            Parent root = loader.load();
            UserFormController formController = loader.getController();

            // Créer et afficher la fenêtre
            Stage stage = new Stage();
            stage.setTitle(userToEdit == null ? "Ajouter Utilisateur" : "Modifier Utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            // Initialiser le contrôleur
            if (userToEdit == null) {
                formController.initializeForCreate(this);
            } else {
                formController.initializeForEdit(userToEdit, this);
            }

            stage.showAndWait();
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger le formulaire : " + e.getMessage());
        }
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-padding: 20;");
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title); alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-padding: 20;");
        alert.showAndWait();
    }

    @FXML
    public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
