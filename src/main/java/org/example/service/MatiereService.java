package org.example.service;

import org.example.entity.Lecon;
import org.example.entity.Matiere;
import org.example.entity.Test;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatiereService {
    private Connection connection;

    public MatiereService() {
        try {
            this.connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // CRUD: CREATE
    public boolean add(Matiere m) {
        String query = "INSERT INTO matiere (titre, description, filiere_id, prof_id, is_visible) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, m.getTitre());
            pstmt.setString(2, m.getDescription());
            pstmt.setObject(3, m.getFiliereId() == 0 ? null : m.getFiliereId());
            pstmt.setInt(4, m.getProfId());
            pstmt.setBoolean(5, m.isVisible());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        m.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // CRUD: UPDATE
    public boolean update(Matiere m) {
        String query = "UPDATE matiere SET titre = ?, description = ?, is_visible = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, m.getTitre());
            pstmt.setString(2, m.getDescription());
            pstmt.setBoolean(3, m.isVisible());
            pstmt.setInt(4, m.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // CRUD: DELETE
    public boolean delete(int id) {
        String query = "DELETE FROM matiere WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // CRUD: READ with Filtering and Search
    public List<Matiere> findVisible(String search, Integer filiereId) {
        List<Matiere> matieres = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT * FROM matiere WHERE is_visible = 1");
        
        if (search != null && !search.isEmpty()) {
            query.append(" AND (titre LIKE ? OR description LIKE ?)");
        }
        if (filiereId != null) {
            query.append(" AND filiere_id = ?");
        }

        try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
            int paramIndex = 1;
            if (search != null && !search.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + search + "%");
                pstmt.setString(paramIndex++, "%" + search + "%");
            }
            if (filiereId != null) {
                pstmt.setInt(paramIndex++, filiereId);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Matiere m = new Matiere();
                m.setId(rs.getInt("id"));
                m.setTitre(rs.getString("titre"));
                m.setDescription(rs.getString("description"));
                m.setFiliereId(rs.getInt("filiere_id"));
                m.setProfId(rs.getInt("prof_id"));
                matieres.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matieres;
    }

    // READ DETAILS (Matiere + Lecons + Tests)
    public Matiere findWithDetails(int id) {
        Matiere m = null;
        String query = "SELECT * FROM matiere WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                m = new Matiere();
                m.setId(rs.getInt("id"));
                m.setTitre(rs.getString("titre"));
                m.setDescription(rs.getString("description"));
                m.setLecons(findLeconsByMatiere(id));
                m.setTests(findTestsByMatiere(id));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private List<Lecon> findLeconsByMatiere(int matiereId) {
        List<Lecon> lecons = new ArrayList<>();
        String query = "SELECT * FROM lecon WHERE matiere_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, matiereId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Lecon l = new Lecon();
                l.setId(rs.getInt("id"));
                l.setTitre(rs.getString("titre"));
                l.setDescription(rs.getString("description"));
                l.setContenu(rs.getString("contenu"));
                l.setFichier(rs.getString("fichier"));
                l.setDuree(rs.getInt("duree"));
                lecons.add(l);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lecons;
    }

    private List<Test> findTestsByMatiere(int matiereId) {
        List<Test> tests = new ArrayList<>();
        String query = "SELECT * FROM test WHERE matiere_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, matiereId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Test t = new Test();
                t.setId(rs.getInt("id"));
                t.setTitre(rs.getString("titre"));
                t.setDuree(rs.getInt("duree"));
                tests.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tests;
    }
}
