package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Candidature — table `candidature`.
 */
public class Candidature {

    private int    id;
    private String statut;
    private String lettreMotivation;
    private int    etudiantId;
    private int    offreId;
    private Date   createdAt;

    public Candidature() {
        this.createdAt = new Date();
    }

    public Candidature(String statut, String lettreMotivation, int etudiantId, int offreId) {
        this.statut           = statut;
        this.lettreMotivation = lettreMotivation;
        this.etudiantId       = etudiantId;
        this.offreId          = offreId;
        this.createdAt        = new Date();
    }

    public Candidature(int id, String statut, String lettreMotivation,
                       int etudiantId, int offreId, Date createdAt) {
        this.id               = id;
        this.statut           = statut;
        this.lettreMotivation = lettreMotivation;
        this.etudiantId       = etudiantId;
        this.offreId          = offreId;
        this.createdAt        = createdAt;
    }

    public int    getId()                                  { return id; }
    public void   setId(int id)                            { this.id = id; }
    public String getStatut()                              { return statut; }
    public void   setStatut(String statut)                 { this.statut = statut; }
    public String getLettreMotivation()                    { return lettreMotivation; }
    public void   setLettreMotivation(String l)            { this.lettreMotivation = l; }
    public int    getEtudiantId()                          { return etudiantId; }
    public void   setEtudiantId(int etudiantId)            { this.etudiantId = etudiantId; }
    public int    getOffreId()                             { return offreId; }
    public void   setOffreId(int offreId)                  { this.offreId = offreId; }
    public Date   getCreatedAt()                           { return createdAt; }
    public void   setCreatedAt(Date createdAt)             { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Candidature{id=" + id + ", statut='" + statut + "', etudiantId=" + etudiantId + "}";
    }
}
