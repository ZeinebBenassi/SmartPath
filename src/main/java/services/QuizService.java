package services;

import tn.esprit.entity.Answer;
import tn.esprit.entity.Question;
import tn.esprit.entity.QuizResult;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public List<Question> findActiveQuestionsForQuiz() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE is_active=1 ORDER BY ordre ASC LIMIT 15";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Question(rs.getInt("id"), rs.getString("text"),
                        rs.getString("category"), rs.getInt("ordre"), rs.getBoolean("is_active")));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement questions : " + e.getMessage());
        }
        return list;
    }

    public List<Answer> findAnswersByQuestion(int questionId) {
        List<Answer> list = new ArrayList<>();
        String sql = "SELECT * FROM answer WHERE question_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Answer(rs.getInt("id"), rs.getString("text"),
                        rs.getInt("points"), rs.getString("trait"), rs.getInt("question_id")));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement réponses : " + e.getMessage());
        }
        return list;
    }

    public Answer findAnswerById(int answerId) {
        String sql = "SELECT * FROM answer WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, answerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Answer(rs.getInt("id"), rs.getString("text"),
                        rs.getInt("points"), rs.getString("trait"), rs.getInt("question_id"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAnswerById : " + e.getMessage());
        }
        return null;
    }

    public void saveQuizResult(QuizResult result) {
        String sql = "INSERT INTO quiz_result (etudiant_id, responses, scores, recommendations, profile_type, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, result.getEtudiantId());
            ps.setString(2, result.getResponses());
            ps.setString(3, result.getScores());
            ps.setString(4, result.getRecommendations());
            ps.setString(5, result.getProfileType());
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) result.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde QuizResult : " + e.getMessage());
        }
    }
}
