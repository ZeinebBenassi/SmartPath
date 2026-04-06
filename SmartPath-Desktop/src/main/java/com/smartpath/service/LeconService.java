package com.smartpath.service;

import com.smartpath.model.Lecon;
import com.smartpath.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeconService {
    public List<Lecon> getAll() throws SQLException {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon";
        ResultSet rs = DBConnection.getInstance().createStatement().executeQuery(sql);
        while (rs.next()) {
            list.add(new Lecon(rs.getInt("id"), rs.getString("titre"), rs.getString("contenu"), rs.getInt("matiere_id")));
        }
        return list;
    }

    public void create(Lecon l) throws SQLException {
        String sql = "INSERT INTO lecon (titre, contenu, matiere_id) VALUES (?, ?, ?)";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, l.getTitre());
        ps.setString(2, l.getContenu());
        ps.setInt(3, l.getMatiereId());
        ps.executeUpdate();
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
