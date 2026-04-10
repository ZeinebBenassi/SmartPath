package tn.esprit.entity;

import java.util.Date;

public class QuizResult {

    private int    id;
    private int    etudiantId;
    private String responses;
    private String scores;
    private String recommendations;
    private String profileType;
    private Date   createdAt;

    public QuizResult() { this.createdAt = new Date(); }

    public QuizResult(int id, int etudiantId, String responses, String scores,
                      String recommendations, String profileType, Date createdAt) {
        this.id = id; this.etudiantId = etudiantId; this.responses = responses;
        this.scores = scores; this.recommendations = recommendations;
        this.profileType = profileType; this.createdAt = createdAt;
    }

    public int    getId()                                    { return id; }
    public void   setId(int id)                              { this.id = id; }
    public int    getEtudiantId()                            { return etudiantId; }
    public void   setEtudiantId(int eid)                     { this.etudiantId = eid; }
    public String getResponses()                             { return responses; }
    public void   setResponses(String r)                     { this.responses = r; }
    public String getScores()                                { return scores; }
    public void   setScores(String s)                        { this.scores = s; }
    public String getRecommendations()                       { return recommendations; }
    public void   setRecommendations(String r)               { this.recommendations = r; }
    public String getProfileType()                           { return profileType; }
    public void   setProfileType(String p)                   { this.profileType = p; }
    public Date   getCreatedAt()                             { return createdAt; }
    public void   setCreatedAt(Date d)                       { this.createdAt = d; }
}
