package org.example.entity;

import java.util.ArrayList;
import java.util.List;

public class QcmQuestion {
    private int id;
    private String texte;
    private int ordre = 1;
    private int testId; // Link to Test
    private List<QcmReponse> reponses = new ArrayList<>();

    public QcmQuestion() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public List<QcmReponse> getReponses() { return reponses; }
    public void setReponses(List<QcmReponse> reponses) { this.reponses = reponses; }

    public QcmReponse getReponseCorrecte() {
        for (QcmReponse r : reponses) {
            if (r.isEstCorrecte()) return r;
        }
        return null;
    }
}
