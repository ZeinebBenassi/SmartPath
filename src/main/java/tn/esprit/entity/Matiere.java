package tn.esprit.entity;

/**
 * Entité Matiere — table `matiere`.
 */
public class Matiere {

    private int    id;
    private String titre;
    private String description;
    private double coefficient  = 1.0;
    private String domaine;
    private double noteMinRequise;
    private double noteMax      = 20.0;
    private int    filiereId;
    private int    profId;

    public Matiere() {}

    public Matiere(String titre, String description, double coefficient,
                   String domaine, int filiereId, int profId) {
        this.titre       = titre;
        this.description = description;
        this.coefficient = coefficient;
        this.domaine     = domaine;
        this.filiereId   = filiereId;
        this.profId      = profId;
        this.noteMax     = 20.0;
    }

    public Matiere(int id, String titre, String description, double coefficient,
                   String domaine, double noteMinRequise, double noteMax,
                   int filiereId, int profId) {
        this.id             = id;
        this.titre          = titre;
        this.description    = description;
        this.coefficient    = coefficient;
        this.domaine        = domaine;
        this.noteMinRequise = noteMinRequise;
        this.noteMax        = noteMax;
        this.filiereId      = filiereId;
        this.profId         = profId;
    }

    public int    getId()                             { return id; }
    public void   setId(int id)                       { this.id = id; }
    public String getTitre()                          { return titre; }
    public void   setTitre(String titre)              { this.titre = titre; }
    public String getDescription()                    { return description; }
    public void   setDescription(String description)  { this.description = description; }
    public double getCoefficient()                    { return coefficient; }
    public void   setCoefficient(double coefficient)  { this.coefficient = coefficient; }
    public String getDomaine()                        { return domaine; }
    public void   setDomaine(String domaine)          { this.domaine = domaine; }
    public double getNoteMinRequise()                 { return noteMinRequise; }
    public void   setNoteMinRequise(double n)         { this.noteMinRequise = n; }
    public double getNoteMax()                        { return noteMax; }
    public void   setNoteMax(double noteMax)          { this.noteMax = noteMax; }
    public int    getFiliereId()                      { return filiereId; }
    public void   setFiliereId(int filiereId)         { this.filiereId = filiereId; }
    public int    getProfId()                         { return profId; }
    public void   setProfId(int profId)               { this.profId = profId; }

    @Override
    public String toString() {
        return "Matiere{id=" + id + ", titre='" + titre + "', coefficient=" + coefficient + "}";
    }
}
