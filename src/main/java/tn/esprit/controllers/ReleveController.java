package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.User;

public class ReleveController {

    @FXML private Label moyenneGenerale;
    @FXML private Label mentionLabel;
    @FXML private Label niveauLabel;
    @FXML private Label filiereLabel;
    @FXML private TableView<NoteRow> notesTable;
    @FXML private TableColumn<NoteRow, String> colMatiere;
    @FXML private TableColumn<NoteRow, String> colNote;
    @FXML private TableColumn<NoteRow, String> colCoeff;
    @FXML private TableColumn<NoteRow, String> colMention;

    private static User currentUser;
    public static void setCurrentUser(User u) { currentUser = u; }

    @FXML
    public void initialize() {
        if (currentUser != null) {
            if (niveauLabel  != null) niveauLabel.setText("L2");
            if (filiereLabel != null) filiereLabel.setText("Informatique");
        }

        // DonnÃ©es exemple
        if (colMatiere != null) colMatiere.setCellValueFactory(new PropertyValueFactory<>("matiere"));
        if (colNote    != null) colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        if (colCoeff   != null) colCoeff.setCellValueFactory(new PropertyValueFactory<>("coeff"));
        if (colMention != null) colMention.setCellValueFactory(new PropertyValueFactory<>("mention"));

        if (notesTable != null) {
            notesTable.setItems(FXCollections.observableArrayList(
                new NoteRow("MathÃ©matiques",     "15.5", "4", "Bien"),
                new NoteRow("Informatique",       "17.0", "5", "TrÃ¨s bien"),
                new NoteRow("Physique",           "12.0", "3", "Assez bien"),
                new NoteRow("Anglais",            "14.5", "2", "Bien"),
                new NoteRow("Ã‰conomie",           "11.0", "2", "Passable"),
                new NoteRow("Base de donnÃ©es",    "16.0", "4", "TrÃ¨s bien")
            ));
        }

        if (moyenneGenerale != null) moyenneGenerale.setText("14.8 / 20");
        if (mentionLabel    != null) mentionLabel.setText("Bien");
    }

    @FXML public void exportPDF() {
        new Alert(Alert.AlertType.INFORMATION,
            "Export PDF en cours de dÃ©veloppement.", ButtonType.OK).showAndWait();
    }

    @FXML public void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/interfaces/DashboardEtudiant.fxml"));
            if (notesTable != null) notesTable.getScene().setRoot(root);
            else if (moyenneGenerale != null) moyenneGenerale.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // â”€â”€ Inner class pour les lignes du tableau â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public static class NoteRow {
        private final String matiere;
        private final String note;
        private final String coeff;
        private final String mention;

        public NoteRow(String matiere, String note, String coeff, String mention) {
            this.matiere = matiere; this.note = note;
            this.coeff = coeff; this.mention = mention;
        }
        public String getMatiere()  { return matiere; }
        public String getNote()     { return note; }
        public String getCoeff()    { return coeff; }
        public String getMention()  { return mention; }
    }
}

