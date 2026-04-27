package tn.esprit.services.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Lecon;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeconService {
    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public List<Lecon> getAll() throws SQLException {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon";
        ResultSet rs = getConnection().createStatement().executeQuery(sql);
        while (rs.next()) {
            list.add(new Lecon(rs.getInt("id"), rs.getString("titre"), rs.getString("contenu"), rs.getInt("matiere_id")));
        }
        return list;
    }

    public List<Lecon> getByMatiereId(int matiereId) throws SQLException {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon WHERE matiere_id=? ORDER BY id DESC";
        PreparedStatement ps = getConnection().prepareStatement(sql);
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
        Connection conn = getConnection();
        // Simplified column check for brevity, assuming standard structure or adapting like QuizService if needed
        String sql = "INSERT INTO lecon (titre, contenu, matiere_id) VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, l.getTitre());
        ps.setString(2, l.getContenu());
        ps.setInt(3, l.getMatiereId());
        ps.executeUpdate();
    }

    public void update(Lecon l) throws SQLException {
        String sql = "UPDATE lecon SET titre=?, contenu=?, matiere_id=? WHERE id=?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, l.getTitre());
        ps.setString(2, l.getContenu());
        ps.setInt(3, l.getMatiereId());
        ps.setInt(4, l.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM lecon WHERE id=?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
