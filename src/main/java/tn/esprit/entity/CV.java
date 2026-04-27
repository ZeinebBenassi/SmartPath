package tn.esprit.entity;

import java.util.Date;

/**
 * Entité CV — table `cv`.
 */
public class CV {

    private int    id;
    private String fichier;
    private int    etudiantId;
    private Date   createdAt;

    public CV() {
        this.createdAt = new Date();
    }

    public CV(String fichier, int etudiantId) {
        this.fichier     = fichier;
        this.etudiantId  = etudiantId;
        this.createdAt   = new Date();
    }

    public CV(int id, String fichier, int etudiantId, Date createdAt) {
        this.id         = id;
        this.fichier    = fichier;
        this.etudiantId = etudiantId;
        this.createdAt  = createdAt;
    }

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }
    public String getFichier()                   { return fichier; }
    public void   setFichier(String fichier)     { this.fichier = fichier; }
    public int    getEtudiantId()                { return etudiantId; }
    public void   setEtudiantId(int etudiantId)  { this.etudiantId = etudiantId; }
    public Date   getCreatedAt()                 { return createdAt; }
    public void   setCreatedAt(Date createdAt)   { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "CV{id=" + id + ", fichier='" + fichier + "', etudiantId=" + etudiantId + "}";
    }
}
