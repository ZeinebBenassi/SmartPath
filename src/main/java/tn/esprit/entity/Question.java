package tn.esprit.entity;

import java.util.ArrayList;
import java.util.List;

public class Question {

    private int          id;
    private String       text;
    private String       category;
    private int          ordre;
    private boolean      isActive = true;
    private List<Answer> answers  = new ArrayList<>();

    public Question() {}

    public Question(int id, String text, String category, int ordre, boolean isActive) {
        this.id = id; this.text = text; this.category = category;
        this.ordre = ordre; this.isActive = isActive;
    }

    public int          getId()                          { return id; }
    public void         setId(int id)                    { this.id = id; }
    public String       getText()                        { return text; }
    public void         setText(String t)                { this.text = t; }
    public String       getCategory()                   { return category; }
    public void         setCategory(String c)            { this.category = c; }
    public int          getOrdre()                       { return ordre; }
    public void         setOrdre(int o)                  { this.ordre = o; }
    public boolean      isActive()                       { return isActive; }
    public void         setActive(boolean a)             { this.isActive = a; }
    public List<Answer> getAnswers()                     { return answers; }
    public void         setAnswers(List<Answer> answers) { this.answers = answers; }
}
