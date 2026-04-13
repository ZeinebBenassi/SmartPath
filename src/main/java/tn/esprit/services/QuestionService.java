package tn.esprit.services;

import tn.esprit.entity.Answer;
import tn.esprit.entity.Question;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements ICrud<Question> {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Question q) throws SQLException {
        if (q.getText() == null || q.getText().isEmpty())
            throw new IllegalArgumentException("Le texte de la question est obligatoire");
        if (q.getCategory() == null || q.getCategory().isEmpty())
            throw new IllegalArgumentException("La catégorie est obligatoire");
        if (q.getOrdre() <= 0) q.setOrdre(getNextOrdre());

        String sql = "INSERT INTO question (text, category, ordre, is_active) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getText());
            ps.setString(2, q.getCategory());
            ps.setInt(3, q.getOrdre());
            ps.setBoolean(4, q.isActive());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) q.setId(rs.getInt(1));
        }
        if (q.getAnswers() != null && !q.getAnswers().isEmpty())
            ajouterAnswers(q.getId(), q.getAnswers());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM answer WHERE question_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM question WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Question q) throws SQLException {
        String sql = "UPDATE question SET text=?, category=?, ordre=?, is_active=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getText()); ps.setString(2, q.getCategory());
            ps.setInt(3, q.getOrdre()); ps.setBoolean(4, q.isActive());
            ps.setInt(5, q.getId()); ps.executeUpdate();
        }
        if (q.getAnswers() != null && !q.getAnswers().isEmpty())
            modifierAnswers(q.getAnswers());
    }

    @Override
    public List<Question> afficher() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question ORDER BY ordre ASC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Question q = mapRow(rs);
                q.setAnswers(getAnswersByQuestion(q.getId()));
                list.add(q);
            }
        }
        return list;
    }

    public Question getById(int id) throws SQLException {
        String sql = "SELECT * FROM question WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Question q = mapRow(rs);
                q.setAnswers(getAnswersByQuestion(q.getId()));
                return q;
            }
        }
        return null;
    }

    public void toggleActive(int id) throws SQLException {
        String sql = "UPDATE question SET is_active = NOT is_active WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public int countActiveQuestions() throws SQLException {
        String sql = "SELECT COUNT(*) FROM question WHERE is_active=1";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private List<Answer> getAnswersByQuestion(int questionId) throws SQLException {
        List<Answer> answers = new ArrayList<>();
        String sql = "SELECT * FROM answer WHERE question_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                answers.add(new Answer(rs.getInt("id"), rs.getString("text"),
                        rs.getInt("points"), rs.getString("trait"), rs.getInt("question_id")));
            }
        }
        return answers;
    }

    private void ajouterAnswers(int questionId, List<Answer> answers) throws SQLException {
        String sql = "INSERT INTO answer (text, points, trait, question_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Answer a : answers) {
                ps.setString(1, a.getText()); ps.setInt(2, a.getPoints());
                ps.setString(3, a.getTrait()); ps.setInt(4, questionId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void modifierAnswers(List<Answer> answers) throws SQLException {
        String sql = "UPDATE answer SET text=?, points=?, trait=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Answer a : answers) {
                ps.setString(1, a.getText()); ps.setInt(2, a.getPoints());
                ps.setString(3, a.getTrait()); ps.setInt(4, a.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private int getNextOrdre() throws SQLException {
        String sql = "SELECT MAX(ordre) FROM question";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1) + 1;
        }
        return 1;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        return new Question(rs.getInt("id"), rs.getString("text"), rs.getString("category"),
                rs.getInt("ordre"), rs.getBoolean("is_active"));
    }
}
