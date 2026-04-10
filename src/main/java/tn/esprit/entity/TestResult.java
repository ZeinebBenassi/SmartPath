package tn.esprit.entity;

import java.util.Date;

/**
 * Entité TestResult — table `test_result`.
 */
public class TestResult {

    private int    id;
    private double score;
    private Date   dateSoumission;
    private int    etudiantId;
    private int    testId;

    public TestResult() {}

    public TestResult(double score, Date dateSoumission, int etudiantId, int testId) {
        this.score          = score;
        this.dateSoumission = dateSoumission;
        this.etudiantId     = etudiantId;
        this.testId         = testId;
    }

    public TestResult(int id, double score, Date dateSoumission, int etudiantId, int testId) {
        this.id             = id;
        this.score          = score;
        this.dateSoumission = dateSoumission;
        this.etudiantId     = etudiantId;
        this.testId         = testId;
    }

    public int    getId()                            { return id; }
    public void   setId(int id)                      { this.id = id; }
    public double getScore()                         { return score; }
    public void   setScore(double score)             { this.score = score; }
    public Date   getDateSoumission()                { return dateSoumission; }
    public void   setDateSoumission(Date d)          { this.dateSoumission = d; }
    public int    getEtudiantId()                    { return etudiantId; }
    public void   setEtudiantId(int etudiantId)      { this.etudiantId = etudiantId; }
    public int    getTestId()                        { return testId; }
    public void   setTestId(int testId)              { this.testId = testId; }

    @Override
    public String toString() {
        return "TestResult{id=" + id + ", score=" + score + ", etudiantId=" + etudiantId + "}";
    }
}
