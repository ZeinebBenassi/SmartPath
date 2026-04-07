package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.entity.Prof;
import tn.esprit.interfaces.IService;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD admin pour les enseignants, dans un style proche de ExerciseService.
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
                if (!generatedKeys.next()) {
                    throw new SQLException("Creation enseignant echouee: id user non genere");
                }
                userId = generatedKeys.getInt(1);
            }

            profStmt.setInt(1, userId);
            profStmt.setString(2, prof.getSpecialite());
            profStmt.executeUpdate();

            connection.commit();
            prof.setId(userId);
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur ajout enseignant: " + e.getMessage(), e);
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
            throw new SQLException("Erreur modification enseignant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void supprimer(int id) throws SQLException {
        String deleteProf = "DELETE FROM prof WHERE id=?";
        String deleteUser = "DELETE FROM `user` WHERE id=?";

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (PreparedStatement profStmt = connection.prepareStatement(deleteProf);
             PreparedStatement userStmt = connection.prepareStatement(deleteUser)) {

            profStmt.setInt(1, id);
            profStmt.executeUpdate();

            userStmt.setInt(1, id);
            userStmt.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur suppression enseignant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public ObservableList<Prof> afficher() throws SQLException {
        ObservableList<Prof> profList = FXCollections.observableArrayList();
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, p.specialite FROM `user` u INNER JOIN prof p ON u.id = p.id ORDER BY u.id DESC";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                profList.add(mapProf(resultSet));
            }
        } catch (SQLException e) {
            throw new SQLException("Erreur affichage enseignants: " + e.getMessage(), e);
        }

        return profList;
    }

    public ObservableList<Prof> search(String nom) throws SQLException {
        ObservableList<Prof> profList = FXCollections.observableArrayList();
        String query = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, p.specialite FROM `user` u INNER JOIN prof p ON u.id = p.id WHERE u.nom LIKE ? ORDER BY u.id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, nom + "%");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    profList.add(mapProf(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Erreur recherche enseignants: " + e.getMessage(), e);
        }

        return profList;
    }

    @Override
    public void add(Prof prof) {
        try {
            ajouter(prof);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void update(Prof prof) {
        try {
            modifier(prof);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        try {
            supprimer(id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<Prof> getAll() {
        try {
            return new ArrayList<>(afficher());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Prof getById(int id) {
        String query = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, p.specialite FROM `user` u INNER JOIN prof p ON u.id = p.id WHERE u.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProf(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recuperation enseignant: " + e.getMessage(), e);
        }

        return null;
    }

    private void fillUserStatement(PreparedStatement stmt, Prof prof) throws SQLException {
        stmt.setString(1, prof.getNom());
        stmt.setString(2, prof.getPrenom());
        stmt.setString(3, prof.getEmail());
        stmt.setString(4, prof.getPassword());
        stmt.setString(5, prof.getCIN());
        stmt.setString(6, prof.getTelephone());
        stmt.setString(7, prof.getAdresse());

        if (prof.getDateNaissance() != null) {
            stmt.setDate(8, new Date(prof.getDateNaissance().getTime()));
        } else {
            stmt.setDate(8, null);
        }

        stmt.setString(9, prof.getPhoto());
        stmt.setString(10, "prof");

        String roles = (prof.getRoles() == null || prof.getRoles().isBlank())
                ? "[\"ROLE_ENSEIGNANT\"]"
                : prof.getRoles();
        stmt.setString(11, roles);

        Timestamp createdAt = (prof.getCreatedAt() != null)
                ? new Timestamp(prof.getCreatedAt().getTime())
                : new Timestamp(System.currentTimeMillis());
        stmt.setTimestamp(12, createdAt);
    }

    private void fillUserStatementForUpdate(PreparedStatement stmt, Prof prof) throws SQLException {
        stmt.setString(1, prof.getNom());
        stmt.setString(2, prof.getPrenom());
        stmt.setString(3, prof.getEmail());
        stmt.setString(4, prof.getPassword());
        stmt.setString(5, prof.getCIN());
        stmt.setString(6, prof.getTelephone());
        stmt.setString(7, prof.getAdresse());

        if (prof.getDateNaissance() != null) {
            stmt.setDate(8, new Date(prof.getDateNaissance().getTime()));
        } else {
            stmt.setDate(8, null);
        }

        stmt.setString(9, prof.getPhoto());
        stmt.setString(10, "prof");

        String roles = (prof.getRoles() == null || prof.getRoles().isBlank())
                ? "[\"ROLE_ENSEIGNANT\"]"
                : prof.getRoles();
        stmt.setString(11, roles);
    }

    private Prof mapProf(ResultSet rs) throws SQLException {
        return new Prof(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("CIN"),
                rs.getString("telephone"),
                rs.getString("adresse"),
                rs.getDate("date_naissance"),
                rs.getString("photo"),
                rs.getString("roles"),
                rs.getTimestamp("created_at"),
                rs.getString("specialite")
        );
    }
}
