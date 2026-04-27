package tn.esprit.services.feature_cours_et_quiz;

import tn.esprit.entity.feature_cours_et_quiz.Matiere;
import java.sql.SQLException;
import java.util.List;

public class MatiereCrudService {
    private final MatiereService delegate = new MatiereService();

    public List<Matiere> getAll() throws SQLException {
        return delegate.getAll();
    }

    public void create(Matiere matiere) throws SQLException {
        delegate.create(matiere);
    }

    public void update(Matiere matiere) throws SQLException {
        delegate.update(matiere);
    }

    public void delete(int id) throws SQLException {
        delegate.delete(id);
    }
}
