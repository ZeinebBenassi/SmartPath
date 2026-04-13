package com.smartpath.service.feature_cours_et_quiz;

import com.smartpath.model.Quiz;
import com.smartpath.service.QuizService;

import java.sql.SQLException;
import java.util.List;

public class QuizCrudService {
    private final QuizService delegate = new QuizService();

    public List<Quiz> getAll() throws SQLException {
        return delegate.getAll();
    }

    public void create(Quiz quiz) throws SQLException {
        delegate.create(quiz);
    }

    public int createAndReturnId(Quiz quiz) throws SQLException {
        return delegate.createAndReturnId(quiz, null);
    }

    public int createAndReturnId(Quiz quiz, int profId) throws SQLException {
        return delegate.createAndReturnId(quiz, profId);
    }

    public void update(Quiz quiz) throws SQLException {
        delegate.update(quiz);
    }

    public void delete(int id) throws SQLException {
        delegate.delete(id);
    }
}
