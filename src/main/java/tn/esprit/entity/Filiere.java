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

    public Filiere() {}

    public Filiere(String nom, String categorie, String niveau, String description) {
        this.nom         = nom;
        this.categorie   = categorie;
        this.niveau      = niveau;
        this.description = description;
    }

    public Filiere(int id, String nom, String categorie, String niveau,
                   String description, String debouches, String competences, String icon) {
        this.id          = id;
        this.nom         = nom;
        this.categorie   = categorie;
        this.niveau      = niveau;
        this.description = description;
        this.debouches   = debouches;
        this.competences = competences;
        this.icon        = icon;
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }
    public String getNom()                         { return nom; }
    public void   setNom(String nom)               { this.nom = nom; }
    public String getCategorie()                   { return categorie; }
    public void   setCategorie(String categorie)   { this.categorie = categorie; }
    public String getNiveau()                      { return niveau; }
    public void   setNiveau(String niveau)         { this.niveau = niveau; }
    public String getDescription()                 { return description; }
    public void   setDescription(String desc)      { this.description = desc; }
    public String getDebouches()                   { return debouches; }
    public void   setDebouches(String debouches)   { this.debouches = debouches; }
    public String getCompetences()                 { return competences; }
    public void   setCompetences(String comp)      { this.competences = comp; }
    public String getIcon()                        { return icon; }
    public void   setIcon(String icon)             { this.icon = icon; }

    @Override
    public String toString() {
        return "Filiere{id=" + id + ", nom='" + nom + "', niveau='" + niveau + "'}";
    }
}
