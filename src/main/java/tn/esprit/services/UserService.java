package tn.esprit.services;

import tn.esprit.entity.Etudiant;
import tn.esprit.entity.Prof;
import tn.esprit.entity.User;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static Boolean statusColumnExists = null;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
        ensureStatusColumn();
    }

    private void ensureStatusColumn() {
        if (statusColumnExists != null) return;
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT status FROM `user` LIMIT 1")) {
                ps.executeQuery();
                statusColumnExists = true;
            }
        } catch (Exception e) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "ALTER TABLE `user` ADD COLUMN `status` VARCHAR(20) DEFAULT 'actif'")) {
                ps.executeUpdate();
                statusColumnExists = true;
                System.out.println("Colonne 'status' ajoutee dans la table user");
            } catch (Exception ex) {
                statusColumnExists = false;
                System.out.println("Impossible d'ajouter status : " + ex.getMessage());
            }
        }
    }

    // ── AUTHENTIFICATION ─────────────────────────────────────────────────────

    public User login(String email, String password) {
        String statusExpr = Boolean.TRUE.equals(statusColumnExists)
                ? " COALESCE(u.status, 'actif') AS status" : " 'actif' AS status";
        String sql = "SELECT u.*," + statusExpr + ","
                + " CASE WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'"
                + "      WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'"
                + "      ELSE 'admin' END AS user_type"
                + " FROM `user` u WHERE u.email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    if (isPasswordMatch(password, stored)) return mapUser(rs);
                }
            }
        } catch (Exception e) { System.out.println("Erreur login: " + e.getMessage()); }
        return null;
    }

    private boolean isPasswordMatch(String plain, String stored) {
        if (stored == null) return false;
        if (stored.equals(plain)) return true;
        if (stored.startsWith("$2y$") || stored.startsWith("$2a$") || stored.startsWith("$2b$")) {
            String norm = stored.startsWith("$2y$") ? "$2a$" + stored.substring(4) : stored;
            try { return BCrypt.checkpw(plain, norm); } catch (Exception ignored) { return false; }
        }
        return false;
    }

    // ── STATISTIQUES ─────────────────────────────────────────────────────────

    public int countAll() { return scalarInt("SELECT COUNT(*) FROM `user`"); }

    public int countByType(String type) {
        if ("etudiant".equalsIgnoreCase(type)) return scalarInt("SELECT COUNT(*) FROM etudiant");
        if ("prof".equalsIgnoreCase(type))     return scalarInt("SELECT COUNT(*) FROM prof");
        return 0;
    }

    // ── LECTURE ──────────────────────────────────────────────────────────────

    private String statusExpr() {
        return Boolean.TRUE.equals(statusColumnExists)
                ? " COALESCE(u.status, 'actif') AS status" : " 'actif' AS status";
    }

    private String baseSelect() {
        return "SELECT u.*," + statusExpr() + ","
                + " CASE WHEN EXISTS (SELECT 1 FROM etudiant e WHERE e.id = u.id) THEN 'etudiant'"
                + "      WHEN EXISTS (SELECT 1 FROM prof p WHERE p.id = u.id) THEN 'prof'"
                + "      ELSE 'admin' END AS user_type"
                + " FROM `user` u";
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(baseSelect() + " ORDER BY u.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (Exception e) { System.out.println("Erreur findAll: " + e.getMessage()); }
        return list;
    }

    public User findById(int id) {
        try (PreparedStatement ps = connection.prepareStatement(baseSelect() + " WHERE u.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapUser(rs); }
        } catch (Exception e) { System.out.println("Erreur findById: " + e.getMessage()); }
        return null;
    }

    public List<User> search(String query) {
        List<User> list = new ArrayList<>();
        String sql = baseSelect() + " WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ? ORDER BY u.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String p = "%" + query + "%";
            ps.setString(1, p); ps.setString(2, p); ps.setString(3, p);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapUser(rs)); }
        } catch (Exception e) { System.out.println("Erreur search: " + e.getMessage()); }
        return list;
    }

    public List<User> findByType(String type) {
        List<User> list = new ArrayList<>();
        String sql = baseSelect()
                + " WHERE CASE WHEN 'etudiant'=? THEN EXISTS(SELECT 1 FROM etudiant e WHERE e.id=u.id)"
                + "            WHEN 'prof'=?     THEN EXISTS(SELECT 1 FROM prof p WHERE p.id=u.id)"
                + "            WHEN 'admin'=?    THEN NOT EXISTS(SELECT 1 FROM etudiant e WHERE e.id=u.id) AND NOT EXISTS(SELECT 1 FROM prof p WHERE p.id=u.id)"
                + "            ELSE TRUE END ORDER BY u.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type); ps.setString(2, type); ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapUser(rs)); }
        } catch (Exception e) { System.out.println("Erreur findByType: " + e.getMessage()); }
        return list;
    }

    // ── ECRITURE ─────────────────────────────────────────────────────────────

    public boolean emailExists(String email) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM `user` WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (Exception e) { System.err.println("Erreur emailExists: " + e.getMessage()); }
        return false;
    }

    public int create(User user) {
        String ins = Boolean.TRUE.equals(statusColumnExists)
                ? "INSERT INTO `user` (nom,prenom,email,password,CIN,telephone,adresse,date_naissance,photo,roles,type,created_at,status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
                : "INSERT INTO `user` (nom,prenom,email,password,CIN,telephone,adresse,date_naissance,photo,roles,type,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            String type = user.getType() != null && !user.getType().isBlank() ? user.getType().trim().toLowerCase() : "admin";
            String roles = type.equals("etudiant") ? "[\"ROLE_ETUDIANT\"]" : type.equals("prof") ? "[\"ROLE_PROF\"]" : "[\"ROLE_ADMIN\"]";
            String status = user.getStatus() != null && !user.getStatus().isBlank() ? user.getStatus().trim() : "actif";
            connection.setAutoCommit(false);
            int userId;
            try (PreparedStatement ps = connection.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getNom());       ps.setString(2, user.getPrenom());
                ps.setString(3, user.getEmail());     ps.setString(4, user.getPassword());
                ps.setString(5, user.getCin());       ps.setString(6, user.getTelephone());
                ps.setString(7, user.getAdresse());
                ps.setDate(8, user.getDateNaissance() != null ? new java.sql.Date(user.getDateNaissance().getTime()) : null);
                ps.setString(9, user.getPhoto());     ps.setString(10, roles);
                ps.setString(11, type);               ps.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                if (Boolean.TRUE.equals(statusColumnExists)) ps.setString(13, status);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new Exception("ID non genere"); userId = keys.getInt(1); }
            }
            if ("etudiant".equals(type)) {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO etudiant(id,niveau,status,suspended_until,filiere_id) VALUES(?,?,?,?,?)")) {
                    ps.setInt(1, userId); ps.setString(2, "L1"); ps.setString(3, status); ps.setTimestamp(4, null); ps.setNull(5, Types.INTEGER); ps.executeUpdate();
                }
            } else if ("prof".equals(type)) {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO prof(id,specialite) VALUES(?,?)")) {
                    ps.setInt(1, userId); ps.setString(2, ""); ps.executeUpdate();
                }
            }
            connection.commit(); connection.setAutoCommit(true);
            return userId;
        } catch (Exception e) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (Exception ignored) {}
            throw new RuntimeException("Erreur create: " + e.getMessage(), e);
        }
    }

    public boolean update(User user) {
        String sql = "UPDATE `user` SET nom=?,prenom=?,email=?,CIN=?,telephone=?,adresse=?,date_naissance=?,photo=?,roles=?,type=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getNom());    ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());  ps.setString(4, user.getCin());
            ps.setString(5, user.getTelephone()); ps.setString(6, user.getAdresse());
            ps.setDate(7, user.getDateNaissance() != null ? new java.sql.Date(user.getDateNaissance().getTime()) : null);
            ps.setString(8, user.getPhoto());  ps.setString(9, user.getRoles());
            ps.setString(10, user.getType()); ps.setInt(11, user.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                try (PreparedStatement p2 = connection.prepareStatement("UPDATE `user` SET password=? WHERE id=?")) {
                    p2.setString(1, user.getPassword()); p2.setInt(2, user.getId()); p2.executeUpdate();
                }
            }
            return ok;
        } catch (Exception e) { System.out.println("Erreur update: " + e.getMessage()); }
        return false;
    }

    public boolean updateStatus(int userId, String status) {
        ensureStatusColumn();
        if (!Boolean.TRUE.equals(statusColumnExists)) return false;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE `user` SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, userId); return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("Erreur updateStatus: " + e.getMessage()); }
        return false;
    }

    public void delete(int id) {
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM etudiant WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM prof WHERE id=?"))     { ps.setInt(1, id); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `user` WHERE id=?"))   { ps.setInt(1, id); ps.executeUpdate(); }
        } catch (Exception e) { System.out.println("Erreur delete: " + e.getMessage()); }
    }

    // ── INSCRIPTION ──────────────────────────────────────────────────────────

    public boolean registerEtudiant(Etudiant e) {
        String ins = "INSERT INTO `user`(nom,prenom,email,password,CIN,telephone,adresse,date_naissance,photo,roles,type,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            connection.setAutoCommit(false); int userId;
            try (PreparedStatement ps = connection.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1,e.getNom()); ps.setString(2,e.getPrenom()); ps.setString(3,e.getEmail()); ps.setString(4,e.getPassword());
                ps.setString(5,e.getCin()); ps.setString(6,e.getTelephone()); ps.setString(7,e.getAdresse());
                ps.setTimestamp(8, e.getDateNaissance()!=null ? new Timestamp(e.getDateNaissance().getTime()) : null);
                ps.setString(9,null); ps.setString(10,"[\"ROLE_ETUDIANT\"]"); ps.setString(11,"etudiant"); ps.setTimestamp(12,new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
                try (ResultSet k=ps.getGeneratedKeys()) { if(!k.next()){connection.rollback();return false;} userId=k.getInt(1); }
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO etudiant(id,niveau,status,suspended_until,filiere_id) VALUES(?,?,?,?,?)")) {
                ps.setInt(1,userId); ps.setString(2,e.getNiveau()!=null?e.getNiveau():"L1"); ps.setString(3,e.getStatus()!=null?e.getStatus():"actif"); ps.setTimestamp(4,null);
                if(e.getFiliereId()>0) ps.setInt(5,e.getFiliereId()); else ps.setNull(5,Types.INTEGER); ps.executeUpdate();
            }
            connection.commit(); connection.setAutoCommit(true); return true;
        } catch (Exception ex) { try{connection.rollback();connection.setAutoCommit(true);}catch(Exception ignored){} System.out.println("Erreur registerEtudiant: "+ex.getMessage()); return false; }
    }

    public boolean registerProf(Prof p) {
        String ins = "INSERT INTO `user`(nom,prenom,email,password,CIN,telephone,adresse,date_naissance,photo,roles,type,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            connection.setAutoCommit(false); int userId;
            try (PreparedStatement ps = connection.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1,p.getNom()); ps.setString(2,p.getPrenom()); ps.setString(3,p.getEmail()); ps.setString(4,p.getPassword());
                ps.setString(5,p.getCin()); ps.setString(6,p.getTelephone()); ps.setString(7,p.getAdresse()); ps.setTimestamp(8,null);
                ps.setString(9,null); ps.setString(10,"[\"ROLE_ENSEIGNANT\"]"); ps.setString(11,"prof"); ps.setTimestamp(12,new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
                try (ResultSet k=ps.getGeneratedKeys()) { if(!k.next()){connection.rollback();return false;} userId=k.getInt(1); }
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO prof(id,specialite) VALUES(?,?)")) {
                ps.setInt(1,userId); ps.setString(2,p.getSpecialite()!=null?p.getSpecialite():""); ps.executeUpdate();
            }
            connection.commit(); connection.setAutoCommit(true); return true;
        } catch (Exception ex) { try{connection.rollback();connection.setAutoCommit(true);}catch(Exception ignored){} System.out.println("Erreur registerProf: "+ex.getMessage()); return false; }
    }

    // ── MOT DE PASSE OUBLIE ──────────────────────────────────────────────────

    public String generateResetToken(String email) {
        if (!emailExists(email)) return null;
        String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        resetTokensByEmail.put(email, token);
        return token;
    }

    public boolean resetPassword(String email, String token, String newPassword) {
        String expected = resetTokensByEmail.get(email);
        if (expected == null || !expected.equalsIgnoreCase(token)) return false;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE `user` SET password=? WHERE email=?")) {
            ps.setString(1, newPassword); ps.setString(2, email);
            if (ps.executeUpdate() > 0) { resetTokensByEmail.remove(email); return true; }
        } catch (Exception e) { System.out.println("Erreur resetPassword: " + e.getMessage()); }
        return false;
    }

    // ── UTILITAIRES ──────────────────────────────────────────────────────────

    private int scalarInt(String sql) {
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { System.out.println("Erreur scalarInt: " + e.getMessage()); }
        return 0;
    }

    private User mapUser(ResultSet rs) throws Exception {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));           u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));        u.setPassword(rs.getString("password"));
        u.setCin(rs.getString("CIN"));            u.setTelephone(rs.getString("telephone"));
        u.setAdresse(rs.getString("adresse"));    u.setDateNaissance(rs.getDate("date_naissance"));
        u.setPhoto(rs.getString("photo"));        u.setRoles(rs.getString("roles"));
        try { String s = rs.getString("status"); u.setStatus(s != null ? s : "actif"); } catch (Exception e) { u.setStatus("actif"); }
        Timestamp created = rs.getTimestamp("created_at");
        u.setCreatedAt(created == null ? new Date() : new Date(created.getTime()));
        String type = rs.getString("user_type");
        u.setType(type == null || type.isBlank() ? "etudiant" : type);
        return u;
    }
}
