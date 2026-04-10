package services;

import models.Etudiant;
import models.Prof;
import models.User;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    private final Connection connection;
    private final Map<String, String> resetTokensByEmail = new ConcurrentHashMap<>();

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public User login(String email, String password) {
        String sql = "SELECT u.*," +
                " CASE" +
                "   WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'" +
                "   WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'" +
                "   ELSE 'admin'" +
                " END AS user_type" +
                " FROM `user` u WHERE u.email=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (isPasswordMatch(password, storedPassword)) {
                        return mapUser(rs);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur login: " + e.getMessage());
        }
        return null;
    }

    private boolean isPasswordMatch(String plainPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        if (storedPassword.equals(plainPassword)) {
            return true;
        }

        // Symfony/PHP may store BCrypt with $2y$ prefix; jBCrypt expects $2a$/$2b$.
        if (storedPassword.startsWith("$2y$") || storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            String normalized = storedPassword.startsWith("$2y$")
                    ? "$2a$" + storedPassword.substring(4)
                    : storedPassword;
            try {
                return BCrypt.checkpw(plainPassword, normalized);
            } catch (Exception ignored) {
                return false;
            }
        }

        return false;
    }

    public int countAll() {
        return scalarInt("SELECT COUNT(*) FROM `user`");
    }

    public int countByType(String type) {
        if ("etudiant".equalsIgnoreCase(type)) {
            return scalarInt("SELECT COUNT(*) FROM etudiant");
        }
        if ("prof".equalsIgnoreCase(type)) {
            return scalarInt("SELECT COUNT(*) FROM prof");
        }
        return 0;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*," +
                " CASE" +
                "   WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'" +
                "   WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'" +
                "   ELSE 'admin'" +
                " END AS user_type" +
                " FROM `user` u ORDER BY u.id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur findAll: " + e.getMessage());
        }

        return users;
    }

    public void delete(int id) {
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM etudiant WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM prof WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println("Erreur delete user: " + e.getMessage());
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM `user` WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean registerEtudiant(Etudiant e) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertEtudiant = "INSERT INTO etudiant (id, niveau, status, suspended_until, filiere_id) VALUES (?, ?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);
            int userId;

            try (PreparedStatement ps = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, e.getNom());
                ps.setString(2, e.getPrenom());
                ps.setString(3, e.getEmail());
                ps.setString(4, e.getPassword());
                ps.setString(5, e.getCin());
                ps.setString(6, e.getTelephone());
                ps.setString(7, e.getAdresse());
                ps.setTimestamp(8, null);
                ps.setString(9, null);
                ps.setString(10, "[\"ROLE_ETUDIANT\"]");
                ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        return false;
                    }
                    userId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insertEtudiant)) {
                ps.setInt(1, userId);
                ps.setString(2, e.getNiveau());
                ps.setString(3, e.getStatus() == null ? "actif" : e.getStatus());
                ps.setTimestamp(4, null);
                ps.setInt(5, e.getFiliereId());
                ps.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (Exception ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            System.out.println("Erreur registerEtudiant: " + ex.getMessage());
            return false;
        }
    }

    public boolean registerProf(Prof p) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertProf = "INSERT INTO prof (id, specialite) VALUES (?, ?)";

        try {
            connection.setAutoCommit(false);
            int userId;

            try (PreparedStatement ps = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getNom());
                ps.setString(2, p.getPrenom());
                ps.setString(3, p.getEmail());
                ps.setString(4, p.getPassword());
                ps.setString(5, p.getCin());
                ps.setString(6, p.getTelephone());
                ps.setString(7, p.getAdresse());
                ps.setTimestamp(8, null);
                ps.setString(9, null);
                ps.setString(10, "[\"ROLE_ENSEIGNANT\"]");
                ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        return false;
                    }
                    userId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insertProf)) {
                ps.setInt(1, userId);
                ps.setString(2, p.getSpecialite());
                ps.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (Exception ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            System.out.println("Erreur registerProf: " + ex.getMessage());
            return false;
        }
    }

    public String generateResetToken(String email) {
        if (!emailExists(email)) {
            return null;
        }
        String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        resetTokensByEmail.put(email, token);
        return token;
    }

    public boolean resetPassword(String email, String token, String newPassword) {
        String expected = resetTokensByEmail.get(email);
        if (expected == null || !expected.equalsIgnoreCase(token)) {
            return false;
        }

        String sql = "UPDATE `user` SET password=? WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                resetTokensByEmail.remove(email);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Erreur resetPassword: " + e.getMessage());
        }
        return false;
    }

    private int scalarInt(String sql) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Erreur scalarInt: " + e.getMessage());
        }
        return 0;
    }

    private User mapUser(ResultSet rs) throws Exception {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setCin(rs.getString("CIN"));
        u.setTelephone(rs.getString("telephone"));
        u.setAdresse(rs.getString("adresse"));
        u.setDateNaissance(rs.getDate("date_naissance"));
        u.setPhoto(rs.getString("photo"));
        u.setRoles(rs.getString("roles"));
        Timestamp created = rs.getTimestamp("created_at");
        u.setCreatedAt(created == null ? new Date() : new Date(created.getTime()));

        String type = rs.getString("user_type");
        if (type == null || type.isBlank()) {
            type = "etudiant";
        }
        u.setType(type);
        return u;
    }
}
