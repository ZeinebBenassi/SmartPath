package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Prof — table `prof` (JOINED avec `user`).
 */
public class Prof extends User {

    private String specialite;

    public Prof() {
        super();
        this.setRoles("[\"ROLE_ENSEIGNANT\"]");
    }

    public Prof(String nom, String prenom, String email, String password, String specialite) {
        super(nom, prenom, email, password);
        this.specialite = specialite;
        this.setRoles("[\"ROLE_ENSEIGNANT\"]");
    }

    public Prof(int id, String nom, String prenom, String email,
                String password, String CIN, String telephone,
                String adresse, Date dateNaissance, String photo,
                String roles, Date createdAt, String specialite) {
        super(id, nom, prenom, email, password, CIN, telephone,
              adresse, dateNaissance, photo, roles, createdAt);
        this.specialite = specialite;
    }

    public String getSpecialite()              { return specialite; }
    public void   setSpecialite(String s)      { this.specialite = s; }

    @Override
    public String toString() {
        return "Prof{id=" + getId() + ", nom='" + getNom() + "', prenom='" + getPrenom()
                + "', specialite='" + specialite + "'}";
    }
}
