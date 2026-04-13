package tn.esprit.entity;

/**
 * Entité Answer — table `answer`.
 * Représente une réponse possible à une Question du quiz.
 * Relations : ManyToOne Question.
 */
public class Answer {

    private int    id;
    private String text;
    private int    points;
    private String trait;       // ex: "analytique", "créatif", "social"
    private int    questionId;  // FK → question.id

    public Answer() {}

    public Answer(String text, int points, String trait, int questionId) {
        this.text       = text;
        this.points     = points;
        this.trait      = trait;
        this.questionId = questionId;
    }

    public Answer(int id, String text, int points,
                  String trait, int questionId) {
        this.id         = id;
        this.text       = text;
        this.points     = points;
        this.trait      = trait;
        this.questionId = questionId;
    }


    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }

    public String getText()                        { return text; }
    public void   setText(String text)             { this.text = text; }

    public int    getPoints()                      { return points; }
    public void   setPoints(int points)            { this.points = points; }

    public String getTrait()                       { return trait; }
    public void   setTrait(String trait)           { this.trait = trait; }

    public int    getQuestionId()                  { return questionId; }
    public void   setQuestionId(int questionId)    { this.questionId = questionId; }

    @Override
    public String toString() {
        return "Answer{id=" + id + ", text='" + text
                + "', points=" + points
                + ", trait='" + trait + "'}";
    }
}
