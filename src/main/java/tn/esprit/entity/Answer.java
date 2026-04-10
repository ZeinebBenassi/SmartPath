package tn.esprit.entity;

public class Answer {

    private int    id;
    private String text;
    private int    points;
    private String trait;
    private int    questionId;

    public Answer() {}

    public Answer(int id, String text, int points, String trait, int questionId) {
        this.id = id; this.text = text; this.points = points;
        this.trait = trait; this.questionId = questionId;
    }

    public int    getId()                       { return id; }
    public void   setId(int id)                 { this.id = id; }
    public String getText()                     { return text; }
    public void   setText(String text)          { this.text = text; }
    public int    getPoints()                   { return points; }
    public void   setPoints(int points)         { this.points = points; }
    public String getTrait()                    { return trait; }
    public void   setTrait(String trait)        { this.trait = trait; }
    public int    getQuestionId()               { return questionId; }
    public void   setQuestionId(int qid)        { this.questionId = qid; }
}
