package tn.esprit.entity;

import java.util.Date;

public class Etudiant extends User {

    public static final String STATUS_ACTIF    = "actif";
    public static final String STATUS_BAN      = "ban";
    public static final String STATUS_SUSPENDU = "suspendu";

    private String niveau;
    private String status = STATUS_ACTIF;
    private Date   suspendedUntil;
    private int    filiereId;

    public Etudiant() {
        super();
        this.setRoles("[\"ROLE_ETUDIANT\"]");
    }

    public Etudiant(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password);
        this.status = STATUS_ACTIF;
        this.setRoles("[\"ROLE_ETUDIANT\"]");
    }

    public Etudiant(int id, String nom, String prenom, String email,
                    String password, String CIN, String telephone,
                    String adresse, Date dateNaissance, String photo,
                    String roles, Date createdAt,
                    String niveau, String status, Date suspendedUntil, int filiereId) {
        super(id, nom, prenom, email, password, CIN, telephone,
              adresse, dateNaissance, photo, roles, createdAt);
        this.niveau         = niveau;
        this.status         = status;
        this.suspendedUntil = suspendedUntil;
        this.filiereId      = filiereId;
    }

    public String getNiveau()                            { return niveau; }
    public void   setNiveau(String niveau)               { this.niveau = niveau; }
    public String getStatus()                            { return status; }
    public void   setStatus(String status)               { this.status = status; }
    public Date   getSuspendedUntil()                    { return suspendedUntil; }
    public void   setSuspendedUntil(Date suspendedUntil) { this.suspendedUntil = suspendedUntil; }
    public int    getFiliereId()                         { return filiereId; }
    public void   setFiliereId(int filiereId)            { this.filiereId = filiereId; }

    @Override
    public String toString() {
        return "Etudiant{id=" + getId() + ", nom='" + getNom() + "', prenom='" + getPrenom()
                + "', email='" + getEmail() + "', niveau='" + niveau + "', status='" + status + "'}";
    }
}
