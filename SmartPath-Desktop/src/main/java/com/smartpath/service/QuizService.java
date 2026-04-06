package com.smartpath.service;

import com.smartpath.model.Quiz;
import com.smartpath.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {

    public List<Quiz> getAll() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM test";
        ResultSet rs = DBConnection.getInstance().createStatement().executeQuery(sql);
        while (rs.next()) {
            Quiz q = new Quiz();
            q.setId(rs.getInt("id"));
            q.setTitre(rs.getString("titre"));
            q.setContenu(rs.getString("contenu") != null ? rs.getString("contenu") : "");
            q.setMatiereId(rs.getInt("matiere_id"));

            // Try duree — handles both possible column names safely
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
        // Detect correct column name first
        String dureCol = getDureeColumnName();
        String sql = "INSERT INTO test (titre, contenu, " + dureCol + ", matiere_id, created_at) VALUES (?, ?, ?, ?, NOW())";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, q.getTitre());
        ps.setString(2, q.getContenu());
        ps.setInt(3, q.getDuree());
        ps.setInt(4, q.getMatiereId());
        ps.executeUpdate();
    }

    public void update(Quiz q) throws SQLException {
        String dureCol = getDureeColumnName();
        String sql = "UPDATE test SET titre=?, contenu=?, " + dureCol + "=? WHERE id=?";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, q.getTitre());
        ps.setString(2, q.getContenu());
        ps.setInt(3, q.getDuree());
        ps.setInt(4, q.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        // Delete linked questions first to avoid foreign key errors
        PreparedStatement ps1 = DBConnection.getInstance()
                .prepareStatement("DELETE FROM question WHERE test_id=?");
        ps1.setInt(1, id);
        ps1.executeUpdate();

        // Then delete the quiz
        PreparedStatement ps2 = DBConnection.getInstance()
                .prepareStatement("DELETE FROM test WHERE id=?");
        ps2.setInt(1, id);
        ps2.executeUpdate();
    }

    // Auto-detects whether column is "duree" or "dureo" in your DB
    private String getDureeColumnName() {
        try {
            ResultSet rs = DBConnection.getInstance()
                    .createStatement()
                    .executeQuery("SELECT duree FROM test LIMIT 1");
            rs.close();
            return "duree";
        } catch (SQLException e) {
            return "dureo";
        }
    }
}