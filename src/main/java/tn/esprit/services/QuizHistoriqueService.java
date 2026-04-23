package tn.esprit.services;

import tn.esprit.entity.QuizResult;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizHistoriqueService — requêtes BDD pour l'historique admin des résultats quiz.
 * Équivalent de AdminQuizController::results() + QuizResultRepository Symfony.
 */
public class QuizHistoriqueService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ------------------------------------------------------------------ //
    //  Lecture                                                            //
    // ------------------------------------------------------------------ //

    /** Tous les résultats, ordre DESC par date */
    public List<QuizResult> findAllOrderedByDate() {
        List<QuizResult> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz_result ORDER BY created_at DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[QuizHistorique] findAll : " + e.getMessage());
        }
        return list;
    }

    /**
     * Compte les résultats dont l'etudiant_id correspond à un vrai
     * utilisateur dans la table user.
     * Équivalent Symfony : results|filter(r => r.result.etudiant is not null)
     */
    public int countWithRealEtudiant() {
        // Un etudiant_id est "réel" s'il existe dans la table user
        String sql = "SELECT COUNT(*) FROM quiz_result qr " +
                     "INNER JOIN user u ON u.id = qr.etudiant_id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[QuizHistorique] countWithRealEtudiant : " + e.getMessage());
        }
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  Suppression                                                        //
    // ------------------------------------------------------------------ //

    public boolean deleteById(int id) {
        String sql = "DELETE FROM quiz_result WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[QuizHistorique] deleteById : " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAll() {
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate("DELETE FROM quiz_result");
            return true;
        } catch (SQLException e) {
            System.err.println("[QuizHistorique] deleteAll : " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ //
    //  Nom de l'étudiant — JOIN sur la table user                        //
    // ------------------------------------------------------------------ //

    /**
     * Retourne "Prénom Nom" si l'etudiant_id existe dans la table user,
     * null sinon (= anonyme dans l'affichage).
     * Équivalent Symfony : result.etudiant?.nom ~ ' ' ~ result.etudiant?.prenom
     */
    public String getNomEtudiant(int etudiantId) {
        if (etudiantId <= 0) return null;
        // Essai avec colonnes nom + prenom
        String sql = "SELECT nom, prenom FROM user WHERE id=? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom    = rs.getString("nom")    != null ? rs.getString("nom").trim()    : "";
                String prenom = rs.getString("prenom") != null ? rs.getString("prenom").trim() : "";
                String full   = (nom + " " + prenom).trim();
                return full.isEmpty() ? null : full;
            }
        } catch (SQLException e) {
            // Colonnes différentes — essai avec 'name'
            try {
                String sql2 = "SELECT name FROM user WHERE id=? LIMIT 1";
                PreparedStatement ps2 = cnx.prepareStatement(sql2);
                ps2.setInt(1, etudiantId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) return rs2.getString("name");
            } catch (SQLException ignored) {}
        }
        return null; // ID existe pas dans user → anonyme
    }

    // ------------------------------------------------------------------ //
    //  Mapping                                                            //
    // ------------------------------------------------------------------ //

    private QuizResult mapRow(ResultSet rs) throws SQLException {
        return new QuizResult(
            rs.getInt("id"),
            rs.getInt("etudiant_id"),
            rs.getString("responses"),
            rs.getString("scores"),
            rs.getString("recommendations"),
            rs.getString("profile_type"),
            rs.getTimestamp("created_at")
        );
    }
}
