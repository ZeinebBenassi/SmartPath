package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Plan — table `plan`.
 */
public class Plan {

    private int    id;
    private String objectif;
    private String duree;
    private String niveauCible;
    private int    profId;
    private int    filiereId;
    private Date   createdAt;

    public Plan() {
        this.createdAt = new Date();
    }

    public Plan(String objectif, String duree, String niveauCible, int profId, int filiereId) {
        this.objectif    = objectif;
        this.duree       = duree;
        this.niveauCible = niveauCible;
        this.profId      = profId;
        this.filiereId   = filiereId;
        this.createdAt   = new Date();
    }

    public Plan(int id, String objectif, String duree, String niveauCible,
                int profId, int filiereId, Date createdAt) {
        this.id          = id;
        this.objectif    = objectif;
        this.duree       = duree;
        this.niveauCible = niveauCible;
        this.profId      = profId;
        this.filiereId   = filiereId;
        this.createdAt   = createdAt;
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }
    public String getObjectif()                    { return objectif; }
    public void   setObjectif(String objectif)     { this.objectif = objectif; }
    public String getDuree()                       { return duree; }
    public void   setDuree(String duree)           { this.duree = duree; }
    public String getNiveauCible()                 { return niveauCible; }
    public void   setNiveauCible(String n)         { this.niveauCible = n; }
    public int    getProfId()                      { return profId; }
    public void   setProfId(int profId)            { this.profId = profId; }
    public int    getFiliereId()                   { return filiereId; }
    public void   setFiliereId(int filiereId)      { this.filiereId = filiereId; }
    public Date   getCreatedAt()                   { return createdAt; }
    public void   setCreatedAt(Date createdAt)     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Plan{id=" + id + ", objectif='" + objectif + "', niveauCible='" + niveauCible + "'}";
    }
}
