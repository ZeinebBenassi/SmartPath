package models;

import java.util.Date;

/**
 * models.User etend tn.esprit.entity.User pour assurer la compatibilite
 * entre UserService (qui retourne tn.esprit.entity.User) et les controllers.
 * Tous les champs sont herites de tn.esprit.entity.User.
 */
public class User extends tn.esprit.entity.User {

    public User() {
        super();
    }

    public User(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password);
    }

    public User(int id, String nom, String prenom, String email,
                String password, String cin, String telephone,
                String adresse, Date dateNaissance, String photo,
                String roles, Date createdAt) {
        super(id, nom, prenom, email, password, cin, telephone,
              adresse, dateNaissance, photo, roles, createdAt);
    }
}
