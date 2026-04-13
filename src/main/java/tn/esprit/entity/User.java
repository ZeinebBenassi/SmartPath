package tn.esprit.entity;

import java.util.Date;


public class User {

    private int    id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String CIN;
    private String telephone;
    private String adresse;
    private Date   dateNaissance;
    private String photo;
    private String roles;
    private Date   createdAt;
    private String status; // "actif" ou "ban"
    private String type;   // "admin", "prof", "etudiant"

    public User() {
        this.createdAt = new Date();
    }

    public User(String nom, String prenom, String email, String password) {
        this.nom       = nom;
        this.prenom    = prenom;
        this.email     = email;
        this.password  = password;
        this.createdAt = new Date();
    }

    public User(int id, String nom, String prenom, String email,
                String password, String CIN, String telephone,
                String adresse, Date dateNaissance, String photo,
                String roles, Date createdAt) {
        this.id            = id;
        this.nom           = nom;
        this.prenom        = prenom;
        this.email         = email;
        this.password      = password;
        this.CIN           = CIN;
        this.telephone     = telephone;
        this.adresse       = adresse;
        this.dateNaissance = dateNaissance;
        this.photo         = photo;
        this.roles         = roles;
        this.createdAt     = createdAt;
    }

    public int    getId()                              { return id; }
    public void   setId(int id)                        { this.id = id; }
    public String getNom()                             { return nom; }
    public void   setNom(String nom)                   { this.nom = nom; }
    public String getPrenom()                          { return prenom; }
    public void   setPrenom(String prenom)             { this.prenom = prenom; }
    public String getEmail()                           { return email; }
    public void   setEmail(String email)               { this.email = email; }
    public String getPassword()                        { return password; }
    public void   setPassword(String password)         { this.password = password; }
    public String getCIN()                             { return CIN; }
    public void   setCIN(String CIN)                   { this.CIN = CIN; }
    public String getCin()                             { return CIN; }  // Alias lowercase
    public void   setCin(String cin)                   { this.CIN = cin; }  // Alias lowercase
    public String getTelephone()                       { return telephone; }
    public void   setTelephone(String telephone)       { this.telephone = telephone; }
    public String getAdresse()                         { return adresse; }
    public void   setAdresse(String adresse)           { this.adresse = adresse; }
    public Date   getDateNaissance()                   { return dateNaissance; }
    public void   setDateNaissance(Date dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getPhoto()                           { return photo; }
    public void   setPhoto(String photo)               { this.photo = photo; }
    public String getRoles()                           { return roles; }
    public void   setRoles(String roles)               { this.roles = roles; }
    public Date   getCreatedAt()                       { return createdAt; }
    public void   setCreatedAt(Date createdAt)         { this.createdAt = createdAt; }
    public String getStatus()                          { return status != null ? status : "actif"; }
    public void   setStatus(String status)             { this.status = status; }
    public String getType()                            { return type; }
    public void   setType(String type)                 { this.type = type; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "'}";
    }
}
