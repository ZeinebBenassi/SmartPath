package tn.esprit.services;

import tn.esprit.entity.Filiere;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FiliereService implements ICrud<Filiere> {

    private final Connection cnx = MyDatabase.getInstance().getConnection();
    private static Boolean imageColumnExists  = null;
    private static Boolean traitsColumnExists = null;

    public FiliereService() {
        ensureImageColumn();
        ensureTraitsColumn();
    }

    private void ensureImageColumn() {
        if (imageColumnExists != null) return;
        try (ResultSet rs = cnx.getMetaData().getColumns(null, null, "filiere", "image")) {
            if (rs.next()) { imageColumnExists = true; return; }
        } catch (Exception ignored) {}
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate("ALTER TABLE filiere ADD COLUMN IF NOT EXISTS image VARCHAR(500) NULL");
            imageColumnExists = true;
        } catch (SQLException e) {
            imageColumnExists = false;
            System.out.println("[FiliereService] image column unavailable: " + e.getMessage());
        }
    }

    /** Crée la colonne traits si absente (compatible MySQL 8+). */
    private void ensureTraitsColumn() {
        if (traitsColumnExists != null) return;
        try (ResultSet rs = cnx.getMetaData().getColumns(null, null, "filiere", "traits")) {
            if (rs.next()) { traitsColumnExists = true; return; }
        } catch (Exception ignored) {}
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate("ALTER TABLE filiere ADD COLUMN IF NOT EXISTS traits JSON NULL");
            traitsColumnExists = true;
            System.out.println("[FiliereService] Colonne traits ajoutée à la table filiere.");
        } catch (SQLException e) {
            traitsColumnExists = false;
            System.out.println("[FiliereService] traits column unavailable: " + e.getMessage());
        }
    }

    @Override
    public void ajouter(Filiere f) throws SQLException {
        ensureImageColumn();
        ensureTraitsColumn();
        String traitsJson = (f.getTraits() != null) ? f.getTraits() : null;
        boolean withImage  = Boolean.TRUE.equals(imageColumnExists);
        boolean withTraits = Boolean.TRUE.equals(traitsColumnExists);

        // Construire dynamiquement le SQL selon les colonnes disponibles
        StringBuilder cols = new StringBuilder("INSERT INTO filiere (nom, categorie, niveau, description, debouches, competences, icon");
        if (withImage)  cols.append(", image");
        if (withTraits) cols.append(", traits");
        cols.append(") VALUES (?, ?, ?, ?, ?, ?, ?");
        if (withImage)  cols.append(", ?");
        if (withTraits) cols.append(", ?");
        cols.append(")");

        try (PreparedStatement ps = cnx.prepareStatement(cols.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, f.getNom());
            ps.setString(idx++, f.getCategorie());
            ps.setString(idx++, f.getNiveau());
            ps.setString(idx++, f.getDescription());
            ps.setString(idx++, f.getDebouches());
            ps.setString(idx++, f.getCompetences());
            ps.setString(idx++, f.getIcon());
            if (withImage)  ps.setString(idx++, f.getImage());
            if (withTraits) ps.setString(idx++, traitsJson);
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
        ensureTraitsColumn();
        String traitsJson = (f.getTraits() != null) ? f.getTraits() : null;
        boolean withImage  = Boolean.TRUE.equals(imageColumnExists);
        boolean withTraits = Boolean.TRUE.equals(traitsColumnExists);

        StringBuilder sql = new StringBuilder("UPDATE filiere SET nom=?, categorie=?, niveau=?, description=?, debouches=?, competences=?, icon=?");
        if (withImage)  sql.append(", image=?");
        if (withTraits) sql.append(", traits=?");
        sql.append(" WHERE id=?");

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, f.getNom());
            ps.setString(idx++, f.getCategorie());
            ps.setString(idx++, f.getNiveau());
            ps.setString(idx++, f.getDescription());
            ps.setString(idx++, f.getDebouches());
            ps.setString(idx++, f.getCompetences());
            ps.setString(idx++, f.getIcon());
            if (withImage)  ps.setString(idx++, f.getImage());
            if (withTraits) ps.setString(idx++, traitsJson);
            ps.setInt(idx, f.getId());
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
        try {
            f.setTraits(rs.getString("traits"));
        } catch (SQLException ignored) {
            f.setTraits(null);
        }
        return f;
    }
}
