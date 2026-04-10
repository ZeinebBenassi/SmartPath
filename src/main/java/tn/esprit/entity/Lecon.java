package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Lecon — table `lecon`.
 */
public class Lecon {

    private int    id;
    private String titre;
    private String description;
    private String contenu;
    private String fichier;
    private int    duree;
    private int    matiereId;
    private int    profId;
    private Date   createdAt;

    public Lecon() {
        this.createdAt = new Date();
    }

    public Lecon(String titre, String description, String contenu,
                 String fichier, int duree, int matiereId, int profId) {
        this.titre       = titre;
        this.description = description;
        this.contenu     = contenu;
        this.fichier     = fichier;
        this.duree       = duree;
        this.matiereId   = matiereId;
        this.profId      = profId;
        this.createdAt   = new Date();
    }

    public Lecon(int id, String titre, String description, String contenu,
                 String fichier, int duree, int matiereId, int profId, Date createdAt) {
        this.id          = id;
        this.titre       = titre;
        this.description = description;
        this.contenu     = contenu;
        this.fichier     = fichier;
        this.duree       = duree;
        this.matiereId   = matiereId;
        this.profId      = profId;
        this.createdAt   = createdAt;
    }

    public int    getId()                            { return id; }
    public void   setId(int id)                      { this.id = id; }
    public String getTitre()                         { return titre; }
    public void   setTitre(String titre)             { this.titre = titre; }
    public String getDescription()                   { return description; }
    public void   setDescription(String description) { this.description = description; }
    public String getContenu()                       { return contenu; }
    public void   setContenu(String contenu)         { this.contenu = contenu; }
    public String getFichier()                       { return fichier; }
    public void   setFichier(String fichier)         { this.fichier = fichier; }
    public int    getDuree()                         { return duree; }
    public void   setDuree(int duree)                { this.duree = duree; }
    public int    getMatiereId()                     { return matiereId; }
    public void   setMatiereId(int matiereId)        { this.matiereId = matiereId; }
    public int    getProfId()                        { return profId; }
    public void   setProfId(int profId)              { this.profId = profId; }
    public Date   getCreatedAt()                     { return createdAt; }
    public void   setCreatedAt(Date createdAt)       { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Lecon{id=" + id + ", titre='" + titre + "', duree=" + duree + " min}";
    }
}
