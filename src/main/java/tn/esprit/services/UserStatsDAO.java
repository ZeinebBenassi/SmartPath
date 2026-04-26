package tn.esprit.services;

import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO dédié aux statistiques utilisateurs.
 *
 * ⚠️  La table dans SmartPath s'appelle  `user`  (singulier), pas `users`.
 *
 * Colonnes utilisées :
 *   user(id, nom, prenom, email, type, created_at, status, ...)
 *
 * La colonne `type` contient : "admin", "prof", "etudiant"
 */
public class UserStatsDAO {

    private final Connection conn;

    public UserStatsDAO() {
        this.conn = MyDatabase.getInstance().getConnection();
    }

    // ────────────────────────────────────────────────────────────────────────
    // 1. Nombre total d'utilisateurs
    // ────────────────────────────────────────────────────────────────────────

    public int getTotalUsers() {
        // On compte tous les enregistrements de la table `user`
        String sql = "SELECT COUNT(*) FROM `user`";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getTotalUsers : " + e.getMessage());
        }
        return 0;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 2. Répartition par rôle — pour PieChart
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Utilise la sous-requête CASE pour déduire le rôle :
     *   - s'il existe dans `etudiant` → "etudiant"
     *   - s'il existe dans `prof`     → "prof"
     *   - sinon                       → "admin"
     * Compatible même si la colonne `type` n'existe pas encore.
     */
    public Map<String, Integer> getUsersByRole() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("admin",    0);
        result.put("prof",     0);
        result.put("etudiant", 0);

        // Stratégie 1 : utiliser la colonne `type` si elle existe
        String sqlWithType = """
            SELECT LOWER(COALESCE(type, 'admin')) AS role, COUNT(*) AS nb
            FROM `user`
            GROUP BY role
            """;

        // Stratégie 2 (fallback) : déduire via les tables etudiant / prof
        String sqlFallback = """
            SELECT
              CASE
                WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'
                WHEN EXISTS (SELECT 1 FROM prof     p WHERE p.id = u.id) THEN 'prof'
                ELSE 'admin'
              END AS role,
              COUNT(*) AS nb
            FROM `user` u
            GROUP BY role
            """;

        try (PreparedStatement ps = conn.prepareStatement(sqlWithType);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String role = rs.getString("role");
                int count   = rs.getInt("nb");
                if (role != null) result.put(role, count);
            }
        } catch (SQLException e) {
            // Colonne `type` absente → fallback
            System.err.println("getUsersByRole (type) → fallback : " + e.getMessage());
            try (PreparedStatement ps = conn.prepareStatement(sqlFallback);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("role"), rs.getInt("nb"));
                }
            } catch (SQLException ex) {
                System.err.println("getUsersByRole (fallback) : " + ex.getMessage());
            }
        }
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. Inscriptions par JOUR — 30 derniers jours (LineChart)
    // ────────────────────────────────────────────────────────────────────────

    public Map<String, Integer> getRegistrationsByDay() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
            SELECT DATE(created_at) AS jour, COUNT(*) AS nb
            FROM `user`
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
            GROUP BY DATE(created_at)
            ORDER BY jour ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("jour"), rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getRegistrationsByDay : " + e.getMessage());
        }
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 4. Inscriptions par MOIS — 12 derniers mois (LineChart)
    // ────────────────────────────────────────────────────────────────────────

    public Map<String, Integer> getRegistrationsByMonth() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS mois, COUNT(*) AS nb
            FROM `user`
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
            GROUP BY mois
            ORDER BY mois ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("mois"), rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getRegistrationsByMonth : " + e.getMessage());
        }
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 5. Nouveaux inscrits aujourd'hui
    // ────────────────────────────────────────────────────────────────────────

    public int getRegistrationsToday() {
        String sql = "SELECT COUNT(*) FROM `user` WHERE DATE(created_at) = CURDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getRegistrationsToday : " + e.getMessage());
        }
        return 0;
    }
}
