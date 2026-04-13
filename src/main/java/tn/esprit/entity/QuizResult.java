package tn.esprit.entity;

import java.util.Date;


public class QuizResult {

    private int    id;
    private int    etudiantId;       // FK → etudiant.id
    private String responses;        // JSON
    private String scores;           // JSON
    private String recommendations;  // JSON
    private String profileType;      // ex: "analytique"
    private Date   createdAt;


    public QuizResult() {
        this.createdAt = new Date();
    }

    public QuizResult(int etudiantId, String responses, String scores,
                      String recommendations, String profileType) {
        this.etudiantId      = etudiantId;
        this.responses       = responses;
        this.scores          = scores;
        this.recommendations = recommendations;
        this.profileType     = profileType;
        this.createdAt       = new Date();
    }

    public QuizResult(int id, int etudiantId, String responses, String scores,
                      String recommendations, String profileType, Date createdAt) {
        this.id              = id;
        this.etudiantId      = etudiantId;
        this.responses       = responses;
        this.scores          = scores;
        this.recommendations = recommendations;
        this.profileType     = profileType;
        this.createdAt       = createdAt;
    }


    public int    getId()                                      { return id; }
    public void   setId(int id)                                { this.id = id; }

    public int    getEtudiantId()                              { return etudiantId; }
    public void   setEtudiantId(int etudiantId)                { this.etudiantId = etudiantId; }

    public String getResponses()                               { return responses; }
    public void   setResponses(String responses)               { this.responses = responses; }

    public String getScores()                                  { return scores; }
    public void   setScores(String scores)                     { this.scores = scores; }

    public String getRecommendations()                         { return recommendations; }
    public void   setRecommendations(String recommendations)   { this.recommendations = recommendations; }

    public String getProfileType()                             { return profileType; }
    public void   setProfileType(String profileType)           { this.profileType = profileType; }

    public Date   getCreatedAt()                               { return createdAt; }
    public void   setCreatedAt(Date createdAt)                 { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "QuizResult{id=" + id
                + ", etudiantId=" + etudiantId
                + ", profileType='" + profileType + "'}";
    }
}
