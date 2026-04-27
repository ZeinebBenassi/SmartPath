package tn.esprit.services;

import tn.esprit.entity.Notification;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour les notifications admin.
 *
 * Table SQL requise (à créer une seule fois) :
 * ──────────────────────────────────────────────────────
 * CREATE TABLE IF NOT EXISTS notification (
 *     id             INT AUTO_INCREMENT PRIMARY KEY,
 *     etudiant_id    INT         NOT NULL,
 *     etudiant_nom   VARCHAR(100) NOT NULL,
 *     etudiant_prenom VARCHAR(100) NOT NULL,
 *     profile_type   VARCHAR(100) NOT NULL,
 *     created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     is_read        TINYINT(1)  NOT NULL DEFAULT 0
 * );
 * ──────────────────────────────────────────────────────
 */
public class NotificationService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ── Création de la table si elle n'existe pas ─────────────────────────
    public NotificationService() {
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notification (" +
                "  id              INT AUTO_INCREMENT PRIMARY KEY," +
                "  etudiant_id     INT          NOT NULL," +
                "  etudiant_nom    VARCHAR(100) NOT NULL," +
                "  etudiant_prenom VARCHAR(100) NOT NULL," +
                "  profile_type    VARCHAR(100) NOT NULL," +
                "  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  is_read         TINYINT(1)   NOT NULL DEFAULT 0" +
                ")"
            );
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur création table : " + e.getMessage());
        }
    }

    // ── Créer une notification ─────────────────────────────────────────────
    /**
     * Appelé depuis QuizPlayerController dès qu'un étudiant soumet son quiz.
     */
    public void create(int etudiantId, String nom, String prenom, String profileType) {
        String sql = "INSERT INTO notification (etudiant_id, etudiant_nom, etudiant_prenom, profile_type, created_at, is_read) " +
                     "VALUES (?, ?, ?, ?, NOW(), 0)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ps.setString(2, nom);
            ps.setString(3, prenom);
            ps.setString(4, profileType);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur create : " + e.getMessage());
        }
    }

    // ── Lire toutes les notifications (non lues en premier) ───────────────
    public List<Notification> findAll() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification ORDER BY is_read ASC, created_at DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur findAll : " + e.getMessage());
        }
        return list;
    }

    // ── Compter les non lues ──────────────────────────────────────────────
    public int countUnread() {
        String sql = "SELECT COUNT(*) FROM notification WHERE is_read = 0";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur countUnread : " + e.getMessage());
        }
        return 0;
    }

    // ── Marquer une notification comme lue ───────────────────────────────
    public void markAsRead(int notificationId) {
        String sql = "UPDATE notification SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur markAsRead : " + e.getMessage());
        }
    }

    // ── Marquer toutes comme lues ─────────────────────────────────────────
    public void markAllAsRead() {
        String sql = "UPDATE notification SET is_read = 1 WHERE is_read = 0";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur markAllAsRead : " + e.getMessage());
        }
    }

    // ── Supprimer une notification ────────────────────────────────────────
    public void delete(int notificationId) {
        String sql = "DELETE FROM notification WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService] Erreur delete : " + e.getMessage());
        }
    }

    // ── Mapping ResultSet → Notification ─────────────────────────────────
    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setEtudiantId(rs.getInt("etudiant_id"));
        n.setEtudiantNom(rs.getString("etudiant_nom"));
        n.setEtudiantPrenom(rs.getString("etudiant_prenom"));
        n.setProfileType(rs.getString("profile_type"));
        Timestamp ts = rs.getTimestamp("created_at");
        n.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        n.setRead(rs.getBoolean("is_read"));
        return n;
    }
}
