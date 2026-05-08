package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.json.JSONObject;
import tn.esprit.entity.Filiere;
import tn.esprit.interfaces.NumericSpinner;
import tn.esprit.services.FiliereService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    // ── Champs Traits de Personnalité (identiques au formulaire Symfony) ──────
    @FXML private NumericSpinner traitAnalytique;
    @FXML private NumericSpinner traitPratique;
    @FXML private NumericSpinner traitCreatif;
    @FXML private NumericSpinner traitTechnique;
    @FXML private NumericSpinner traitMathematique;
    @FXML private NumericSpinner traitAlgorithmique;
    @FXML private NumericSpinner traitSystemes;
    @FXML private NumericSpinner traitReseaux;
    @FXML private NumericSpinner traitSecurite;
    @FXML private NumericSpinner traitDonnees;

    private final FiliereService filiereService = new FiliereService();
    private FiliereController    parentController;
    private Filiere              currentFiliere;
    private String               selectedImagePath = null;

    private static final String[] CATEGORIES = {"informatique","mathematiques","sciences","ingenierie","gestion"};
    private static final String[] NIVEAUX    = {"Licence","Master","Doctorat","BTS","DUT","Ingénieur"};
    private static final String[] ICONS      = {"💻","📊","🔒","🌐","🤖","🎨","⚙","🧮","🗄","📱"};

    // ── Clés JSON exactement identiques à celles de Symfony ──────────────────
    private static final String[] TRAIT_KEYS = {
        "analytique","pratique","creatif","technique",
        "mathematique","algorithmique","systemes","reseaux",
        "securite","donnees"
    };

    // ── Dossier partagé avec Symfony : public/uploads/filieres/ ──────────────
    private static final String SYMFONY_UPLOAD_DIR =
        "C:/Users/GIGABYTE/Desktop/dev/integration-web-java/" +
        "Esprit-PIDEV-3A40-2025-2026-SmartPath/public/uploads/filieres/";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategorie.getItems().addAll(CATEGORIES);
        cbNiveau.getItems().addAll(NIVEAUX);
        cbIcon.getItems().addAll(ICONS);
        
        // La validation des traits est maintenant gérée par NumericSpinner
    }

    public void initData(Filiere filiere, FiliereController parent) {
        this.parentController = parent;
        this.currentFiliere   = filiere;

        if (filiere == null) {
            lblTitle.setText("➕  Nouvelle Filière");
            // Valeurs par défaut = 0 pour tous les traits
            setAllTraitsToZero();
        } else {
            lblTitle.setText("✏  Modifier la Filière");
            txtNom.setText(filiere.getNom());
            cbCategorie.setValue(filiere.getCategorie());
            cbNiveau.setValue(filiere.getNiveau());
            txtDescription.setText(filiere.getDescription());
            txtDebouches.setText(filiere.getDebouches());
            txtCompetences.setText(filiere.getCompetences());
            cbIcon.setValue(filiere.getIcon());

            // ── Charger les traits depuis le JSON stocké en BDD ──────────────
            loadTraitsFromJson(filiere.getTraits());

            // ── Charger l'image ───────────────────────────────────────────────
            if (filiere.getImage() != null && !filiere.getImage().isEmpty()) {
                selectedImagePath = filiere.getImage();
                String displayName = filiere.getImage().contains("/")
                    ? filiere.getImage().substring(filiere.getImage().lastIndexOf('/') + 1)
                    : filiere.getImage().substring(filiere.getImage().lastIndexOf('\\') + 1);
                txtImage.setText(displayName);
                if (filiere.getImage().startsWith("/uploads/")) {
                    String physicalPath = SYMFONY_UPLOAD_DIR +
                        filiere.getImage().substring("/uploads/filieres/".length());
                    afficherPreview(physicalPath);
                } else {
                    afficherPreview(filiere.getImage());
                }
            }
        }
    }

    // ── Helpers traits ────────────────────────────────────────────────────────

    /** Retourne les 10 NumericSpinner dans le même ordre que TRAIT_KEYS. */
    private NumericSpinner[] getTraitFields() {
        return new NumericSpinner[]{
            traitAnalytique, traitPratique, traitCreatif, traitTechnique,
            traitMathematique, traitAlgorithmique, traitSystemes, traitReseaux,
            traitSecurite, traitDonnees
        };
    }

    private void setAllTraitsToZero() {
        for (NumericSpinner spinner : getTraitFields()) {
            if (spinner != null) spinner.setValue(0);
        }
    }

    /**
     * Lit le JSON stocké en BDD (ou null) et remplit les champs.
     * Format attendu : {"analytique":5,"pratique":3,...}
     */
    private void loadTraitsFromJson(String traitsJson) {
        if (traitsJson == null || traitsJson.isBlank() || traitsJson.equals("null")) {
            setAllTraitsToZero();
            return;
        }
        try {
            JSONObject obj = new JSONObject(traitsJson);
            NumericSpinner[] fields = getTraitFields();
            for (int i = 0; i < TRAIT_KEYS.length; i++) {
                String key = TRAIT_KEYS[i];
                int    val = obj.optInt(key, 0);
                // Clamp 0-5
                val = Math.max(0, Math.min(5, val));
                if (fields[i] != null) fields[i].setValue(val);
            }
        } catch (Exception e) {
            System.err.println("[FiliereForm] Impossible de lire les traits JSON : " + e.getMessage());
            setAllTraitsToZero();
        }
    }

    /**
     * Construit le JSON des traits à partir des champs du formulaire.
     * Produit : {"analytique":5,"pratique":3,...}
     */
    private String buildTraitsJson() {
        JSONObject obj    = new JSONObject();
        NumericSpinner[] fields = getTraitFields();
        for (int i = 0; i < TRAIT_KEYS.length; i++) {
            int val = 0;
            if (fields[i] != null) {
                val = fields[i].getValue();
                val = Math.max(0, Math.min(5, val));
            }
            obj.put(TRAIT_KEYS[i], val);
        }
        return obj.toString();
    }

    // ── Gestion image ─────────────────────────────────────────────────────────

    @FXML private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp")
        );
        File file = fc.showOpenDialog(btnSave.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            txtImage.setText(file.getName());
            afficherPreview(selectedImagePath);
        }
    }

    @FXML private void handleRemoveImage() {
        selectedImagePath = null;
        txtImage.clear();
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
        } catch (Exception e) {
            System.err.println("Aperçu impossible : " + e.getMessage());
        }
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    @FXML private void handleSave() {
        // Validation champs obligatoires
        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            showWarning("Le nom de la filière est obligatoire."); txtNom.requestFocus(); return;
        }
        if (cbCategorie.getValue() == null) { showWarning("Veuillez sélectionner une catégorie."); return; }
        if (cbNiveau.getValue()    == null) { showWarning("Veuillez sélectionner un niveau."); return; }

        Filiere f = (currentFiliere == null) ? new Filiere() : currentFiliere;
        f.setNom(txtNom.getText().trim());
        f.setCategorie(cbCategorie.getValue());
        f.setNiveau(cbNiveau.getValue());
        f.setDescription(txtDescription.getText());
        f.setDebouches(txtDebouches.getText());
        f.setCompetences(txtCompetences.getText());
        f.setIcon(cbIcon.getValue() != null ? cbIcon.getValue() : "🎓");

        // ── Traits : construire le JSON et l'affecter ─────────────────────────
        String traitsJson = buildTraitsJson();
        f.setTraits(traitsJson);
        System.out.println("[FiliereForm] Traits JSON sauvegardé : " + traitsJson);

        // ── Gestion image ─────────────────────────────────────────────────────
        if (selectedImagePath != null && !selectedImagePath.startsWith("/uploads/")) {
            String webPath = copyImageToSymfony(selectedImagePath);
            if (webPath != null) {
                f.setImage(webPath);
            } else {
                f.setImage(selectedImagePath);
                showWarning("L'image n'a pas pu être copiée vers Symfony.\n" +
                    "Elle sera stockée avec son chemin local.");
            }
        } else {
            f.setImage(selectedImagePath);
        }

        try {
            if (currentFiliere == null) { filiereService.ajouter(f);  showInfo("Filière ajoutée avec ses traits !"); }
            else                        { filiereService.modifier(f); showInfo("Filière modifiée avec ses traits !"); }
            if (parentController != null) parentController.refreshData();
            closeWindow();
        } catch (SQLException e) {
            showError("Erreur BDD : " + e.getMessage());
        }
    }

    /** Copie l'image dans le dossier Symfony et retourne le chemin web /uploads/filieres/... */
    private String copyImageToSymfony(String sourcePath) {
        try {
            File source = new File(sourcePath);
            if (!source.exists() || !source.isFile()) return null;

            File destDir = new File(SYMFONY_UPLOAD_DIR);
            if (!destDir.exists()) destDir.mkdirs();

            String name     = source.getName();
            String ext      = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "jpg";
            String stem     = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String slug     = stem.replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = slug + "_" + System.currentTimeMillis() + "." + ext;
            File   dest     = new File(destDir, filename);

            try (InputStream  in  = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int    n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            System.out.println("[FiliereForm] Image copiée : " + dest.getAbsolutePath());
            return "/uploads/filieres/" + filename;
        } catch (Exception e) {
            System.err.println("[FiliereForm] Copie image échouée : " + e.getMessage());
            return null;
        }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() { btnCancel.getScene().getWindow().hide(); }
    private void showWarning(String msg) { new Alert(Alert.AlertType.WARNING,     msg, ButtonType.OK).showAndWait(); }
    private void showError(String msg)   { new Alert(Alert.AlertType.ERROR,       msg, ButtonType.OK).showAndWait(); }
    private void showInfo(String msg)    { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
