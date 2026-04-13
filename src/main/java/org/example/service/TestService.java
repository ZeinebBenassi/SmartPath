package org.example.service;

import org.example.entity.QcmQuestion;
import org.example.entity.QcmReponse;
import org.example.entity.Test;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestService {
    private Connection connection;

    public TestService() {
        try {
            this.connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // CRUD: Read all tests
    public List<Test> findAll() {
        List<Test> tests = new ArrayList<>();
        String query = "SELECT * FROM test";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                Test test = new Test();
                test.setId(rs.getInt("id"));
                test.setTitre(rs.getString("titre"));
                test.setContenu(rs.getString("contenu"));
                test.setDuree(rs.getInt("duree"));
                test.setMatiereId(rs.getInt("matiere_id"));
                test.setProfId(rs.getInt("prof_id"));
                tests.add(test);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tests;
    }

    // Read full test with questions and answers
    public Test findWithDetails(int testId) {
        Test test = null;
        String query = "SELECT * FROM test WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, testId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                test = new Test();
                test.setId(rs.getInt("id"));
                test.setTitre(rs.getString("titre"));
                test.setContenu(rs.getString("contenu"));
                test.setQcmQuestions(findQuestionsByTestId(testId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    private List<QcmQuestion> findQuestionsByTestId(int testId) {
        List<QcmQuestion> questions = new ArrayList<>();
        String query = "SELECT * FROM qcm_question WHERE test_id = ? ORDER BY ordre ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, testId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                QcmQuestion question = new QcmQuestion();
                question.setId(rs.getInt("id"));
                question.setTexte(rs.getString("texte"));
                question.setOrdre(rs.getInt("ordre"));
                question.setReponses(findReponsesByQuestionId(question.getId()));
                questions.add(question);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }

    private List<QcmReponse> findReponsesByQuestionId(int questionId) {
        List<QcmReponse> reponses = new ArrayList<>();
        String query = "SELECT * FROM qcm_reponse WHERE question_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                QcmReponse reponse = new QcmReponse();
                reponse.setId(rs.getInt("id"));
                reponse.setTexte(rs.getString("texte"));
                reponse.setEstCorrecte(rs.getBoolean("est_correcte"));
                reponses.add(reponse);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reponses;
    }

    // CREATE: Submit result
    public boolean submitResult(int etudiantId, int testId, float note) {
        String query = "INSERT INTO test_result (etudiant_id, test_id, note, created_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, etudiantId);
            pstmt.setInt(2, testId);
            pstmt.setFloat(3, note);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
