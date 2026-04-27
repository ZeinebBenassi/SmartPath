package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Offre — table `offre`.
 */
public class Offre {

    private int    id;
    private String titre;
    private String entreprise;
    private String description;
    private String type;
    private String lieu;
    private int    adminId;
    private Date   createdAt;

    public Offre() {
        this.createdAt = new Date();
    }

    public Offre(String titre, String entreprise, String description,
                 String type, String lieu, int adminId) {
        this.titre       = titre;
        this.entreprise  = entreprise;
        this.description = description;
        this.type        = type;
        this.lieu        = lieu;
        this.adminId     = adminId;
        this.createdAt   = new Date();
    }

    public Offre(int id, String titre, String entreprise, String description,
                 String type, String lieu, int adminId, Date createdAt) {
        this.id          = id;
        this.titre       = titre;
        this.entreprise  = entreprise;
        this.description = description;
        this.type        = type;
        this.lieu        = lieu;
        this.adminId     = adminId;
        this.createdAt   = createdAt;
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }
    public String getTitre()                       { return titre; }
    public void   setTitre(String titre)           { this.titre = titre; }
    public String getEntreprise()                  { return entreprise; }
    public void   setEntreprise(String entreprise) { this.entreprise = entreprise; }
    public String getDescription()                 { return description; }
    public void   setDescription(String desc)      { this.description = desc; }
    public String getType()                        { return type; }
    public void   setType(String type)             { this.type = type; }
    public String getLieu()                        { return lieu; }
    public void   setLieu(String lieu)             { this.lieu = lieu; }
    public int    getAdminId()                     { return adminId; }
    public void   setAdminId(int adminId)          { this.adminId = adminId; }
    public Date   getCreatedAt()                   { return createdAt; }
    public void   setCreatedAt(Date createdAt)     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Offre{id=" + id + ", titre='" + titre + "', entreprise='" + entreprise + "'}";
    }
}
