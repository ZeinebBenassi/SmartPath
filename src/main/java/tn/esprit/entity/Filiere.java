package tn.esprit.entity;

public class Filiere {

    private int    id;
    private String nom;
    private String categorie;
    private String niveau;
    private String description;
    private String debouches;
    private String competences;
    private String icon;
    private String image;
    /**
     * Traits de personnalité stockés en JSON, ex:
     * {"analytique":5,"pratique":3,"creatif":1,...}
     * Identique au champ `traits` JSON de l'entité Symfony Filiere.
     */
    private String traits;

    public Filiere() {}

    public Filiere(String nom, String categorie, String niveau, String description) {
        this.nom = nom; this.categorie = categorie; this.niveau = niveau; this.description = description;
    }

    public Filiere(int id, String nom, String categorie, String niveau,
                   String description, String debouches, String competences, String icon) {
        this.id = id; this.nom = nom; this.categorie = categorie; this.niveau = niveau;
        this.description = description; this.debouches = debouches;
        this.competences = competences; this.icon = icon;
    }

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }
    public String getNom()                       { return nom; }
    public void   setNom(String nom)             { this.nom = nom; }
    public String getCategorie()                 { return categorie; }
    public void   setCategorie(String c)         { this.categorie = c; }
    public String getNiveau()                    { return niveau; }
    public void   setNiveau(String n)            { this.niveau = n; }
    public String getDescription()              { return description; }
    public void   setDescription(String d)       { this.description = d; }
    public String getDebouches()                 { return debouches; }
    public void   setDebouches(String d)         { this.debouches = d; }
    public String getCompetences()               { return competences; }
    public void   setCompetences(String c)       { this.competences = c; }
    public String getIcon()                      { return icon; }
    public void   setIcon(String icon)           { this.icon = icon; }
    public String getImage()                      { return image; }
    public void   setImage(String image)          { this.image = image; }
    public String getTraits()                      { return traits; }
    public void   setTraits(String traits)         { this.traits = traits; }
}
