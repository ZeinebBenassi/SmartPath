package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.entity.Matiere;
import org.example.service.MatiereService;

import java.io.IOException;
import java.util.List;

public class CoursController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filiereFilter;
    @FXML private FlowPane coursesContainer;

    private MatiereService matiereService = new MatiereService();

    @FXML
    public void initialize() {
        // Load initial data
        loadCourses(null, null);
        
        // Setup Search on Enter key
        searchField.setOnAction(e -> handleFilter());
    }

    private void loadCourses(String search, Integer filiereId) {
        coursesContainer.getChildren().clear();
        List<Matiere> matieres = matiereService.findVisible(search, filiereId);
        
        if (matieres.isEmpty()) {
            Label emptyLabel = new Label("Aucun cours trouvé.");
            emptyLabel.getStyleClass().add("header-subtitle");
            coursesContainer.getChildren().add(emptyLabel);
        } else {
            for (Matiere m : matieres) {
                coursesContainer.getChildren().add(createCourseCard(m));
            }
        }
    }

    private VBox createCourseCard(Matiere m) {
        VBox card = new VBox(10);
        card.getStyleClass().add("course-card");
        card.setPrefWidth(280);

        Label filiereLabel = new Label("Informatique"); // Default for now
        filiereLabel.getStyleClass().add("course-filiere");

        Label titleLabel = new Label(m.getTitre());
        titleLabel.getStyleClass().add("course-title");
        titleLabel.setWrapText(true);

        Text descText = new Text(m.getDescription());
        descText.getStyleClass().add("course-desc");
        descText.setWrappingWidth(250);

        Button actionBtn = new Button("Accéder au cours →");
        actionBtn.getStyleClass().add("btn-primary");
        actionBtn.setPrefWidth(Double.MAX_VALUE);
        actionBtn.setCursor(javafx.scene.Cursor.HAND);
        
        // Navigation to Matiere Details (Matches Symfony 'cours_show')
        actionBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/matiere_details.fxml"));
                Parent root = loader.load();
                
                MatiereDetailController detailController = loader.getController();
                detailController.initData(m.getId());
                
                Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de charger les détails du cours.");
            }
        });

        card.getChildren().addAll(filiereLabel, titleLabel, descText, actionBtn);
        return card;
    }

    @FXML
    private void handleFilter() {
        loadCourses(searchField.getText(), null);
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        loadCourses(null, null);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
