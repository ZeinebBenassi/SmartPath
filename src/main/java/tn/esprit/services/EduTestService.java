package tn.esprit.services;

import tn.esprit.entity.Test;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EduTestService {

    private static volatile Boolean testHasCreatedAt;
    private static volatile Boolean testHasProfId;
    private final Connection conn = MyDatabase.getInstance().getConnection();

    public List<Test> getAll() throws SQLException {
        List<Test> list = new ArrayList<>();
        String sql = "SELECT * FROM test";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            Test q = new Test();
            q.setId(rs.getInt("id"));
            q.setTitre(rs.getString("titre"));
            q.setContenu(rs.getString("contenu") != null ? rs.getString("contenu") : "");
            q.setMatiereId(rs.getInt("matiere_id"));

            // Try duree
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

    public void create(Test q) throws SQLException {
        createAndReturnId(q, null);
    }

    public int createAndReturnId(Test q, Integer profId) throws SQLException {
        // Detect correct column name first
        String dureCol = getDureeColumnName();
        boolean hasCreatedAt = tableHasColumn(conn, "test", "created_at");
        boolean hasProfId = tableHasColumn(conn, "test", "prof_id");

        Integer effectiveProfId = profId;
        if (hasProfId) {
            if (effectiveProfId == null || effectiveProfId <= 0) {
                effectiveProfId = q.getProfId();
            }
            if (effectiveProfId <= 0) {
                throw new SQLException("La table 'test' exige prof_id. Impossible de créer un quiz sans prof_id.");
            }
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
            if (hasProfId) {
                ps.setInt(idx++, effectiveProfId);
            }
            if (hasCreatedAt) {
                ps.setTimestamp(idx, new Timestamp(System.currentTimeMillis()));
            }
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return 0;
    }

    public void update(Test q) throws SQLException {
        String dureCol = getDureeColumnName();
        String sql = "UPDATE test SET titre=?, contenu=?, " + dureCol + "=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, q.getTitre());
        ps.setString(2, q.getContenu());
        ps.setInt(3, q.getDuree());
        ps.setInt(4, q.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        // Delete linked questions first
        SQLException last = null;
        String[] candidates = { "test_id", "quiz_id", "id_test", "id_quiz" };
        for (String col : candidates) {
            try {
                PreparedStatement ps1 = conn.prepareStatement("DELETE FROM question WHERE " + col + "=?");
                ps1.setInt(1, id);
                ps1.executeUpdate();
                last = null;
                break;
            } catch (SQLException e) {
                last = e;
            }
        }
        if (last != null && !last.getMessage().toLowerCase().contains("unknown column")) {
            throw last;
        }

        // Then delete the test
        PreparedStatement ps2 = conn.prepareStatement("DELETE FROM test WHERE id=?");
        ps2.setInt(1, id);
        ps2.executeUpdate();
    }

    private String getDureeColumnName() {
        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT duree FROM test LIMIT 1");
            rs.close();
            return "duree";
        } catch (SQLException e) {
            return "dureo";
        }
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        if ("test".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            Boolean cached = testHasCreatedAt;
            if (cached != null) return cached;
        }
        if ("test".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            Boolean cached = testHasProfId;
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

        if ("test".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) testHasCreatedAt = found;
        if ("test".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) testHasProfId = found;
        return found;
    }
}
