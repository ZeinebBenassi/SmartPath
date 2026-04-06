package com.smartpath.model;

public class Question {
    private int id;
    private String text;
    private String category;
    private int ordre;
    private boolean isActive;

    public Question() {}
    public Question(int id, String text, String category) {
        this.id = id; this.text = text; this.category = category;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
}