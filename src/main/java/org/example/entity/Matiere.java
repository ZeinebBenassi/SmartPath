package org.example.entity;

import java.util.ArrayList;
import java.util.List;

public class Matiere {
    private int id;
    private String titre;
    private String description;
    private int filiereId;
    private int profId;
    private boolean isVisible = true;
    private List<Lecon> lecons = new ArrayList<>();
    private List<Test> tests = new ArrayList<>();

    public Matiere() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getFiliereId() { return filiereId; }
    public void setFiliereId(int filiereId) { this.filiereId = filiereId; }

    public int getProfId() { return profId; }
    public void setProfId(int profId) { this.profId = profId; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public List<Lecon> getLecons() { return lecons; }
    public void setLecons(List<Lecon> lecons) { this.lecons = lecons; }

    public List<Test> getTests() { return tests; }
    public void setTests(List<Test> tests) { this.tests = tests; }

    @Override
    public String toString() { return titre; }
}
