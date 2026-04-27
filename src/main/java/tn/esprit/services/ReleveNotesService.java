package tn.esprit.services;

import tn.esprit.entity.ReleveNotes;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour la table releve_notes.
 * Équivalent du ReleveNotesRepository Symfony.
 */
public class ReleveNotesService {

    private final Connection connection;

    public ReleveNotesService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    public void save(ReleveNotes releve) {
        String sql = """
            INSERT INTO releve_notes
              (etudiant_id, fichier_path, fichier_type, texte_extrait,
               notes_detectees, score_par_filiere, filiere_recommandee,
               analyse_ia, moyenne_generale, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    releve.getEtudiantId());
            ps.setString(2, releve.getFichierPath());
            ps.setString(3, releve.getFichierType());
            ps.setString(4, releve.getTexteExtrait());
            ps.setString(5, releve.getNotesDetectees());
            ps.setString(6, releve.getScoreParFiliere());
            ps.setString(7, releve.getFiliereRecommandee());
            ps.setString(8, releve.getAnalyseIA());
            ps.setDouble(9, releve.getMoyenneGenerale());
            ps.setTimestamp(10, new Timestamp(releve.getCreatedAt().getTime()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) releve.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur sauvegarde releve_notes : " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────

    public List<ReleveNotes> findByEtudiantId(int etudiantId) {
        List<ReleveNotes> list = new ArrayList<>();
        String sql = "SELECT * FROM releve_notes WHERE etudiant_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByEtudiantId : " + e.getMessage(), e);
        }
        return list;
    }

    public ReleveNotes findById(int id) {
        String sql = "SELECT * FROM releve_notes WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById : " + e.getMessage(), e);
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    public void delete(int id) {
        String sql = "DELETE FROM releve_notes WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression releve_notes : " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // MAPPING
    // ─────────────────────────────────────────────

    private ReleveNotes map(ResultSet rs) throws SQLException {
        ReleveNotes r = new ReleveNotes();
        r.setId(rs.getInt("id"));
        r.setEtudiantId(rs.getInt("etudiant_id"));
        r.setFichierPath(rs.getString("fichier_path"));
        r.setFichierType(rs.getString("fichier_type"));
        r.setTexteExtrait(rs.getString("texte_extrait"));
        r.setNotesDetectees(rs.getString("notes_detectees"));
        r.setScoreParFiliere(rs.getString("score_par_filiere"));
        r.setFiliereRecommandee(rs.getString("filiere_recommandee"));
        r.setAnalyseIA(rs.getString("analyse_ia"));
        r.setMoyenneGenerale(rs.getDouble("moyenne_generale"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(new java.util.Date(ts.getTime()));
        return r;
    }
}
