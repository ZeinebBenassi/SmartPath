package com.smartpath.service;

import com.smartpath.model.Matiere;
import com.smartpath.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatiereService {
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
        String sql = "INSERT INTO matiere (titre, description, is_visible) VALUES (?, ?, 1)";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, m.getTitre());
        ps.setString(2, m.getDescription());
        ps.executeUpdate();
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