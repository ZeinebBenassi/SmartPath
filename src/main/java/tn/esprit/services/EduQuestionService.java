package tn.esprit.services;

import tn.esprit.entity.Question;
import tn.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class EduQuestionService {
    private static volatile String cachedQuizFkColumn;
    private static volatile Boolean questionHasCreatedAt;
    private static volatile Boolean questionHasProfId;
    private final Connection conn = MyDatabase.getInstance().getConnection();

    public List<Question> getAll() throws SQLException {
        List<Question> list = new ArrayList<>();
        ResultSet rs = conn.createStatement()
                .executeQuery("SELECT * FROM question");
        while (rs.next()) {
            Question q = new Question();
            q.setId(rs.getInt("id"));
            q.setText(rs.getString("text"));
            q.setCategory(rs.getString("category"));
            try { q.setOrdre(rs.getInt("ordre")); } catch (SQLException ignored) {}
            try { q.setActive(rs.getInt("is_active") == 1); } catch (SQLException ignored) {}
            // Note: tn.esprit.entity.Question doesn't have testId/quizId field.
            // If you need it, you might need to handle it via a wrapper or by adding it.
            list.add(q);
        }
        return list;
    }

    public List<Question> getByTestId(int testId) throws SQLException {
        List<Question> list = new ArrayList<>();
        try {
            String fkCol = detectQuizFkColumn();
            String sql = "SELECT * FROM question WHERE " + fkCol + "=? ORDER BY ordre ASC, id ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, testId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setText(rs.getString("text"));
                q.setCategory(rs.getString("category"));
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
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setText(rs.getString("text"));
                q.setCategory(rs.getString("category"));
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
        String cached = cachedQuizFkColumn;
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        DatabaseMetaData meta = conn.getMetaData();

        Set<String> columns = new HashSet<>();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "question", null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name != null) {
                    columns.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (columns.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "Question", null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null) {
                        columns.add(name.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }

        String[] candidates = { "test_id", "quiz_id", "id_test", "id_quiz", "testid", "quizid" };
        for (String candidate : candidates) {
            if (columns.contains(candidate)) {
                cachedQuizFkColumn = candidate;
                return candidate;
            }
        }

        for (String candidate : candidates) {
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM question WHERE " + candidate + "=? LIMIT 1");
                ps.setInt(1, 0);
                ps.executeQuery().close();
                cachedQuizFkColumn = candidate;
                return candidate;
            } catch (SQLException e) {
                String msg = (e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT));
                if (!msg.contains("unknown column") && !msg.contains("column") && !msg.contains("doesn't exist")) {
                    throw e;
                }
            }
        }

        throw new SQLException("Impossible de trouver la colonne de liaison quiz->question (ex: test_id / quiz_id). Vérifiez la table 'question'.");
    }

    public void createForTestId(int testId, Question q, Integer profId) throws SQLException {
        String fkCol = null;
        try {
            fkCol = detectQuizFkColumn();
        } catch (SQLException ignored) {}
        boolean hasCreatedAt = tableHasColumn(conn, "question", "created_at");
        boolean hasProfId = tableHasColumn(conn, "question", "prof_id");

        Integer effectiveProfId = profId;
        if (hasProfId) {
            if (effectiveProfId == null || effectiveProfId <= 0) {
                throw new SQLException("La table 'question' exige prof_id. Impossible d'ajouter une question sans prof_id.");
            }
        }

        boolean includeFk = fkCol != null && !fkCol.isBlank() && testId > 0;

        String sql;
        if (includeFk && hasProfId && hasCreatedAt) {
            sql = "INSERT INTO question (text, category, ordre, is_active, " + fkCol + ", prof_id, created_at) VALUES (?, ?, ?, 1, ?, ?, ?)";
        } else if (includeFk && hasProfId) {
            sql = "INSERT INTO question (text, category, ordre, is_active, " + fkCol + ", prof_id) VALUES (?, ?, ?, 1, ?, ?)";
        } else if (includeFk && hasCreatedAt) {
            sql = "INSERT INTO question (text, category, ordre, is_active, " + fkCol + ", created_at) VALUES (?, ?, ?, 1, ?, ?)";
        } else if (includeFk) {
            sql = "INSERT INTO question (text, category, ordre, is_active, " + fkCol + ") VALUES (?, ?, ?, 1, ?)";
        } else if (hasProfId && hasCreatedAt) {
            sql = "INSERT INTO question (text, category, ordre, is_active, prof_id, created_at) VALUES (?, ?, ?, 1, ?, ?)";
        } else if (hasProfId) {
            sql = "INSERT INTO question (text, category, ordre, is_active, prof_id) VALUES (?, ?, ?, 1, ?)";
        } else if (hasCreatedAt) {
            sql = "INSERT INTO question (text, category, ordre, is_active, created_at) VALUES (?, ?, ?, 1, ?)";
        } else {
            sql = "INSERT INTO question (text, category, ordre, is_active) VALUES (?, ?, ?, 1)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, q.getText());
            ps.setString(idx++, q.getCategory());
            ps.setInt(idx++, q.getOrdre());
            if (includeFk) {
                ps.setInt(idx++, testId);
            }
            if (hasProfId) {
                ps.setInt(idx++, effectiveProfId);
            }
            if (hasCreatedAt) {
                ps.setTimestamp(idx, new Timestamp(System.currentTimeMillis()));
            }
            ps.executeUpdate();
        }
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        if ("question".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            Boolean cached = questionHasCreatedAt;
            if (cached != null) return cached;
        }
        if ("question".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            Boolean cached = questionHasProfId;
            if (cached != null) return cached;
        }

        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        boolean found = false;

        try (ResultSet rs = meta.getColumns(catalog, null, table, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name != null && name.toLowerCase(Locale.ROOT).equals(column.toLowerCase(Locale.ROOT))) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            try (ResultSet rs = meta.getColumns(catalog, null, table.toUpperCase(Locale.ROOT), null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null && name.toLowerCase(Locale.ROOT).equals(column.toLowerCase(Locale.ROOT))) {
                        found = true;
                        break;
                    }
                }
            }
        }

        if ("question".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            questionHasCreatedAt = found;
        }
        if ("question".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            questionHasProfId = found;
        }
        return found;
    }

    public void update(Question q) throws SQLException {
        String sql = "UPDATE question SET text=?, category=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, q.getText());
        ps.setString(2, q.getCategory());
        ps.setInt(3, q.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM question WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
