package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    private void loadUsers() {
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
        Dialog<User> dialog = new Dialog<>();
        boolean isNew = (userToEdit == null);
        dialog.setTitle(isNew ? "Ajouter" : "Modifier");
        dialog.setHeaderText(null);

        VBox mainContainer = new VBox();
        mainContainer.setSpacing(20);
        mainContainer.setStyle("-fx-padding: 25; -fx-background-color: white;");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(14); grid.setStyle("-fx-padding: 0;");

        TextField tfNom       = createStyledTextField("Nom complet");
        TextField tfPrenom    = createStyledTextField("Prénom");
        TextField tfEmail     = createStyledTextField("Adresse email");
        PasswordField pfPassword = new PasswordField();
        pfPassword.setStyle("-fx-font-size: 12; -fx-padding: 12; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-insets: 0;");
        pfPassword.setPromptText("Mot de passe");
        TextField tfCIN       = createStyledTextField("Numéro CIN");
        TextField tfTelephone = createStyledTextField("Téléphone");
        TextField tfAdresse   = createStyledTextField("Adresse");
        ComboBox<String> cbType = new ComboBox<>();
        cbType.setItems(FXCollections.observableArrayList("admin", "prof", "etudiant"));
        cbType.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-border-width: 1;");
        cbType.setPrefWidth(300);

        if (userToEdit != null) {
            tfNom.setText(userToEdit.getNom());
            tfPrenom.setText(userToEdit.getPrenom());
            tfEmail.setText(userToEdit.getEmail());
            tfCIN.setText(userToEdit.getCin() != null ? userToEdit.getCin() : "");
            tfTelephone.setText(userToEdit.getTelephone() != null ? userToEdit.getTelephone() : "");
            tfAdresse.setText(userToEdit.getAdresse() != null ? userToEdit.getAdresse() : "");
            cbType.setValue(userToEdit.getType() != null ? userToEdit.getType() : "admin");
            pfPassword.setPromptText("Laisser vide pour conserver le mot de passe");
        } else {
            cbType.getSelectionModel().selectFirst();
        }

        int row = 0;
        grid.add(createStyledLabel("Nom:"),          0, row); grid.add(tfNom,       1, row++);
        grid.add(createStyledLabel("Prénom:"),       0, row); grid.add(tfPrenom,    1, row++);
        grid.add(createStyledLabel("Email:"),        0, row); grid.add(tfEmail,     1, row++);
        grid.add(createStyledLabel("Mot de passe:"),0, row); grid.add(pfPassword,  1, row++);
        grid.add(createStyledLabel("CIN:"),          0, row); grid.add(tfCIN,       1, row++);
        grid.add(createStyledLabel("Téléphone:"),   0, row); grid.add(tfTelephone, 1, row++);
        grid.add(createStyledLabel("Adresse:"),      0, row); grid.add(tfAdresse,   1, row++);
        grid.add(createStyledLabel("Type:"),         0, row); grid.add(cbType,      1, row);

        mainContainer.getChildren().add(grid);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-padding: 15; -fx-background-color: #f4f6fb;");

        Button okBtn     = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (okBtn != null) {
            okBtn.setText(isNew ? "Créer" : "Modifier");
            okBtn.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 12 32; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        }
        if (cancelBtn != null) {
            cancelBtn.setText("Annuler");
            cancelBtn.setStyle("-fx-font-size: 12; -fx-padding: 12 32; -fx-background-color: #e2e8f4; -fx-text-fill: #1a2340; -fx-background-radius: 8; -fx-cursor: hand;");
        }

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
                String password = pfPassword.getText();
                if (!password.isEmpty()) user.setPassword(password);
                return user;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        if (result.isPresent()) {
            User user = result.get();
            try {
                if (user.getNom() == null || user.getNom().isEmpty()) { showError("Validation", "Le nom est obligatoire"); return; }
                if (user.getPrenom() == null || user.getPrenom().isEmpty()) { showError("Validation", "Le prénom est obligatoire"); return; }
                if (user.getEmail() == null || user.getEmail().isEmpty()) { showError("Validation", "L'email est obligatoire"); return; }

                if (userToEdit == null) {
                    if (user.getPassword() == null || user.getPassword().isEmpty()) {
                        showError("Validation", "Le mot de passe est obligatoire pour un nouvel utilisateur"); return;
                    }
                    int newId = userService.create(user);
                    if (newId > 0) showSuccess("Succès", "Utilisateur créé avec succès !\n\n" + user.getNom() + " " + user.getPrenom() + "\n" + user.getEmail());
                    else showError("Erreur", "Impossible de créer l'utilisateur");
                } else {
                    if (userService.update(user)) showSuccess("Succès", "Utilisateur modifié avec succès !\n\n" + user.getNom() + " " + user.getPrenom());
                    else showError("Erreur", "Impossible de modifier l'utilisateur");
                }
                loadUsers();
            } catch (Exception e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    private TextField createStyledTextField(String promptText) {
        TextField tf = new TextField();
        tf.setPromptText(promptText);
        tf.setStyle("-fx-font-size: 12; -fx-padding: 12; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-insets: 0;");
        tf.setPrefWidth(300);
        return tf;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #1a2340;");
        return label;
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
