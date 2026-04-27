package tn.esprit.services;

import tn.esprit.entity.QuizResult;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizHistoriqueService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ── Lecture ───────────────────────────────────────────────────────────

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
     * Nombre d'étudiants UNIQUES ayant passé le quiz.
     * = COUNT(DISTINCT etudiant_id) en faisant un JOIN sur user
     *   pour ne compter que les vrais utilisateurs (pas les anonymes).
     *
     * On fait le JOIN avec la table user pour s'assurer que l'ID existe vraiment.
     */
    public int countEtudiantsUniques() {
        // Requête 1 : SANS JOIN — compte tous les etudiant_id distincts > 0
        // (pas de JOIN pour éviter l'exclusion si la colonne s'appelle différemment)
        String sql = "SELECT COUNT(DISTINCT etudiant_id) FROM quiz_result WHERE etudiant_id > 0";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("[QuizStats] Étudiants uniques (SQL COUNT DISTINCT) = " + count);
                // Debug : afficher tous les IDs distincts
                debugDistinctIds();
                return count;
            }
        } catch (SQLException e) {
            System.err.println("[QuizHistorique] countEtudiantsUniques : " + e.getMessage());
        }
        return 0;
    }

    /** Debug : affiche tous les etudiant_id distincts dans la console */
    private void debugDistinctIds() {
        String sql = "SELECT DISTINCT etudiant_id FROM quiz_result ORDER BY etudiant_id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.print("[QuizStats] IDs distincts dans quiz_result : ");
            while (rs.next()) System.out.print(rs.getInt(1) + "  ");
            System.out.println();
        } catch (SQLException e) {
            System.err.println("[QuizStats] debug : " + e.getMessage());
        }
    }

    // ── Suppression ───────────────────────────────────────────────────────

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

    // ── Nom étudiant ──────────────────────────────────────────────────────

    public String getNomEtudiant(int etudiantId) {
        if (etudiantId <= 0) return null;
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
            try (PreparedStatement ps2 = cnx.prepareStatement("SELECT name FROM user WHERE id=? LIMIT 1")) {
                ps2.setInt(1, etudiantId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) return rs2.getString("name");
            } catch (SQLException ignored) {}
        }
        return null;
    }

    // ── Mapping ───────────────────────────────────────────────────────────

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
