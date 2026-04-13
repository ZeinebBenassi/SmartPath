package com.smartpath.model;

public class Quiz {
    private int id;
    private String titre;
    private String contenu;
    private int duree;
    private int matiereId;

    public Quiz() {}

    public Quiz(int id, String titre, String contenu, int duree, int matiereId) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.duree = duree;
        this.matiereId = matiereId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }

    @Override
    public String toString() {
        return titre;
    }
}