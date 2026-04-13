package org.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Test {
    private int id;
    private String titre;
    private String contenu;
    private LocalDate dateTest;
    private int duree;
    private int profId; // Link to Prof
    private int matiereId; // Link to Matiere
    private LocalDateTime createdAt;
    private List<QcmQuestion> qcmQuestions = new ArrayList<>();

    public Test() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDate getDateTest() { return dateTest; }
    public void setDateTest(LocalDate dateTest) { this.dateTest = dateTest; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public int getProfId() { return profId; }
    public void setProfId(int profId) { this.profId = profId; }

    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<QcmQuestion> getQcmQuestions() { return qcmQuestions; }
    public void setQcmQuestions(List<QcmQuestion> qcmQuestions) { this.qcmQuestions = qcmQuestions; }

    @Override
    public String toString() { return titre; }
}
