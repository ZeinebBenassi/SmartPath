package tn.esprit.services;

import tn.esprit.entity.Filiere;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FiliereService implements ICrud<Filiere> {

    private final Connection cnx = MyDatabase.getInstance().getConnection();
    private static Boolean imageColumnExists = null;

    public FiliereService() {
        ensureImageColumn();
    }

    private void ensureImageColumn() {
        if (imageColumnExists != null) return;
        try (ResultSet rs = cnx.getMetaData().getColumns(null, null, "filiere", "image")) {
            if (rs.next()) {
                imageColumnExists = true;
                return;
            }
        } catch (Exception ignored) {}

        try (Statement st = cnx.createStatement()) {
            st.executeUpdate("ALTER TABLE filiere ADD COLUMN image VARCHAR(500) NULL");
            imageColumnExists = true;
        } catch (SQLException e) {
            imageColumnExists = false;
            System.out.println("[FiliereService] image column unavailable: " + e.getMessage());
        }
    }

    @Override
    public void ajouter(Filiere f) throws SQLException {
        ensureImageColumn();
        String sql = Boolean.TRUE.equals(imageColumnExists)
                ? "INSERT INTO filiere (nom, categorie, niveau, description, debouches, competences, icon, image, traits) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'null')"
                : "INSERT INTO filiere (nom, categorie, niveau, description, debouches, competences, icon, traits) VALUES (?, ?, ?, ?, ?, ?, ?, 'null')";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNom());
            ps.setString(2, f.getCategorie());
            ps.setString(3, f.getNiveau());
            ps.setString(4, f.getDescription());
            ps.setString(5, f.getDebouches());
            ps.setString(6, f.getCompetences());
            ps.setString(7, f.getIcon());
            if (Boolean.TRUE.equals(imageColumnExists)) {
                ps.setString(8, f.getImage());
            }
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) f.setId(rs.getInt(1));
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM filiere WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Filiere f) throws SQLException {
        ensureImageColumn();
        String sql = Boolean.TRUE.equals(imageColumnExists)
                ? "UPDATE filiere SET nom=?, categorie=?, niveau=?, description=?, debouches=?, competences=?, icon=?, image=? WHERE id=?"
                : "UPDATE filiere SET nom=?, categorie=?, niveau=?, description=?, debouches=?, competences=?, icon=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, f.getNom());
            ps.setString(2, f.getCategorie());
            ps.setString(3, f.getNiveau());
            ps.setString(4, f.getDescription());
            ps.setString(5, f.getDebouches());
            ps.setString(6, f.getCompetences());
            ps.setString(7, f.getIcon());
            if (Boolean.TRUE.equals(imageColumnExists)) {
                ps.setString(8, f.getImage());
                ps.setInt(9, f.getId());
            } else {
                ps.setInt(8, f.getId());
            }
            ps.executeUpdate();
        }
    }

    @Override
    public List<Filiere> afficher() throws SQLException {
        List<Filiere> list = new ArrayList<>();
        String sql = "SELECT * FROM filiere ORDER BY nom ASC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Filiere getById(int id) throws SQLException {
        String sql = "SELECT * FROM filiere WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    private Filiere mapRow(ResultSet rs) throws SQLException {
        Filiere f = new Filiere(
                rs.getInt("id"), rs.getString("nom"), rs.getString("categorie"),
                rs.getString("niveau"), rs.getString("description"),
                rs.getString("debouches"), rs.getString("competences"), rs.getString("icon")
        );
        try {
            f.setImage(rs.getString("image"));
        } catch (SQLException ignored) {
            f.setImage(null);
        }
        return f;
    }
}
