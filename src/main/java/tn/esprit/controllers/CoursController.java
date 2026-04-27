package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entity.User;

public class CoursController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> matiereFilter;
    @FXML private Button btnAdd;
    @FXML private ListView<String> leconsList;
    @FXML private Label leconTitre;
    @FXML private Label leconMatiere;
    @FXML private Label leconDuree;
    @FXML private Label leconProf;
    @FXML private Label leconDescription;
    @FXML private Label leconContenu;
    @FXML private Label leconFichier;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnTelecharger;

    private static User currentUser;
    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        boolean isProf = currentUser != null && "prof".equals(currentUser.getType());
        if (btnAdd    != null) btnAdd.setVisible(isProf);
        if (btnEdit   != null) btnEdit.setVisible(false);
        if (btnDelete != null) btnDelete.setVisible(false);
        if (matiereFilter != null) { matiereFilter.getItems().clear(); matiereFilter.setPromptText("Filtrer"); }
        if (leconsList    != null) { leconsList.getItems().clear(); leconsList.setPlaceholder(new Label("Aucune leçon disponible pour le moment.")); }
        clearLeconDetails();
    }

    private void clearLeconDetails() {
        if (leconTitre       != null) leconTitre.setText("Aucune leçon sélectionnée");
        if (leconMatiere     != null) leconMatiere.setText("");
        if (leconDuree       != null) leconDuree.setText("");
        if (leconProf        != null) leconProf.setText("");
        if (leconDescription != null) leconDescription.setText("Le contenu sera affiché ici quand des leçons seront ajoutées.");
        if (leconContenu     != null) leconContenu.setText("Aucun contenu disponible.");
        if (leconFichier     != null) leconFichier.setText("");
        if (btnEdit          != null) btnEdit.setVisible(false);
        if (btnDelete        != null) btnDelete.setVisible(false);
        if (btnTelecharger   != null) btnTelecharger.setVisible(false);
    }

    @FXML public void addLecon()          { navigate("/tn/esprit/interfaces/LeconForm.fxml"); }
    @FXML public void editLecon()         { navigate("/tn/esprit/interfaces/LeconForm.fxml"); }
    @FXML public void deleteLecon()       { new Alert(Alert.AlertType.INFORMATION, "Aucune leçon à supprimer pour le moment.", ButtonType.OK).showAndWait(); }
    @FXML public void telechargerFichier(){ new Alert(Alert.AlertType.INFORMATION, "Aucun fichier à télécharger pour le moment.", ButtonType.OK).showAndWait(); }

    @FXML public void goBack() {
        navigate((currentUser != null && "prof".equals(currentUser.getType()))
            ? "/tn/esprit/interfaces/DashboardProf.fxml"
            : "/tn/esprit/interfaces/DashboardEtudiant.fxml");
    }

    private void navigate(String fxml) {
        try { Parent root = FXMLLoader.load(getClass().getResource(fxml)); leconsList.getScene().setRoot(root); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
