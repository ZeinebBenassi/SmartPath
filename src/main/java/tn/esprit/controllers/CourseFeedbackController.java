package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entity.User;
import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import tn.esprit.services.CourseRatingService;
import tn.esprit.services.feature_cours_et_quiz.MatiereCrudService;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class CourseFeedbackController {

    @FXML private ComboBox<Matiere> courseComboBox;
    @FXML private TableView<FeedbackEntry> feedbackTable;
    @FXML private TableColumn<FeedbackEntry, String> colStudent;
    @FXML private TableColumn<FeedbackEntry, Integer> colStars;
    @FXML private TableColumn<FeedbackEntry, String> colComment;
    @FXML private TableColumn<FeedbackEntry, Date> colDate;
    @FXML private Label statusLabel;

    private final MatiereCrudService matiereService = new MatiereCrudService();
    private final CourseRatingService ratingService = new CourseRatingService();
    private final ObservableList<FeedbackEntry> feedbackList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStars.setCellValueFactory(new PropertyValueFactory<>("stars"));
        colComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        feedbackTable.setItems(feedbackList);

        try {
            List<Matiere> matieres = matiereService.getAll();
            courseComboBox.setItems(FXCollections.observableArrayList(matieres));
            courseComboBox.setConverter(new javafx.util.StringConverter<Matiere>() {
                @Override
                public String toString(Matiere matiere) {
                    return matiere != null ? matiere.getTitre() : "";
                }

                @Override
                public Matiere fromString(String string) {
                    return null; // Not used for ComboBox display
                }
            });
        } catch (SQLException e) {
            statusLabel.setText("Error loading courses: " + e.getMessage());
        }
    }

    @FXML
    public void loadFeedback() {
        Matiere selectedMatiere = courseComboBox.getSelectionModel().getSelectedItem();
        if (selectedMatiere == null) {
            statusLabel.setText("Please select a course.");
            return;
        }

        feedbackList.clear();
        String sql = "SELECT cr.stars, cr.comment, cr.created_at, u.nom, u.prenom " +
                     "FROM course_ratings cr JOIN user u ON cr.student_id = u.id " +
                     "WHERE cr.course_id = ? ORDER BY cr.created_at DESC";

        try (Connection cnx = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, selectedMatiere.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String studentName = rs.getString("prenom") + " " + rs.getString("nom");
                int stars = rs.getInt("stars");
                String comment = rs.getString("comment");
                Date date = rs.getTimestamp("created_at");
                feedbackList.add(new FeedbackEntry(studentName, stars, comment, date));
            }
            statusLabel.setText("Loaded " + feedbackList.size() + " feedback entries for " + selectedMatiere.getTitre());

        } catch (SQLException e) {
            statusLabel.setText("Error loading feedback: " + e.getMessage());
        }
    }

    public static class FeedbackEntry {
        private final String studentName;
        private final int stars;
        private final String comment;
        private final Date date;

        public FeedbackEntry(String studentName, int stars, String comment, Date date) {
            this.studentName = studentName;
            this.stars = stars;
            this.comment = comment;
            this.date = date;
        }

        public String getStudentName() { return studentName; }
        public int getStars() { return stars; }
        public String getComment() { return comment; }
        public Date getDate() { return date; }
    }
}
