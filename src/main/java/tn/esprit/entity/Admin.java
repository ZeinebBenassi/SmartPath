package tn.esprit.entity;

import java.util.Date;


public class Admin extends User {

    public Admin() {
        super();
        this.setRoles("[\"ROLE_ADMIN\"]");
    }

    public Admin(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password);
        this.setRoles("[\"ROLE_ADMIN\"]");
    }

    public Admin(int id, String nom, String prenom, String email,
                 String password, String CIN, String telephone,
                 String adresse, Date dateNaissance, String photo,
                 String roles, Date createdAt) {
        super(id, nom, prenom, email, password, CIN, telephone,
              adresse, dateNaissance, photo, roles, createdAt);
    }

    @Override
    public String toString() {
        return "Admin{id=" + getId() + ", nom='" + getNom() + "', email='" + getEmail() + "'}";
    }
}
