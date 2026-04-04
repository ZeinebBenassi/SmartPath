package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Score — table `score`.
 */
public class Score {

    private int    id;
    private double note;
    private Date   dateEvaluation;
    private int    etudiantId;
    private int    matiereId;

    public Score() {}

    public Score(double note, Date dateEvaluation, int etudiantId, int matiereId) {
        this.note           = note;
        this.dateEvaluation = dateEvaluation;
        this.etudiantId     = etudiantId;
        this.matiereId      = matiereId;
    }

    public Score(int id, double note, Date dateEvaluation, int etudiantId, int matiereId) {
        this.id             = id;
        this.note           = note;
        this.dateEvaluation = dateEvaluation;
        this.etudiantId     = etudiantId;
        this.matiereId      = matiereId;
    }

    public int    getId()                              { return id; }
    public void   setId(int id)                        { this.id = id; }
    public double getNote()                            { return note; }
    public void   setNote(double note)                 { this.note = note; }
    public Date   getDateEvaluation()                  { return dateEvaluation; }
    public void   setDateEvaluation(Date d)            { this.dateEvaluation = d; }
    public int    getEtudiantId()                      { return etudiantId; }
    public void   setEtudiantId(int etudiantId)        { this.etudiantId = etudiantId; }
    public int    getMatiereId()                       { return matiereId; }
    public void   setMatiereId(int matiereId)          { this.matiereId = matiereId; }

    @Override
    public String toString() {
        return "Score{id=" + id + ", note=" + note + ", etudiantId=" + etudiantId + "}";
    }
}
