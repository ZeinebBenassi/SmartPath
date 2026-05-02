package tn.esprit.entity.feature_cours_et_quiz;

public class Matiere {
    private int id;
    private String titre;
    private String description;
    private int filiereId;
    private int profId;
    private boolean isVisible;
    private double rating;
    private int nbAvis;

    public Matiere() {}
    public Matiere(int id, String titre, String description) {
        this.id = id; this.titre = titre; this.description = description;
    }

    public Matiere(int id, String titre, String description, double rating, int nbAvis) {
        this.id = id; this.titre = titre; this.description = description;
        this.rating = rating; this.nbAvis = nbAvis;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getFiliereId() { return filiereId; }
    public void setFiliereId(int filiereId) { this.filiereId = filiereId; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getNbAvis() { return nbAvis; }
    public void setNbAvis(int nbAvis) { this.nbAvis = nbAvis; }
}
