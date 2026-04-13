package com.smartpath.service;

import com.smartpath.model.Matiere;
import com.smartpath.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatiereService {
    private static volatile Boolean matiereHasProfId;

    public List<Matiere> getAll() throws SQLException {
        List<Matiere> list = new ArrayList<>();
        ResultSet rs = DBConnection.getInstance().createStatement()
                .executeQuery("SELECT * FROM matiere");
        while (rs.next()) {
            list.add(new Matiere(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description")
            ));
        }
        return list;
    }

    public void create(Matiere m) throws SQLException {
        create(m, null);
    }

    public void create(Matiere m, Integer profId) throws SQLException {
        Connection conn = DBConnection.getInstance();
        boolean hasProfId = tableHasColumn(conn, "matiere", "prof_id");

        Integer effectiveProfId = profId;
        if (hasProfId) {
            if (effectiveProfId == null || effectiveProfId <= 0) {
                throw new SQLException("La table 'matiere' exige prof_id. Veuillez vous reconnecter.");
            }
        }

        String sql = hasProfId
                ? "INSERT INTO matiere (titre, description, is_visible, prof_id) VALUES (?, ?, 1, ?)"
                : "INSERT INTO matiere (titre, description, is_visible) VALUES (?, ?, 1)";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, m.getTitre());
        ps.setString(2, m.getDescription());
        if (hasProfId) {
            ps.setInt(3, effectiveProfId);
        }
        ps.executeUpdate();
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        if ("matiere".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            Boolean cached = matiereHasProfId;
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

        if ("matiere".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            matiereHasProfId = found;
        }
        return found;
    }

    public void update(Matiere m) throws SQLException {
        String sql = "UPDATE matiere SET titre=?, description=? WHERE id=?";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, m.getTitre());
        ps.setString(2, m.getDescription());
        ps.setInt(3, m.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        Connection conn = DBConnection.getInstance();

        // 1. Delete linked lecons first
        PreparedStatement ps1 = conn.prepareStatement("DELETE FROM lecon WHERE matiere_id=?");
        ps1.setInt(1, id);
        ps1.executeUpdate();

        // 2. Delete linked tests
        PreparedStatement ps2 = conn.prepareStatement("DELETE FROM test WHERE matiere_id=?");
        ps2.setInt(1, id);
        ps2.executeUpdate();

        // 3. Now delete the matiere safely
        PreparedStatement ps3 = conn.prepareStatement("DELETE FROM matiere WHERE id=?");
        ps3.setInt(1, id);
        ps3.executeUpdate();
    }
}