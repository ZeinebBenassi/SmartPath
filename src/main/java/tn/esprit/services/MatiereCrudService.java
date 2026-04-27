package tn.esprit.services;

import tn.esprit.entity.Matiere;
import tn.esprit.services.MatiereService;

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

    public void create(Matiere matiere, int profId) throws SQLException {
        delegate.create(matiere, profId);
    }

    public void update(Matiere matiere) throws SQLException {
        delegate.update(matiere);
    }

    public void delete(int id) throws SQLException {
        delegate.delete(id);
    }
}
