package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.entity.Prof;
import tn.esprit.interfaces.IService;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD admin pour les professeurs.
 */
public class AdminProfService implements IService<Prof> {

    private final Connection connection;

    public AdminProfService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Prof prof) throws SQLException {
        String insertUser = "INSERT INTO `user` (nom, prenom, email, password, CIN, telephone, adresse, date_naissance, photo, type, roles, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertProf = "INSERT INTO prof (id, specialite) VALUES (?, ?)";

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (PreparedStatement userStmt = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement profStmt = connection.prepareStatement(insertProf)) {

            fillUserStatement(userStmt, prof);
            userStmt.executeUpdate();

            int userId;
            try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                if (!generatedKeys.next()) throw new SQLException("ID user non généré");
                userId = generatedKeys.getInt(1);
            }

            profStmt.setInt(1, userId);
            profStmt.setString(2, prof.getSpecialite() != null ? prof.getSpecialite() : "");
            profStmt.executeUpdate();

            connection.commit();
            prof.setId(userId);
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur ajout professeur: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void modifier(Prof prof) throws SQLException {
        String updateUser = "UPDATE `user` SET nom=?, prenom=?, email=?, password=?, CIN=?, telephone=?, adresse=?, date_naissance=?, photo=?, type=?, roles=? WHERE id=?";
        String updateProf = "UPDATE prof SET specialite=? WHERE id=?";

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (PreparedStatement userStmt = connection.prepareStatement(updateUser);
             PreparedStatement profStmt = connection.prepareStatement(updateProf)) {

            fillUserStatementForUpdate(userStmt, prof);
            userStmt.setInt(12, prof.getId());
            userStmt.executeUpdate();

            profStmt.setString(1, prof.getSpecialite());
            profStmt.setInt(2, prof.getId());
            profStmt.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur modification professeur: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void supprimer(int id) throws SQLException {
        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement profStmt = connection.prepareStatement("DELETE FROM prof WHERE id=?");
             PreparedStatement userStmt = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
            profStmt.setInt(1, id); profStmt.executeUpdate();
            userStmt.setInt(1, id); userStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur suppression professeur: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public ObservableList<Prof> afficher() throws SQLException {
        ObservableList<Prof> list = FXCollections.observableArrayList();
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, p.specialite FROM `user` u INNER JOIN prof p ON u.id = p.id ORDER BY u.id DESC";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapProf(rs));
        }
        return list;
    }

    @Override public void add(Prof p) { try { ajouter(p); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public void update(Prof p) { try { modifier(p); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public void delete(int id) { try { supprimer(id); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public List<Prof> getAll() { try { return new ArrayList<>(afficher()); } catch (SQLException ex) { throw new RuntimeException(ex); } }
    @Override public Prof getById(int id) {
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, p.specialite FROM `user` u INNER JOIN prof p ON u.id = p.id WHERE u.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapProf(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    private void fillUserStatement(PreparedStatement stmt, Prof prof) throws SQLException {
        stmt.setString(1, prof.getNom()); stmt.setString(2, prof.getPrenom());
        stmt.setString(3, prof.getEmail()); stmt.setString(4, prof.getPassword());
        stmt.setString(5, prof.getCIN()); stmt.setString(6, prof.getTelephone());
        stmt.setString(7, prof.getAdresse());
        stmt.setDate(8, prof.getDateNaissance() != null ? new Date(prof.getDateNaissance().getTime()) : null);
        stmt.setString(9, prof.getPhoto()); stmt.setString(10, "prof");
        String roles = (prof.getRoles() == null || prof.getRoles().isBlank()) ? "[\"ROLE_ENSEIGNANT\"]" : prof.getRoles();
        stmt.setString(11, roles);
        stmt.setTimestamp(12, prof.getCreatedAt() != null ? new Timestamp(prof.getCreatedAt().getTime()) : new Timestamp(System.currentTimeMillis()));
    }

    private void fillUserStatementForUpdate(PreparedStatement stmt, Prof prof) throws SQLException {
        stmt.setString(1, prof.getNom()); stmt.setString(2, prof.getPrenom());
        stmt.setString(3, prof.getEmail()); stmt.setString(4, prof.getPassword());
        stmt.setString(5, prof.getCIN()); stmt.setString(6, prof.getTelephone());
        stmt.setString(7, prof.getAdresse());
        stmt.setDate(8, prof.getDateNaissance() != null ? new Date(prof.getDateNaissance().getTime()) : null);
        stmt.setString(9, prof.getPhoto()); stmt.setString(10, "prof");
        String roles = (prof.getRoles() == null || prof.getRoles().isBlank()) ? "[\"ROLE_ENSEIGNANT\"]" : prof.getRoles();
        stmt.setString(11, roles);
    }

    private Prof mapProf(ResultSet rs) throws SQLException {
        Prof p = new Prof();
        p.setId(rs.getInt("id")); p.setNom(rs.getString("nom")); p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email")); p.setPassword(rs.getString("password"));
        p.setCin(rs.getString("CIN")); p.setTelephone(rs.getString("telephone"));
        p.setAdresse(rs.getString("adresse")); p.setDateNaissance(rs.getDate("date_naissance"));
        p.setPhoto(rs.getString("photo")); p.setRoles(rs.getString("roles"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        p.setCreatedAt(createdAt != null ? new java.util.Date(createdAt.getTime()) : new java.util.Date());
        p.setSpecialite(rs.getString("specialite"));
        return p;
    }
}
