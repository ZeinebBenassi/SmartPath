package com.smartpath.model;

public class Lecon {
    private int id;
    private String titre;
    private String contenu;
    private int matiereId;

    public Lecon() {}

    public Lecon(int id, String titre, String contenu, int matiereId) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.matiereId = matiereId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }
}
