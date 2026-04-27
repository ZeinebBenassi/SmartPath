package tn.esprit.entity;

import java.util.Date;


public class ReleveNotes {

    private int    id;
    private int    etudiantId;          // FK → etudiant.id
    private String fichierPath;
    private String fichierType;         // "pdf", "jpg"…
    private String texteExtrait;
    private String notesDetectees;      // JSON
    private String scoreParFiliere;     // JSON
    private String filiereRecommandee;
    private String analyseIA;
    private double moyenneGenerale;
    private Date   createdAt;



    public ReleveNotes() {
        this.createdAt = new Date();
    }

    public ReleveNotes(int etudiantId, String fichierPath, String fichierType) {
        this.etudiantId  = etudiantId;
        this.fichierPath = fichierPath;
        this.fichierType = fichierType;
        this.createdAt   = new Date();
    }

    public ReleveNotes(int id, int etudiantId, String fichierPath,
                       String fichierType, String texteExtrait,
                       String notesDetectees, String scoreParFiliere,
                       String filiereRecommandee, String analyseIA,
                       double moyenneGenerale, Date createdAt) {
        this.id                 = id;
        this.etudiantId         = etudiantId;
        this.fichierPath        = fichierPath;
        this.fichierType        = fichierType;
        this.texteExtrait       = texteExtrait;
        this.notesDetectees     = notesDetectees;
        this.scoreParFiliere    = scoreParFiliere;
        this.filiereRecommandee = filiereRecommandee;
        this.analyseIA          = analyseIA;
        this.moyenneGenerale    = moyenneGenerale;
        this.createdAt          = createdAt;
    }

    public int    getId()                                          { return id; }
    public void   setId(int id)                                    { this.id = id; }

    public int    getEtudiantId()                                  { return etudiantId; }
    public void   setEtudiantId(int etudiantId)                    { this.etudiantId = etudiantId; }

    public String getFichierPath()                                 { return fichierPath; }
    public void   setFichierPath(String fichierPath)               { this.fichierPath = fichierPath; }

    public String getFichierType()                                 { return fichierType; }
    public void   setFichierType(String fichierType)               { this.fichierType = fichierType; }

    public String getTexteExtrait()                                { return texteExtrait; }
    public void   setTexteExtrait(String texteExtrait)             { this.texteExtrait = texteExtrait; }

    public String getNotesDetectees()                              { return notesDetectees; }
    public void   setNotesDetectees(String notesDetectees)         { this.notesDetectees = notesDetectees; }

    public String getScoreParFiliere()                             { return scoreParFiliere; }
    public void   setScoreParFiliere(String scoreParFiliere)       { this.scoreParFiliere = scoreParFiliere; }

    public String getFiliereRecommandee()                          { return filiereRecommandee; }
    public void   setFiliereRecommandee(String filiereRecommandee) { this.filiereRecommandee = filiereRecommandee; }

    public String getAnalyseIA()                                   { return analyseIA; }
    public void   setAnalyseIA(String analyseIA)                   { this.analyseIA = analyseIA; }

    public double getMoyenneGenerale()                             { return moyenneGenerale; }
    public void   setMoyenneGenerale(double moyenneGenerale)       { this.moyenneGenerale = moyenneGenerale; }

    public Date   getCreatedAt()                                   { return createdAt; }
    public void   setCreatedAt(Date createdAt)                     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ReleveNotes{id=" + id
                + ", etudiantId=" + etudiantId
                + ", filiereRecommandee='" + filiereRecommandee
                + "', moyenneGenerale=" + moyenneGenerale + "}";
    }
}
