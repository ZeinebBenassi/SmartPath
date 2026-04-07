package services;

import models.Etudiant;
import models.Prof;
import models.User;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private final Connection connection;
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    public UserService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public User login(String email, String password) {
        String sql = "SELECT id, nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, created_at " +
                "FROM `user` WHERE email = ? AND password = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = mapUser(rs);
                user.setType(resolveType(user.getId(), user.getRoles()));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Erreur login: " + e.getMessage());
            return null;
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM `user` WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean registerProf(Prof prof) {
        return registerUser(prof, "[\"ROLE_ENSEIGNANT\"]", "prof", "INSERT INTO prof (id, specialite) VALUES (?, ?)");
    }

    public boolean registerEtudiant(Etudiant etudiant) {
        return registerUser(etudiant, "[\"ROLE_ETUDIANT\"]", "etudiant", "INSERT INTO etudiant (id, niveau, status, suspended_until, filiere_id) VALUES (?, ?, 'actif', NULL, NULL)");
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, created_at FROM `user` ORDER BY id DESC";

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = mapUser(rs);
                user.setType(resolveType(user.getId(), user.getRoles()));
                users.add(user);
            }
        } catch (SQLException e) {
            System.out.println("Erreur findAll: " + e.getMessage());
        }

        return users;
    }

    public void delete(int id) {
        String deleteAdmin = "DELETE FROM admin WHERE id = ?";
        String deleteProf = "DELETE FROM prof WHERE id = ?";
        String deleteEtudiant = "DELETE FROM etudiant WHERE id = ?";
        String deleteUser = "DELETE FROM `user` WHERE id = ?";

        try {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);

            deleteById(deleteAdmin, id);
            deleteById(deleteProf, id);
            deleteById(deleteEtudiant, id);
            deleteById(deleteUser, id);

            connection.commit();
            connection.setAutoCommit(previous);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Erreur suppression user: " + e.getMessage(), e);
        }
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM `user`");
    }

    public int countByType(String type) {
        return switch (type) {
            case "admin" -> count("SELECT COUNT(*) FROM admin");
            case "prof" -> count("SELECT COUNT(*) FROM prof");
            case "etudiant" -> count("SELECT COUNT(*) FROM etudiant");
            default -> 0;
        };
    }

    public String generateResetToken(String email) {
        if (!emailExists(email)) {
            return null;
        }
        String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        resetTokens.put(email, token);
        return token;
    }

    public boolean resetPassword(String email, String token, String newPassword) {
        String expected = resetTokens.get(email);
        if (expected == null || !expected.equals(token)) {
            return false;
        }

        String sql = "UPDATE `user` SET password = ? WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, email);
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                resetTokens.remove(email);
            }
            return success;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean registerUser(User user, String roles, String defaultType, String insertRoleSql) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, type, roles, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);

            int newUserId;
            try (PreparedStatement userStmt = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, user.getNom());
                userStmt.setString(2, user.getPrenom());
                userStmt.setString(3, user.getEmail());
                userStmt.setString(4, user.getPassword());
                userStmt.setString(5, user.getCin());
                userStmt.setString(6, user.getTelephone());
                userStmt.setString(7, user.getAdresse());

                LocalDate dateNaissance = user.getDateNaissance();
                if (dateNaissance != null) {
                    userStmt.setDate(8, java.sql.Date.valueOf(dateNaissance));
                } else {
                    userStmt.setDate(8, null);
                }

                userStmt.setString(9, user.getPhoto());
                userStmt.setString(10, defaultType);
                userStmt.setString(11, roles);
                userStmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                userStmt.executeUpdate();

                try (ResultSet keys = userStmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        connection.setAutoCommit(previous);
                        return false;
                    }
                    newUserId = keys.getInt(1);
                }
            }

            try (PreparedStatement roleStmt = connection.prepareStatement(insertRoleSql)) {
                roleStmt.setInt(1, newUserId);
                if ("prof".equals(defaultType)) {
                    roleStmt.setString(2, ((Prof) user).getSpecialite());
                } else {
                    roleStmt.setString(2, ((Etudiant) user).getNiveau());
                }
                roleStmt.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(previous);
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return false;
        }
    }

    private void deleteById(String sql, int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private int count(String sql) {
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private String resolveType(int userId, String roles) {
        if (roles != null) {
            String normalized = roles.toLowerCase();
            if (normalized.contains("admin")) {
                return "admin";
            }
            if (normalized.contains("enseignant") || normalized.contains("prof")) {
                return "prof";
            }
            if (normalized.contains("etudiant")) {
                return "etudiant";
            }
        }

        if (existsInTable("admin", userId)) {
            return "admin";
        }
        if (existsInTable("prof", userId)) {
            return "prof";
        }
        if (existsInTable("etudiant", userId)) {
            return "etudiant";
        }
        return "etudiant";
    }

    private boolean existsInTable(String table, int userId) {
        String sql = "SELECT 1 FROM " + table + " WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setCin(rs.getString("CIN"));
        user.setTelephone(rs.getString("telephone"));
        user.setAdresse(rs.getString("adresse"));

        java.sql.Date dateNaissance = rs.getDate("date_naissance");
        if (dateNaissance != null) {
            user.setDateNaissance(dateNaissance.toLocalDate());
        }

        user.setPhoto(rs.getString("photo"));
        user.setRoles(rs.getString("roles"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        return user;
    }
}
