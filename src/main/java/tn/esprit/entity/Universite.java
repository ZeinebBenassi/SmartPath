package tn.esprit.entity;

import java.util.Date;

/**
 * Entité Universite — table `universite`.
 * Relations : ManyToMany Filiere (table de jointure : universite_filiere).
 */
public class Universite {

    private int    id;
    private String nom;
    private String ville;
    private String type;                 // "Public", "Privé", "École d'ingénieurs"
    private String description;
    private String siteWeb;
    private String adresse;
    private String telephone;
    private String email;
    private String filieresProposes;     // JSON
    private String diplomes;             // JSON
    private double fraisAnnuels;
    private String acces;                // "Bac+0", "Bac+2"
    private String conditionsAdmission;
    private int    capaciteAccueil;
    private double tauxReussite;
    private double tauxInsertion;
    private String sourceUrl;
    private Date   createdAt;
    private Date   updatedAt;


    public Universite() {
        this.createdAt = new Date();
    }

    public Universite(String nom, String ville, String type) {
        this.nom       = nom;
        this.ville     = ville;
        this.type      = type;
        this.createdAt = new Date();
    }

    public Universite(int id, String nom, String ville, String type,
                      String description, String siteWeb, String adresse,
                      String telephone, String email, String filieresProposes,
                      String diplomes, double fraisAnnuels, String acces,
                      String conditionsAdmission, int capaciteAccueil,
                      double tauxReussite, double tauxInsertion,
                      String sourceUrl, Date createdAt, Date updatedAt) {
        this.id                  = id;
        this.nom                 = nom;
        this.ville               = ville;
        this.type                = type;
        this.description         = description;
        this.siteWeb             = siteWeb;
        this.adresse             = adresse;
        this.telephone           = telephone;
        this.email               = email;
        this.filieresProposes    = filieresProposes;
        this.diplomes            = diplomes;
        this.fraisAnnuels        = fraisAnnuels;
        this.acces               = acces;
        this.conditionsAdmission = conditionsAdmission;
        this.capaciteAccueil     = capaciteAccueil;
        this.tauxReussite        = tauxReussite;
        this.tauxInsertion       = tauxInsertion;
        this.sourceUrl           = sourceUrl;
        this.createdAt           = createdAt;
        this.updatedAt           = updatedAt;
    }


    public int    getId()                                     { return id; }
    public void   setId(int id)                               { this.id = id; }

    public String getNom()                                    { return nom; }
    public void   setNom(String nom)                          { this.nom = nom; }

    public String getVille()                                  { return ville; }
    public void   setVille(String ville)                      { this.ville = ville; }

    public String getType()                                   { return type; }
    public void   setType(String type)                        { this.type = type; }

    public String getDescription()                            { return description; }
    public void   setDescription(String description)          { this.description = description; }

    public String getSiteWeb()                                { return siteWeb; }
    public void   setSiteWeb(String siteWeb)                  { this.siteWeb = siteWeb; }

    public String getAdresse()                                { return adresse; }
    public void   setAdresse(String adresse)                  { this.adresse = adresse; }

    public String getTelephone()                              { return telephone; }
    public void   setTelephone(String telephone)              { this.telephone = telephone; }

    public String getEmail()                                  { return email; }
    public void   setEmail(String email)                      { this.email = email; }

    public String getFilieresProposes()                       { return filieresProposes; }
    public void   setFilieresProposes(String fp)              { this.filieresProposes = fp; }

    public String getDiplomes()                               { return diplomes; }
    public void   setDiplomes(String diplomes)                { this.diplomes = diplomes; }

    public double getFraisAnnuels()                           { return fraisAnnuels; }
    public void   setFraisAnnuels(double fraisAnnuels)        { this.fraisAnnuels = fraisAnnuels; }

    public String getAcces()                                  { return acces; }
    public void   setAcces(String acces)                      { this.acces = acces; }

    public String getConditionsAdmission()                    { return conditionsAdmission; }
    public void   setConditionsAdmission(String ca)           { this.conditionsAdmission = ca; }

    public int    getCapaciteAccueil()                        { return capaciteAccueil; }
    public void   setCapaciteAccueil(int capaciteAccueil)     { this.capaciteAccueil = capaciteAccueil; }

    public double getTauxReussite()                           { return tauxReussite; }
    public void   setTauxReussite(double tauxReussite)        { this.tauxReussite = tauxReussite; }

    public double getTauxInsertion()                          { return tauxInsertion; }
    public void   setTauxInsertion(double tauxInsertion)      { this.tauxInsertion = tauxInsertion; }

    public String getSourceUrl()                              { return sourceUrl; }
    public void   setSourceUrl(String sourceUrl)              { this.sourceUrl = sourceUrl; }

    public Date   getCreatedAt()                              { return createdAt; }
    public void   setCreatedAt(Date createdAt)                { this.createdAt = createdAt; }

    public Date   getUpdatedAt()                              { return updatedAt; }
    public void   setUpdatedAt(Date updatedAt)                { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Universite{id=" + id + ", nom='" + nom
                + "', ville='" + ville
                + "', type='" + type + "'}";
    }
}
