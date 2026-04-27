package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.entity.Etudiant;
import tn.esprit.interfaces.IService;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD admin pour les étudiants.
 */
public class AdminEtudiantService implements IService<Etudiant> {

    private final Connection connection;

    public AdminEtudiantService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Etudiant etudiant) throws SQLException {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, type, roles, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertEtudiant = "INSERT INTO etudiant (id, niveau, status, suspended_until, filiere_id) VALUES (?, ?, ?, ?, ?)";

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (PreparedStatement userStmt = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement etudiantStmt = connection.prepareStatement(insertEtudiant)) {

            fillUserStatement(userStmt, etudiant);
            userStmt.executeUpdate();

            int userId;
            try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                if (!generatedKeys.next()) throw new SQLException("ID user non généré");
                userId = generatedKeys.getInt(1);
            }

            etudiantStmt.setInt(1, userId);
            etudiantStmt.setString(2, etudiant.getNiveau());
            etudiantStmt.setString(3, etudiant.getStatus() != null ? etudiant.getStatus() : "actif");
            etudiantStmt.setTimestamp(4, etudiant.getSuspendedUntil() != null
                    ? new Timestamp(etudiant.getSuspendedUntil().getTime()) : null);
            if (etudiant.getFiliereId() > 0) etudiantStmt.setInt(5, etudiant.getFiliereId());
            else etudiantStmt.setNull(5, Types.INTEGER);
            etudiantStmt.executeUpdate();

            connection.commit();
            etudiant.setId(userId);
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur ajout étudiant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void modifier(Etudiant etudiant) throws SQLException {
        String updateUser = "UPDATE `user` SET nom=?, prenom=?, email=?, password=?, CIN=?, telephone=?, adresse=?, date_naissance=?, photo=?, type=?, roles=? WHERE id=?";
        String updateEtudiant = "UPDATE etudiant SET niveau=?, status=?, suspended_until=?, filiere_id=? WHERE id=?";

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (PreparedStatement userStmt = connection.prepareStatement(updateUser);
             PreparedStatement etudiantStmt = connection.prepareStatement(updateEtudiant)) {

            fillUserStatementForUpdate(userStmt, etudiant);
            userStmt.setInt(12, etudiant.getId());
            userStmt.executeUpdate();

            etudiantStmt.setString(1, etudiant.getNiveau());
            etudiantStmt.setString(2, etudiant.getStatus());
            etudiantStmt.setTimestamp(3, etudiant.getSuspendedUntil() != null
                    ? new Timestamp(etudiant.getSuspendedUntil().getTime()) : null);
            if (etudiant.getFiliereId() > 0) etudiantStmt.setInt(4, etudiant.getFiliereId());
            else etudiantStmt.setNull(4, Types.INTEGER);
            etudiantStmt.setInt(5, etudiant.getId());
            etudiantStmt.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur modification étudiant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void supprimer(int id) throws SQLException {
        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement etudiantStmt = connection.prepareStatement("DELETE FROM etudiant WHERE id=?");
             PreparedStatement userStmt = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
            etudiantStmt.setInt(1, id); etudiantStmt.executeUpdate();
            userStmt.setInt(1, id); userStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur suppression étudiant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public ObservableList<Etudiant> afficher() throws SQLException {
        ObservableList<Etudiant> list = FXCollections.observableArrayList();
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, e.niveau, e.status, e.suspended_until, e.filiere_id FROM `user` u INNER JOIN etudiant e ON u.id = e.id ORDER BY u.id DESC";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapEtudiant(rs));
        }
        return list;
    }

    public ObservableList<Etudiant> search(String nom) throws SQLException {
        ObservableList<Etudiant> list = FXCollections.observableArrayList();
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, e.niveau, e.status, e.suspended_until, e.filiere_id FROM `user` u INNER JOIN etudiant e ON u.id = e.id WHERE u.nom LIKE ? ORDER BY u.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nom + "%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapEtudiant(rs)); }
        }
        return list;
    }

    @Override public void add(Etudiant e) { try { ajouter(e); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public void update(Etudiant e) { try { modifier(e); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public void delete(int id) { try { supprimer(id); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public List<Etudiant> getAll() { try { return new ArrayList<>(afficher()); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public Etudiant getById(int id) {
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, e.niveau, e.status, e.suspended_until, e.filiere_id FROM `user` u INNER JOIN etudiant e ON u.id = e.id WHERE u.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapEtudiant(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    private void fillUserStatement(PreparedStatement stmt, Etudiant etudiant) throws SQLException {
        stmt.setString(1, etudiant.getNom()); stmt.setString(2, etudiant.getPrenom());
        stmt.setString(3, etudiant.getEmail()); stmt.setString(4, etudiant.getPassword());
        stmt.setString(5, etudiant.getCIN()); stmt.setString(6, etudiant.getTelephone());
        stmt.setString(7, etudiant.getAdresse());
        stmt.setDate(8, etudiant.getDateNaissance() != null ? new Date(etudiant.getDateNaissance().getTime()) : null);
        stmt.setString(9, etudiant.getPhoto()); stmt.setString(10, "etudiant");
        String roles = (etudiant.getRoles() == null || etudiant.getRoles().isBlank()) ? "[\"ROLE_ETUDIANT\"]" : etudiant.getRoles();
        stmt.setString(11, roles);
        stmt.setTimestamp(12, etudiant.getCreatedAt() != null ? new Timestamp(etudiant.getCreatedAt().getTime()) : new Timestamp(System.currentTimeMillis()));
    }

    private void fillUserStatementForUpdate(PreparedStatement stmt, Etudiant etudiant) throws SQLException {
        stmt.setString(1, etudiant.getNom()); stmt.setString(2, etudiant.getPrenom());
        stmt.setString(3, etudiant.getEmail()); stmt.setString(4, etudiant.getPassword());
        stmt.setString(5, etudiant.getCIN()); stmt.setString(6, etudiant.getTelephone());
        stmt.setString(7, etudiant.getAdresse());
        stmt.setDate(8, etudiant.getDateNaissance() != null ? new Date(etudiant.getDateNaissance().getTime()) : null);
        stmt.setString(9, etudiant.getPhoto()); stmt.setString(10, "etudiant");
        String roles = (etudiant.getRoles() == null || etudiant.getRoles().isBlank()) ? "[\"ROLE_ETUDIANT\"]" : etudiant.getRoles();
        stmt.setString(11, roles);
    }

    private Etudiant mapEtudiant(ResultSet rs) throws SQLException {
        Etudiant e = new Etudiant();
        e.setId(rs.getInt("id")); e.setNom(rs.getString("nom")); e.setPrenom(rs.getString("prenom"));
        e.setEmail(rs.getString("email")); e.setPassword(rs.getString("password"));
        e.setCin(rs.getString("CIN")); e.setTelephone(rs.getString("telephone"));
        e.setAdresse(rs.getString("adresse")); e.setDateNaissance(rs.getDate("date_naissance"));
        e.setPhoto(rs.getString("photo")); e.setRoles(rs.getString("roles"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        e.setCreatedAt(createdAt != null ? new java.util.Date(createdAt.getTime()) : new java.util.Date());
        e.setNiveau(rs.getString("niveau")); e.setStatus(rs.getString("status"));
        Timestamp suspended = rs.getTimestamp("suspended_until");
        e.setSuspendedUntil(suspended != null ? new java.util.Date(suspended.getTime()) : null);
        e.setFiliereId(rs.getInt("filiere_id"));
        return e;
    }
}
