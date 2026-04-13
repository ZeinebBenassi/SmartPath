package services;

import tn.esprit.entity.Admin;
import tn.esprit.entity.Etudiant;
import tn.esprit.entity.Prof;
import tn.esprit.entity.User;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
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
                " END AS user_type," +
                " COALESCE(u.status, 'actif') AS status" +
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
                " END AS user_type," +
                " COALESCE(u.status, 'actif') AS status" +
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
        try {
            if (connection == null) {
                System.err.println("⚠️  Connexion NULL dans emailExists()");
                return false;
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean exists = rs.next();
                    System.out.println("✓ Vérification email '" + email + "': " + (exists ? "existe" : "disponible"));
                    return exists;
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Erreur emailExists: " + e.getMessage());
            return false;
        }
    }

    public boolean registerEtudiant(Etudiant e) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertEtudiant = "INSERT INTO etudiant (id, niveau, status, suspended_until, filiere_id) VALUES (?, ?, ?, ?, ?)";

        try {
            if (connection == null) {
                System.err.println("Erreur registerEtudiant: Connexion à la base de données est NULL");
                return false;
            }
            
            System.out.println("Début inscription étudiant: " + e.getEmail());
            connection.setAutoCommit(false);
            int userId;

            // Étape 1: Insert dans table user
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
                ps.setString(11, "etudiant");  // ← type
                ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                int rows = ps.executeUpdate();
                System.out.println("  ✓ Utilisateur inséré (" + rows + " lignes)");

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        System.err.println("✗ Erreur: ID généré non récupéré");
                        return false;
                    }
                    userId = keys.getInt(1);
                    System.out.println("  ✓ ID généré: " + userId);
                }
            }

            // Étape 2: Insert dans table etudiant
            try (PreparedStatement ps = connection.prepareStatement(insertEtudiant)) {
                ps.setInt(1, userId);
                ps.setString(2, e.getNiveau() != null ? e.getNiveau() : "L1");
                ps.setString(3, e.getStatus() != null ? e.getStatus() : "actif");
                ps.setTimestamp(4, null);
                // filiere_id: NULL si pas défini (évite contrainte FK)
                if (e.getFiliereId() > 0) {
                    ps.setInt(5, e.getFiliereId());
                    System.out.println("  ℹ️  filiere_id = " + e.getFiliereId());
                } else {
                    ps.setNull(5, Types.INTEGER);
                    System.out.println("  ℹ️  filiere_id = NULL (non spécifiée)");
                }
                int rows = ps.executeUpdate();
                System.out.println("  ✓ Étudiant inséré (" + rows + " lignes)");
            }

            connection.commit();
            connection.setAutoCommit(true);
            System.out.println("✅ Inscription réussie pour: " + e.getEmail());
            return true;
        } catch (Exception ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            System.err.println("Erreur registerEtudiant: " + ex.getMessage());
            System.err.println("  Détail: " + ex.getClass().getSimpleName());
            ex.printStackTrace();
            return false;
        }
    }

    public boolean registerProf(Prof p) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertProf = "INSERT INTO prof (id, specialite) VALUES (?, ?)";

        try {
            if (connection == null) {
                System.err.println("Erreur registerProf: Connexion à la base de données est NULL");
                return false;
            }
            
            System.out.println("Début inscription prof: " + p.getEmail());
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
                ps.setString(11, "prof");  // ← type
                ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                int rows = ps.executeUpdate();
                System.out.println("  ✓ Utilisateur inséré (" + rows + " lignes)");

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        System.err.println("✗ Erreur: ID généré non récupéré");
                        return false;
                    }
                    userId = keys.getInt(1);
                    System.out.println("  ✓ ID généré: " + userId);
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insertProf)) {
                ps.setInt(1, userId);
                ps.setString(2, p.getSpecialite() != null ? p.getSpecialite() : "");
                int rows = ps.executeUpdate();
                System.out.println("  ✓ Professeur inséré (" + rows + " lignes)");
            }

            connection.commit();
            connection.setAutoCommit(true);
            System.out.println("✅ Inscription réussie pour: " + p.getEmail());
            return true;
        } catch (Exception ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            System.err.println("Erreur registerProf: " + ex.getMessage());
            System.err.println("  Détail: " + ex.getClass().getSimpleName());
            ex.printStackTrace();
            return false;
        }
    }

    public boolean registerAdmin(Admin a) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, type, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            if (connection == null) {
                System.err.println("Erreur registerAdmin: Connexion à la base de données est NULL");
                return false;
            }
            
            System.out.println("Début inscription admin: " + a.getEmail());
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, a.getNom());
                ps.setString(2, a.getPrenom());
                ps.setString(3, a.getEmail());
                ps.setString(4, a.getPassword());
                ps.setString(5, a.getCin());
                ps.setString(6, a.getTelephone());
                ps.setString(7, a.getAdresse());
                ps.setTimestamp(8, null);
                ps.setString(9, null);
                ps.setString(10, "[\"ROLE_ADMIN\"]");
                ps.setString(11, "admin");  // ← type
                ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                ps.setString(13, a.getStatus() != null ? a.getStatus() : "actif");
                int rows = ps.executeUpdate();
                System.out.println("  ✓ Admin inséré (" + rows + " lignes)");

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        System.err.println("✗ Erreur: ID généré non récupéré");
                        return false;
                    }
                    int userId = keys.getInt(1);
                    System.out.println("  ✓ ID généré: " + userId);
                }
            }

            connection.commit();
            connection.setAutoCommit(true);
            System.out.println("✅ Inscription réussie pour: " + a.getEmail());
            return true;
        } catch (Exception ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            System.err.println("Erreur registerAdmin: " + ex.getMessage());
            System.err.println("  Détail: " + ex.getClass().getSimpleName());
            ex.printStackTrace();
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

    /**
     * Récupère un utilisateur par son ID.
     */
    public User findById(int id) {
        String sql = "SELECT u.*," +
                " CASE" +
                "   WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'" +
                "   WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'" +
                "   ELSE 'admin'" +
                " END AS user_type," +
                " COALESCE(u.status, 'actif') AS status" +
                " FROM `user` u WHERE u.id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur findById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Crée un nouvel utilisateur simple (admin).
     */
    public int create(User user) {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, roles, type, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertEtudiant = "INSERT INTO etudiant (id, niveau, status, suspended_until, filiere_id) VALUES (?, ?, ?, ?, ?)";
        String insertProf = "INSERT INTO prof (id, specialite) VALUES (?, ?)";

        try {
            if (connection == null) {
                throw new IllegalStateException("Connexion à la base de données est NULL");
            }

            String type = (user.getType() != null && !user.getType().trim().isEmpty())
                    ? user.getType().trim().toLowerCase()
                    : "admin";

            String roles;
            if (user.getRoles() != null && !user.getRoles().trim().isEmpty()) {
                roles = user.getRoles();
            } else {
                switch (type) {
                    case "etudiant":
                        roles = "[\"ROLE_ETUDIANT\"]";
                        break;
                    case "prof":
                        roles = "[\"ROLE_PROF\"]";
                        break;
                    default:
                        roles = "[\"ROLE_ADMIN\"]";
                }
            }

            String status = (user.getStatus() != null && !user.getStatus().trim().isEmpty()) ? user.getStatus().trim() : "actif";

            connection.setAutoCommit(false);
            int userId;

            try (PreparedStatement ps = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getNom());
                ps.setString(2, user.getPrenom());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getCin());
                ps.setString(6, user.getTelephone());
                ps.setString(7, user.getAdresse());
                ps.setDate(8, user.getDateNaissance() != null ? new java.sql.Date(user.getDateNaissance().getTime()) : null);
                ps.setString(9, user.getPhoto());
                ps.setString(10, roles);
                ps.setString(11, type);
                ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                ps.setString(13, status);

                int affectedRows = ps.executeUpdate();
                if (affectedRows <= 0) {
                    throw new SQLException("Aucune ligne insérée dans la table user");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Impossible de récupérer l'ID généré");
                    }
                    userId = keys.getInt(1);
                }
            }

            if ("etudiant".equals(type)) {
                try (PreparedStatement ps = connection.prepareStatement(insertEtudiant)) {
                    ps.setInt(1, userId);
                    ps.setString(2, "L1");
                    ps.setString(3, status);
                    ps.setTimestamp(4, null);
                    ps.setNull(5, Types.INTEGER);
                    ps.executeUpdate();
                }
            } else if ("prof".equals(type)) {
                try (PreparedStatement ps = connection.prepareStatement(insertProf)) {
                    ps.setInt(1, userId);
                    ps.setString(2, "");
                    ps.executeUpdate();
                }
            }

            connection.commit();
            connection.setAutoCommit(true);
            return userId;
        } catch (Exception e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Erreur create: " + e.getMessage(), e);
        }
    }

    /**
     * Met à jour un utilisateur existant.
     */
    public boolean update(User user) {
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, password=?, CIN=?, telephone=?, adresse=?, date_naissance=?, photo=?, roles=?, type=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getCin());
            ps.setString(6, user.getTelephone());
            ps.setString(7, user.getAdresse());
            ps.setDate(8, user.getDateNaissance() != null ? new java.sql.Date(user.getDateNaissance().getTime()) : null);
            ps.setString(9, user.getPhoto());
            ps.setString(10, user.getRoles());
            ps.setString(11, user.getType());
            ps.setInt(12, user.getId());
            
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (Exception e) {
            System.out.println("Erreur update: " + e.getMessage());
        }
        return false;
    }

    /**
     * Recherche les utilisateurs par nom, prénom ou email.
     */
    public List<User> search(String query) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*," +
                " CASE" +
                "   WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'" +
                "   WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'" +
                "   ELSE 'admin'" +
                " END AS user_type," +
                " COALESCE(u.status, 'actif') AS status" +
                " FROM `user` u WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ? ORDER BY u.id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur search: " + e.getMessage());
        }
        return users;
    }

    /**
     * Filtre les utilisateurs par type.
     */
    public List<User> findByType(String type) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*," +
                " CASE" +
                "   WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'" +
                "   WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'" +
                "   ELSE 'admin'" +
                " END AS user_type," +
                " COALESCE(u.status, 'actif') AS status" +
                " FROM `user` u WHERE " +
                " CASE" +
                "   WHEN 'etudiant' = ? THEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id)" +
                "   WHEN 'prof' = ? THEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id)" +
                "   WHEN 'admin' = ? THEN NOT EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) AND NOT EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id)" +
                "   ELSE TRUE" +
                " END" +
                " ORDER BY u.id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, type);
            ps.setString(3, type);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur findByType: " + e.getMessage());
        }
        return users;
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

    /**
     * Met à jour le statut d'un utilisateur (actif ou ban)
     */
    public boolean updateStatus(int userId, String status) {
        // D'abord, essayer d'ajouter la colonne si elle n'existe pas
        try {
            String alterTableSql = "ALTER TABLE `user` ADD COLUMN `status` VARCHAR(20) DEFAULT 'actif'";
            try (PreparedStatement altPs = connection.prepareStatement(alterTableSql)) {
                altPs.executeUpdate();
                System.out.println("✓ Colonne 'status' créée");
            }
        } catch (Exception ignored) {
            // Colonne existe déjà, pas d'erreur
        }

        String sql = "UPDATE `user` SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            System.out.println("Erreur updateStatus: " + e.getMessage());
            return false;
        }
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
        // Récupérer le statut avec gestion du cas NULL
        try {
            String status = rs.getString("status");
            u.setStatus(status != null ? status : "actif");
        } catch (Exception e) {
            u.setStatus("actif"); // Valeur par défaut si la colonne n'existe pas
        }
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
