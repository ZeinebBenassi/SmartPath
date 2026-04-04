package tn.esprit.entity;

/**
 * Entité Question — table `question`.
 */
public class Question {

    private int     id;
    private String  text;
    private String  category;
    private int     ordre;
    private boolean isActive = true;

    public Question() {}

    public Question(String text, String category, int ordre) {
        this.text     = text;
        this.category = category;
        this.ordre    = ordre;
        this.isActive = true;
    }

    public Question(int id, String text, String category, int ordre, boolean isActive) {
        this.id       = id;
        this.text     = text;
        this.category = category;
        this.ordre    = ordre;
        this.isActive = isActive;
    }

    public int     getId()                       { return id; }
    public void    setId(int id)                 { this.id = id; }
    public String  getText()                     { return text; }
    public void    setText(String text)          { this.text = text; }
    public String  getCategory()                 { return category; }
    public void    setCategory(String category)  { this.category = category; }
    public int     getOrdre()                    { return ordre; }
    public void    setOrdre(int ordre)           { this.ordre = ordre; }
    public boolean isActive()                    { return isActive; }
    public void    setActive(boolean isActive)   { this.isActive = isActive; }

    @Override
    public String toString() {
        return "Question{id=" + id + ", text='" + text + "', category='" + category + "'}";
    }
}
