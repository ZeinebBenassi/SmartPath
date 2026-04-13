package com.smartpath.service;

import com.smartpath.model.Lecon;
import com.smartpath.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeconService {
    private static volatile Boolean leconHasCreatedAt;
    private static volatile Boolean leconHasProfId;

    public List<Lecon> getAll() throws SQLException {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon";
        ResultSet rs = DBConnection.getInstance().createStatement().executeQuery(sql);
        while (rs.next()) {
            list.add(new Lecon(rs.getInt("id"), rs.getString("titre"), rs.getString("contenu"), rs.getInt("matiere_id")));
        }
        return list;
    }

    public List<Lecon> getByMatiereId(int matiereId) throws SQLException {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon WHERE matiere_id=? ORDER BY id DESC";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setInt(1, matiereId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Lecon(rs.getInt("id"), rs.getString("titre"), rs.getString("contenu"), rs.getInt("matiere_id")));
        }
        return list;
    }

    public void create(Lecon l) throws SQLException {
        create(l, null);
    }

    public void create(Lecon l, Integer profId) throws SQLException {
        Connection conn = DBConnection.getInstance();
        boolean hasCreatedAt = tableHasColumn(conn, "lecon", "created_at");
        boolean hasProfId = tableHasColumn(conn, "lecon", "prof_id");

        Integer effectiveProfId = profId;
        if (hasProfId) {
            if (effectiveProfId == null || effectiveProfId <= 0) {
                throw new SQLException("La table 'lecon' exige prof_id. Veuillez vous reconnecter.");
            }
        }

        String sql;
        if (hasProfId && hasCreatedAt) {
            sql = "INSERT INTO lecon (titre, contenu, matiere_id, prof_id, created_at) VALUES (?, ?, ?, ?, ?)";
        } else if (hasProfId) {
            sql = "INSERT INTO lecon (titre, contenu, matiere_id, prof_id) VALUES (?, ?, ?, ?)";
        } else if (hasCreatedAt) {
            sql = "INSERT INTO lecon (titre, contenu, matiere_id, created_at) VALUES (?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO lecon (titre, contenu, matiere_id) VALUES (?, ?, ?)";
        }

        PreparedStatement ps = conn.prepareStatement(sql);
        int idx = 1;
        ps.setString(idx++, l.getTitre());
        ps.setString(idx++, l.getContenu());
        ps.setInt(idx++, l.getMatiereId());
        if (hasProfId) {
            ps.setInt(idx++, effectiveProfId);
        }
        if (hasCreatedAt) {
            ps.setTimestamp(idx, new Timestamp(System.currentTimeMillis()));
        }
        ps.executeUpdate();
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        if ("lecon".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            Boolean cached = leconHasCreatedAt;
            if (cached != null) return cached;
        }
        if ("lecon".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            Boolean cached = leconHasProfId;
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
            // Try different table case (some MySQL configs)
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

        if ("lecon".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            leconHasCreatedAt = found;
        }
        if ("lecon".equalsIgnoreCase(table) && "prof_id".equalsIgnoreCase(column)) {
            leconHasProfId = found;
        }
        return found;
    }

    public void update(Lecon l) throws SQLException {
        String sql = "UPDATE lecon SET titre=?, contenu=?, matiere_id=? WHERE id=?";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, l.getTitre());
        ps.setString(2, l.getContenu());
        ps.setInt(3, l.getMatiereId());
        ps.setInt(4, l.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM lecon WHERE id=?";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
