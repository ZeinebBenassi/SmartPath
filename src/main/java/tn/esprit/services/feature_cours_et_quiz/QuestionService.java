package tn.esprit.services.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Question;
import tn.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class QuestionService {
    private static volatile String cachedQuizFkColumn;

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public List<Question> getAll() throws SQLException {
        List<Question> list = new ArrayList<>();
        ResultSet rs = getConnection().createStatement()
                .executeQuery("SELECT * FROM question");
        while (rs.next()) {
            Question q = new Question(
                    rs.getInt("id"),
                    rs.getString("text"),
                    rs.getString("category")
            );
            try { q.setOrdre(rs.getInt("ordre")); } catch (SQLException ignored) {}
            try { q.setActive(rs.getInt("is_active") == 1); } catch (SQLException ignored) {}
            try { q.setTestId(rs.getInt("test_id")); } catch (SQLException ignored) {}
            list.add(q);
        }
        return list;
    }

    public List<Question> getByTestId(int testId) throws SQLException {
        List<Question> list = new ArrayList<>();
        try {
            String fkCol = detectQuizFkColumn();
            String sql = "SELECT * FROM question WHERE " + fkCol + "=? ORDER BY ordre ASC, id ASC";
            PreparedStatement ps = getConnection().prepareStatement(sql);
            ps.setInt(1, testId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("text"),
                        rs.getString("category")
                );
                q.setTestId(testId);
                try { q.setOrdre(rs.getInt("ordre")); } catch (SQLException ignored) { q.setOrdre(0); }
                boolean active = true;
                try { active = rs.getInt("is_active") == 1; } catch (SQLException ignored) {}
                q.setActive(active);
                if (q.isActive()) {
                    list.add(q);
                }
            }
            return list;
        } catch (SQLException noFk) {
            String sql = "SELECT * FROM question ORDER BY ordre ASC, id ASC";
            ResultSet rs = getConnection().createStatement().executeQuery(sql);
            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("text"),
                        rs.getString("category")
                );
                q.setTestId(testId);
                try { q.setOrdre(rs.getInt("ordre")); } catch (SQLException ignored) { q.setOrdre(0); }
                boolean active = true;
                try { active = rs.getInt("is_active") == 1; } catch (SQLException ignored) {}
                q.setActive(active);
                if (q.isActive()) {
                    list.add(q);
                }
            }
            return list;
        }
    }

    private String detectQuizFkColumn() throws SQLException {
        if (cachedQuizFkColumn != null) return cachedQuizFkColumn;
        Connection conn = getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "question", null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        String[] candidates = { "test_id", "quiz_id", "id_test", "id_quiz" };
        for (String candidate : candidates) {
            if (columns.contains(candidate)) {
                cachedQuizFkColumn = candidate;
                return candidate;
            }
        }
        throw new SQLException("Impossible de trouver la colonne de liaison quiz->question.");
    }

    public void create(Question q) throws SQLException {
        createForTestId(q.getTestId(), q, null);
    }

    public void createForTestId(int testId, Question q, Integer profId) throws SQLException {
        Connection conn = getConnection();
        String fkCol = null;
        try { fkCol = detectQuizFkColumn(); } catch (SQLException ignored) {}
        
        String sql = (fkCol != null && testId > 0)
            ? "INSERT INTO question (text, category, ordre, is_active, " + fkCol + ") VALUES (?, ?, ?, 1, ?)"
            : "INSERT INTO question (text, category, ordre, is_active) VALUES (?, ?, ?, 1)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getText());
            ps.setString(2, q.getCategory());
            ps.setInt(3, q.getOrdre());
            if (fkCol != null && testId > 0) ps.setInt(4, testId);
            ps.executeUpdate();
        }
    }

    public void update(Question q) throws SQLException {
        String sql = "UPDATE question SET text=?, category=? WHERE id=?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, q.getText());
        ps.setString(2, q.getCategory());
        ps.setInt(3, q.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("DELETE FROM question WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
