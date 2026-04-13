package org.example.entity;

import java.time.LocalDateTime;

public class TestResult {
    private int id;
    private float note;
    private int etudiantId; // Link to Etudiant
    private int testId; // Link to Test
    private LocalDateTime createdAt;

    public TestResult() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float getNote() { return note; }
    public void setNote(float note) { this.note = note; }

    public int getEtudiantId() { return etudiantId; }
    public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
