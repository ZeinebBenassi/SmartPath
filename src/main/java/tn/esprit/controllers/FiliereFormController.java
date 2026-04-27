package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.entity.Filiere;
import tn.esprit.services.FiliereService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FiliereFormController implements Initializable {

    @FXML private Label            lblTitle;
    @FXML private TextField        txtNom;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private TextArea         txtDescription;
    @FXML private TextArea         txtDebouches;
    @FXML private TextArea         txtCompetences;
    @FXML private ComboBox<String> cbIcon;
    @FXML private TextField        txtImage;
    @FXML private StackPane        imgPreviewPane;
    @FXML private Label            lblNoImage;
    @FXML private Button           btnSave;
    @FXML private Button           btnCancel;

    private final FiliereService filiereService = new FiliereService();
    private FiliereController    parentController;
    private Filiere              currentFiliere;
    private String               selectedImagePath = null;

    private static final String[] CATEGORIES = {"informatique","mathematiques","sciences","ingenierie","gestion"};
    private static final String[] NIVEAUX    = {"Licence","Master","Doctorat","BTS","DUT","Ingénieur"};
    private static final String[] ICONS      = {"💻","📊","🔒","🌐","🤖","🎨","⚙","🧮","🗄","📱"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategorie.getItems().addAll(CATEGORIES);
        cbNiveau.getItems().addAll(NIVEAUX);
        cbIcon.getItems().addAll(ICONS);
    }

    public void initData(Filiere filiere, FiliereController parent) {
        this.parentController = parent;
        this.currentFiliere   = filiere;

        if (filiere == null) {
            lblTitle.setText("➕  Nouvelle Filière");
        } else {
            lblTitle.setText("✏  Modifier la Filière");
            txtNom.setText(filiere.getNom());
            cbCategorie.setValue(filiere.getCategorie());
            cbNiveau.setValue(filiere.getNiveau());
            txtDescription.setText(filiere.getDescription());
            txtDebouches.setText(filiere.getDebouches());
            txtCompetences.setText(filiere.getCompetences());
            cbIcon.setValue(filiere.getIcon());
            if (filiere.getImage() != null && !filiere.getImage().isEmpty()) {
                selectedImagePath = filiere.getImage();
                txtImage.setText(filiere.getImage());
                afficherPreview(filiere.getImage());
            }
        }
    }

    @FXML private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp"));
        File file = fc.showOpenDialog(btnSave.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            txtImage.setText(file.getName());
            afficherPreview(selectedImagePath);
        }
    }

    @FXML private void handleRemoveImage() {
        selectedImagePath = null; txtImage.clear();
        imgPreviewPane.getChildren().clear();
        Label lbl = new Label("Aucune image sélectionnée");
        lbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px;");
        imgPreviewPane.getChildren().add(lbl);
    }

    private void afficherPreview(String path) {
        try {
            Image img = new Image(new File(path).toURI().toString(), 460, 120, false, true);
            if (!img.isError()) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(460); iv.setFitHeight(120); iv.setPreserveRatio(true);
                imgPreviewPane.getChildren().clear();
                imgPreviewPane.getChildren().add(iv);
            }
        } catch (Exception e) { System.err.println("Aperçu impossible : " + e.getMessage()); }
    }

    @FXML private void handleSave() {
        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            showWarning("Le nom de la filière est obligatoire."); txtNom.requestFocus(); return;
        }
        if (cbCategorie.getValue() == null) { showWarning("Veuillez sélectionner une catégorie."); return; }
        if (cbNiveau.getValue() == null) { showWarning("Veuillez sélectionner un niveau."); return; }

        Filiere f = (currentFiliere == null) ? new Filiere() : currentFiliere;
        f.setNom(txtNom.getText().trim());
        f.setCategorie(cbCategorie.getValue());
        f.setNiveau(cbNiveau.getValue());
        f.setDescription(txtDescription.getText());
        f.setDebouches(txtDebouches.getText());
        f.setCompetences(txtCompetences.getText());
        f.setIcon(cbIcon.getValue() != null ? cbIcon.getValue() : "🎓");
        f.setImage(selectedImagePath);

        try {
            if (currentFiliere == null) { filiereService.ajouter(f); showInfo("Filière ajoutée !"); }
            else                        { filiereService.modifier(f); showInfo("Filière modifiée !"); }
            closeWindow();
        } catch (SQLException e) { showError("Erreur BDD : " + e.getMessage()); }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() { btnCancel.getScene().getWindow().hide(); }
    private void showWarning(String msg) { new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait(); }
    private void showError(String msg)   { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void showInfo(String msg)    { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
