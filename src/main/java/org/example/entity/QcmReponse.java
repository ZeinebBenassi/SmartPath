package org.example.entity;

public class QcmReponse {
    private int id;
    private String texte;
    private boolean estCorrecte = false;
    private int questionId; // Link to Question

    public QcmReponse() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public boolean isEstCorrecte() { return estCorrecte; }
    public void setEstCorrecte(boolean estCorrecte) { this.estCorrecte = estCorrecte; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
}
