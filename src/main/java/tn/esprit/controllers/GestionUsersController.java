package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import models.User;
import services.UserService;

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

    /**
     * Configure les colonnes du tableau.
     */
    private void setupColumns() {
        if (colNom != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colEmail != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colRole != null) colRole.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Colonne Statut et Date formatées
        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<User, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().isEmpty()) {
                        setText(null);
                    } else {
                        User user = getTableView().getItems().get(getIndex());
                        String type = user.getType();
                        setText(type != null ? type.substring(0, 1).toUpperCase() + type.substring(1) : "N/A");
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
                        if (user.getCreatedAt() != null) {
                            setText(dateFormat.format(user.getCreatedAt()));
                        } else {
                            setText("N/A");
                        }
                    }
                }
            });
        }
    }

    /**
     * Configure les filtres et recherche.
     */
    private void setupFilters() {
        if (roleFilter != null) {
            roleFilter.setItems(FXCollections.observableArrayList("Tous", "admin", "prof", "etudiant"));
            roleFilter.getSelectionModel().selectFirst();
            roleFilter.setOnAction(e -> applyFiltersAndSearch());
        }

        if (searchField != null) {
            searchField.setOnKeyReleased(e -> applyFiltersAndSearch());
        }

        // Tri des colonnes
        setupColumnSorting();
    }

    /**
     * Ajoute le tri aux colonnes cliquables.
     */
    private void setupColumnSorting() {
        if (colId != null) colId.setSortable(true);
        if (colNom != null) colNom.setSortable(true);
        if (colPrenom != null) colPrenom.setSortable(true);
        if (colEmail != null) colEmail.setSortable(true);
        if (colRole != null) colRole.setSortable(true);

        if (usersTable != null) {
            usersTable.getSortOrder().add(colId != null ? colId : colNom);
        }
    }

    /**
     * Configure la colonne Actions avec les boutons.
     */
    private void setupActionButtons() {
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<User, Void>() {
                private final Button btnEdit = new Button("✎ Modifier");
                private final Button btnDel = new Button("✕ Supprimer");
                private final HBox hBox = new HBox(6, btnEdit, btnDel);

                {
                    btnEdit.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                    btnDel.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");

                    btnEdit.setOnAction(e -> handleEdit(getTableRow().getItem()));
                    btnDel.setOnAction(e -> handleDelete(getTableRow().getItem()));

                    hBox.setStyle("-fx-alignment: CENTER;");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : hBox);
                }
            });
        }
    }

    /**
     * Charge tous les utilisateurs depuis la base de données.
     */
    private void loadUsers() {
        try {
            List<User> list = userService.findAll();
            allUsers.setAll(list);
            if (usersTable != null) {
                usersTable.setItems(allUsers);
                usersTable.sort();
            }
            updateCount();
        } catch (Exception e) {
            showError("Erreur de chargement", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    /**
     * Applique le filtrage et la recherche combinés.
     */
    @FXML
    private void applyFiltersAndSearch() {
        try {
            String searchQuery = searchField != null ? searchField.getText() : "";
            String typeFilter = roleFilter != null ? roleFilter.getValue() : "Tous";

            List<User> filtered;

            if ((searchQuery == null || searchQuery.isEmpty()) && ("Tous".equals(typeFilter) || typeFilter == null)) {
                filtered = userService.findAll();
            } else if (searchQuery == null || searchQuery.isEmpty()) {
                filtered = userService.findByType(typeFilter);
            } else if ("Tous".equals(typeFilter) || typeFilter == null) {
                filtered = userService.search(searchQuery);
            } else {
                // Combiner recherche et filtrage
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
            if (usersTable != null) {
                usersTable.setItems(observableFiltered);
                usersTable.sort();
            }
            updateCount();
        } catch (Exception e) {
            showError("Erreur de filtrage", e.getMessage());
        }
    }

    /**
     * Affiche le nombre d'utilisateurs actuels.
     */
    private void updateCount() {
        if (countLabel != null && usersTable != null) {
            int count = usersTable.getItems().size();
            countLabel.setText(count + " utilisateur" + (count > 1 ? "s" : ""));
        }
    }

    /**
     * Gère l'ajout d'un nouvel utilisateur.
     */
    @FXML
    public void handleAjouter() {
        showUserDialog(null);
    }

    /**
     * Gère la modification d'un utilisateur.
     */
    private void handleEdit(User user) {
        if (user == null) return;
        showUserDialog(user);
    }

    /**
     * Gère la suppression d'un utilisateur.
     */
    private void handleDelete(User user) {
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + user.getNom() + " " + user.getPrenom() + " (" + user.getEmail() + ") ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user.getId());
                showInfo("Succès", "Utilisateur supprimé avec succès");
                loadUsers();
            } catch (Exception e) {
                showError("Erreur", "Impossible de supprimer l'utilisateur : " + e.getMessage());
            }
        }
    }

    /**
     * Affiche la boîte de dialogue pour créer/modifier un utilisateur.
     */
    private void showUserDialog(User userToEdit) {
        // Créer la boîte de dialogue
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(userToEdit == null ? "Ajouter un utilisateur" : "Modifier l'utilisateur");
        dialog.setHeaderText(userToEdit == null ? "Créer un nouvel utilisateur" : "Modifier les informations");

        // Créer les contrôles
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");

        TextField tfNom = new TextField();
        tfNom.setPromptText("Nom");
        tfNom.setPrefWidth(300);

        TextField tfPrenom = new TextField();
        tfPrenom.setPromptText("Prénom");

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Email");

        PasswordField pfPassword = new PasswordField();
        pfPassword.setPromptText("Mot de passe (min 6 caractères)");

        TextField tfCIN = new TextField();
        tfCIN.setPromptText("CIN");

        TextField tfTelephone = new TextField();
        tfTelephone.setPromptText("Téléphone");

        TextField tfAdresse = new TextField();
        tfAdresse.setPromptText("Adresse");

        ComboBox<String> cbType = new ComboBox<>();
        cbType.setItems(FXCollections.observableArrayList("admin", "prof", "etudiant"));
        cbType.setPromptText("Type");

        // Si modification, pré-remplir les champs
        if (userToEdit != null) {
            tfNom.setText(userToEdit.getNom());
            tfPrenom.setText(userToEdit.getPrenom());
            tfEmail.setText(userToEdit.getEmail());
            tfCIN.setText(userToEdit.getCin() != null ? userToEdit.getCin() : "");
            tfTelephone.setText(userToEdit.getTelephone() != null ? userToEdit.getTelephone() : "");
            tfAdresse.setText(userToEdit.getAdresse() != null ? userToEdit.getAdresse() : "");
            cbType.setValue(userToEdit.getType() != null ? userToEdit.getType() : "admin");
            pfPassword.setPromptText("Laisser vide pour ne pas changer");
        }

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(tfNom, 1, 0);
        grid.add(new Label("Prénom:"), 0, 1);
        grid.add(tfPrenom, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(tfEmail, 1, 2);
        grid.add(new Label("Mot de passe:"), 0, 3);
        grid.add(pfPassword, 1, 3);
        grid.add(new Label("CIN:"), 0, 4);
        grid.add(tfCIN, 1, 4);
        grid.add(new Label("Téléphone:"), 0, 5);
        grid.add(tfTelephone, 1, 5);
        grid.add(new Label("Adresse:"), 0, 6);
        grid.add(tfAdresse, 1, 6);
        grid.add(new Label("Type:"), 0, 7);
        grid.add(cbType, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                User user = userToEdit != null ? userToEdit : new User();
                user.setNom(tfNom.getText());
                user.setPrenom(tfPrenom.getText());
                user.setEmail(tfEmail.getText());
                user.setCin(tfCIN.getText());
                user.setTelephone(tfTelephone.getText());
                user.setAdresse(tfAdresse.getText());
                user.setType(cbType.getValue() != null ? cbType.getValue() : "admin");

                // Mot de passe
                String password = pfPassword.getText();
                if (!password.isEmpty()) {
                    user.setPassword(password);
                }
                return user;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        if (result.isPresent()) {
            User user = result.get();
            try {
                // Validations simples
                if (user.getNom() == null || user.getNom().isEmpty()) {
                    showError("Validation", "Le nom est obligatoire");
                    return;
                }
                if (user.getPrenom() == null || user.getPrenom().isEmpty()) {
                    showError("Validation", "Le prénom est obligatoire");
                    return;
                }
                if (user.getEmail() == null || user.getEmail().isEmpty()) {
                    showError("Validation", "L'email est obligatoire");
                    return;
                }

                if (userToEdit == null) {
                    // Création
                    if (user.getPassword() == null || user.getPassword().isEmpty()) {
                        showError("Validation", "Le mot de passe est obligatoire pour un nouvel utilisateur");
                        return;
                    }
                    int newId = userService.create(user);
                    if (newId > 0) {
                        showInfo("Succès", "Utilisateur créé avec ID: " + newId);
                    } else {
                        showError("Erreur", "Impossible de créer l'utilisateur");
                    }
                } else {
                    // Modification
                    if (userService.update(user)) {
                        showInfo("Succès", "Utilisateur modifié avec succès");
                    } else {
                        showError("Erreur", "Impossible de modifier l'utilisateur");
                    }
                }
                loadUsers();
            } catch (Exception e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    /**
     * Affiche un message d'information.
     */
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Affiche un message d'erreur.
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardAdmin.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

