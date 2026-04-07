package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.entity.Etudiant;
import tn.esprit.interfaces.IService;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD admin pour les etudiants, dans un style proche de ExerciseService.
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
                if (!generatedKeys.next()) {
                    throw new SQLException("Creation etudiant echouee: id user non genere");
                }
                userId = generatedKeys.getInt(1);
            }

            etudiantStmt.setInt(1, userId);
            etudiantStmt.setString(2, etudiant.getNiveau());
            etudiantStmt.setString(3, etudiant.getStatus());
            if (etudiant.getSuspendedUntil() != null) {
                etudiantStmt.setTimestamp(4, new Timestamp(etudiant.getSuspendedUntil().getTime()));
            } else {
                etudiantStmt.setTimestamp(4, null);
            }
            if (etudiant.getFiliereId() > 0) {
                etudiantStmt.setInt(5, etudiant.getFiliereId());
            } else {
                etudiantStmt.setNull(5, Types.INTEGER);
            }
            etudiantStmt.executeUpdate();

            connection.commit();
            etudiant.setId(userId);
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur ajout etudiant: " + e.getMessage(), e);
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
            if (etudiant.getSuspendedUntil() != null) {
                etudiantStmt.setTimestamp(3, new Timestamp(etudiant.getSuspendedUntil().getTime()));
            } else {
                etudiantStmt.setTimestamp(3, null);
            }
            if (etudiant.getFiliereId() > 0) {
                etudiantStmt.setInt(4, etudiant.getFiliereId());
            } else {
                etudiantStmt.setNull(4, Types.INTEGER);
            }
            etudiantStmt.setInt(5, etudiant.getId());
            etudiantStmt.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur modification etudiant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void supprimer(int id) throws SQLException {
        String deleteEtudiant = "DELETE FROM etudiant WHERE id=?";
        String deleteUser = "DELETE FROM `user` WHERE id=?";

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (PreparedStatement etudiantStmt = connection.prepareStatement(deleteEtudiant);
             PreparedStatement userStmt = connection.prepareStatement(deleteUser)) {

            etudiantStmt.setInt(1, id);
            etudiantStmt.executeUpdate();

            userStmt.setInt(1, id);
            userStmt.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Erreur suppression etudiant: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public ObservableList<Etudiant> afficher() throws SQLException {
        ObservableList<Etudiant> etudiantList = FXCollections.observableArrayList();
        String sql = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, e.niveau, e.status, e.suspended_until, e.filiere_id FROM `user` u INNER JOIN etudiant e ON u.id = e.id ORDER BY u.id DESC";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                etudiantList.add(mapEtudiant(resultSet));
            }
        } catch (SQLException e) {
            throw new SQLException("Erreur affichage etudiants: " + e.getMessage(), e);
        }

        return etudiantList;
    }

    public ObservableList<Etudiant> search(String nom) throws SQLException {
        ObservableList<Etudiant> etudiantList = FXCollections.observableArrayList();
        String query = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, e.niveau, e.status, e.suspended_until, e.filiere_id FROM `user` u INNER JOIN etudiant e ON u.id = e.id WHERE u.nom LIKE ? ORDER BY u.id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, nom + "%");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    etudiantList.add(mapEtudiant(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Erreur recherche etudiants: " + e.getMessage(), e);
        }

        return etudiantList;
    }

    @Override
    public void add(Etudiant etudiant) {
        try {
            ajouter(etudiant);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void update(Etudiant etudiant) {
        try {
            modifier(etudiant);
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
    public List<Etudiant> getAll() {
        try {
            return new ArrayList<>(afficher());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Etudiant getById(int id) {
        String query = "SELECT u.id, u.nom, u.prenom, u.email, u.password, u.CIN, u.telephone, u.adresse, u.date_naissance, u.photo, u.roles, u.created_at, e.niveau, e.status, e.suspended_until, e.filiere_id FROM `user` u INNER JOIN etudiant e ON u.id = e.id WHERE u.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapEtudiant(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recuperation etudiant: " + e.getMessage(), e);
        }

        return null;
    }

    private void fillUserStatement(PreparedStatement stmt, Etudiant etudiant) throws SQLException {
        stmt.setString(1, etudiant.getNom());
        stmt.setString(2, etudiant.getPrenom());
        stmt.setString(3, etudiant.getEmail());
        stmt.setString(4, etudiant.getPassword());
        stmt.setString(5, etudiant.getCIN());
        stmt.setString(6, etudiant.getTelephone());
        stmt.setString(7, etudiant.getAdresse());

        if (etudiant.getDateNaissance() != null) {
            stmt.setDate(8, new Date(etudiant.getDateNaissance().getTime()));
        } else {
            stmt.setDate(8, null);
        }

        stmt.setString(9, etudiant.getPhoto());
        stmt.setString(10, "etudiant");

        String roles = (etudiant.getRoles() == null || etudiant.getRoles().isBlank())
                ? "[\"ROLE_ETUDIANT\"]"
                : etudiant.getRoles();
        stmt.setString(11, roles);

        Timestamp createdAt = (etudiant.getCreatedAt() != null)
                ? new Timestamp(etudiant.getCreatedAt().getTime())
                : new Timestamp(System.currentTimeMillis());
        stmt.setTimestamp(12, createdAt);
    }

    private void fillUserStatementForUpdate(PreparedStatement stmt, Etudiant etudiant) throws SQLException {
        stmt.setString(1, etudiant.getNom());
        stmt.setString(2, etudiant.getPrenom());
        stmt.setString(3, etudiant.getEmail());
        stmt.setString(4, etudiant.getPassword());
        stmt.setString(5, etudiant.getCIN());
        stmt.setString(6, etudiant.getTelephone());
        stmt.setString(7, etudiant.getAdresse());

        if (etudiant.getDateNaissance() != null) {
            stmt.setDate(8, new Date(etudiant.getDateNaissance().getTime()));
        } else {
            stmt.setDate(8, null);
        }

        stmt.setString(9, etudiant.getPhoto());
        stmt.setString(10, "etudiant");

        String roles = (etudiant.getRoles() == null || etudiant.getRoles().isBlank())
                ? "[\"ROLE_ETUDIANT\"]"
                : etudiant.getRoles();
        stmt.setString(11, roles);
    }

    private Etudiant mapEtudiant(ResultSet rs) throws SQLException {
        Timestamp suspendedTs = rs.getTimestamp("suspended_until");
        return new Etudiant(
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
                rs.getString("niveau"),
                rs.getString("status"),
                suspendedTs == null ? null : new java.util.Date(suspendedTs.getTime()),
                rs.getInt("filiere_id")
        );
    }
}
