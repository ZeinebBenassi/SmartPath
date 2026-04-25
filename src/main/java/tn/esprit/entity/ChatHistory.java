package tn.esprit.entity;

import java.time.LocalDateTime;

/**
 * Entité ChatHistory — représente une interaction chatbot stockée en MySQL.
 *
 * Table : chat_history
 * Colonnes : id, user_id, role, question, answer, created_at
 */
public class ChatHistory {

    private int           id;
    private int           userId;
    private String        role;       // "etudiant", "prof", "admin"
    private String        question;
    private String        answer;
    private LocalDateTime createdAt;

    public ChatHistory() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatHistory(int userId, String role, String question, String answer) {
        this();
        this.userId   = userId;
        this.role     = role;
        this.question = question;
        this.answer   = answer;
    }

    public int           getId()                        { return id; }
    public void          setId(int id)                  { this.id = id; }
    public int           getUserId()                    { return userId; }
    public void          setUserId(int userId)          { this.userId = userId; }
    public String        getRole()                      { return role; }
    public void          setRole(String role)           { this.role = role; }
    public String        getQuestion()                  { return question; }
    public void          setQuestion(String question)   { this.question = question; }
    public String        getAnswer()                    { return answer; }
    public void          setAnswer(String answer)       { this.answer = answer; }
    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void          setCreatedAt(LocalDateTime t)  { this.createdAt = t; }

    @Override
    public String toString() {
        return "ChatHistory{id=" + id + ", userId=" + userId +
               ", role='" + role + "', createdAt=" + createdAt + "}";
    }
}
