package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Test — table `test`.
 */
public class Test {

    private int    id;
    private String titre;
    private String contenu;
    private Date   dateTest;
    private int    duree;
    private int    profId;
    private int    matiereId;
    private Date   createdAt;

    public Test() {
        this.createdAt = new Date();
    }

    public Test(String titre, String contenu, Date dateTest, int duree, int profId, int matiereId) {
        this.titre     = titre;
        this.contenu   = contenu;
        this.dateTest  = dateTest;
        this.duree     = duree;
        this.profId    = profId;
        this.matiereId = matiereId;
        this.createdAt = new Date();
    }

    public Test(int id, String titre, String contenu, Date dateTest,
                int duree, int profId, int matiereId, Date createdAt) {
        this.id        = id;
        this.titre     = titre;
        this.contenu   = contenu;
        this.dateTest  = dateTest;
        this.duree     = duree;
        this.profId    = profId;
        this.matiereId = matiereId;
        this.createdAt = createdAt;
    }

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }
    public String getTitre()                     { return titre; }
    public void   setTitre(String titre)         { this.titre = titre; }
    public String getContenu()                   { return contenu; }
    public void   setContenu(String contenu)     { this.contenu = contenu; }
    public Date   getDateTest()                  { return dateTest; }
    public void   setDateTest(Date dateTest)     { this.dateTest = dateTest; }
    public int    getDuree()                     { return duree; }
    public void   setDuree(int duree)            { this.duree = duree; }
    public int    getProfId()                    { return profId; }
    public void   setProfId(int profId)          { this.profId = profId; }
    public int    getMatiereId()                 { return matiereId; }
    public void   setMatiereId(int matiereId)    { this.matiereId = matiereId; }
    public Date   getCreatedAt()                 { return createdAt; }
    public void   setCreatedAt(Date createdAt)   { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Test{id=" + id + ", titre='" + titre + "', duree=" + duree + " min}";
    }
}
