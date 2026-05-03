package tn.esprit.entity;

public class Grade {
    private int id;
    private String subject;
    private double score;
    private int studentId;

    public Grade() {}

    public Grade(String subject, double score, int studentId) {
        this.subject = subject;
        this.score = score;
        this.studentId = studentId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
}
