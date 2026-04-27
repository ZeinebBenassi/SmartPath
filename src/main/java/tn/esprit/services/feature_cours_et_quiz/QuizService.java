package tn.esprit.services.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Quiz;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizService {

    private static volatile Boolean testHasCreatedAt;
    private static volatile Boolean testHasProfId;

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public List<Quiz> getAll() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM test";
        ResultSet rs = getConnection().createStatement().executeQuery(sql);
        while (rs.next()) {
            Quiz q = new Quiz();
            q.setId(rs.getInt("id"));
            q.setTitre(rs.getString("titre"));
            q.setContenu(rs.getString("contenu") != null ? rs.getString("contenu") : "");
            q.setMatiereId(rs.getInt("matiere_id"));

            try {
                q.setDuree(rs.getInt("duree"));
            } catch (SQLException e) {
                try {
                    q.setDuree(rs.getInt("dureo"));
                } catch (SQLException ex) {
                    q.setDuree(0);
                }
            }
            list.add(q);
        }
        return list;
    }

    public void create(Quiz q) throws SQLException {
        createAndReturnId(q, null);
    }

    public int createAndReturnId(Quiz q, Integer profId) throws SQLException {
        Connection conn = getConnection();
        String dureCol = getDureeColumnName();
        boolean hasCreatedAt = tableHasColumn(conn, "test", "created_at");
        boolean hasProfId = tableHasColumn(conn, "test", "prof_id");

        Integer effectiveProfId = profId;
        if (hasProfId && (effectiveProfId == null || effectiveProfId <= 0)) {
            throw new SQLException("La table 'test' exige prof_id.");
        }

        String sql;
        if (hasProfId && hasCreatedAt) {
            sql = "INSERT INTO test (titre, contenu, " + dureCol + ", matiere_id, prof_id, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        } else if (hasProfId) {
            sql = "INSERT INTO test (titre, contenu, " + dureCol + ", matiere_id, prof_id) VALUES (?, ?, ?, ?, ?)";
        } else if (hasCreatedAt) {
            sql = "INSERT INTO test (titre, contenu, " + dureCol + ", matiere_id, created_at) VALUES (?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO test (titre, contenu, " + dureCol + ", matiere_id) VALUES (?, ?, ?, ?)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getTitre());
            ps.setString(2, q.getContenu());
            ps.setInt(3, q.getDuree());
            ps.setInt(4, q.getMatiereId());
            int idx = 5;
            if (hasProfId) ps.setInt(idx++, effectiveProfId);
            if (hasCreatedAt) ps.setTimestamp(idx, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys != null && keys.next()) return keys.getInt(1);
            }
        }
        return 0;
    }

    public void update(Quiz q) throws SQLException {
        String dureCol = getDureeColumnName();
        String sql = "UPDATE test SET titre=?, contenu=?, " + dureCol + "=? WHERE id=?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, q.getTitre());
        ps.setString(2, q.getContenu());
        ps.setInt(3, q.getDuree());
        ps.setInt(4, q.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        Connection conn = getConnection();
        String[] candidates = { "test_id", "quiz_id", "id_test", "id_quiz" };
        for (String col : candidates) {
            try {
                PreparedStatement ps1 = conn.prepareStatement("DELETE FROM question WHERE " + col + "=?");
                ps1.setInt(1, id);
                ps1.executeUpdate();
                break;
            } catch (SQLException ignored) {}
        }
        PreparedStatement ps2 = conn.prepareStatement("DELETE FROM test WHERE id=?");
        ps2.setInt(1, id);
        ps2.executeUpdate();
    }

    private String getDureeColumnName() {
        try {
            ResultSet rs = getConnection().createStatement().executeQuery("SELECT duree FROM test LIMIT 1");
            rs.close();
            return "duree";
        } catch (SQLException e) {
            return "dureo";
        }
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
}
