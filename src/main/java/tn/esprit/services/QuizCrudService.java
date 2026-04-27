package tn.esprit.services;

import tn.esprit.entity.Test;
import tn.esprit.services.EduTestService;

import java.sql.SQLException;
import java.util.List;

public class QuizCrudService {
    private final EduTestService delegate = new EduTestService();

    public List<Test> getAll() throws SQLException {
        return delegate.getAll();
    }

    public void create(Test quiz) throws SQLException {
        delegate.create(quiz);
    }

    public int createAndReturnId(Test quiz) throws SQLException {
        return delegate.createAndReturnId(quiz, null);
    }

    public int createAndReturnId(Test quiz, int profId) throws SQLException {
        return delegate.createAndReturnId(quiz, profId);
    }

    public void update(Test quiz) throws SQLException {
        delegate.update(quiz);
    }

    public void delete(int id) throws SQLException {
        delegate.delete(id);
    }
}
