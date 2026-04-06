package com.smartpath.service;

import com.smartpath.model.Question;
import com.smartpath.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService {
    public List<Question> getAll() throws SQLException {
        List<Question> list = new ArrayList<>();
        ResultSet rs = DBConnection.getInstance().createStatement()
                .executeQuery("SELECT * FROM question");
        while (rs.next()) {
            list.add(new Question(
                    rs.getInt("id"),
                    rs.getString("text"),
                    rs.getString("category")
            ));
        }
        return list;
    }

    public void create(Question q) throws SQLException {
        String sql = "INSERT INTO question (text, category, ordre, is_active) VALUES (?, ?, ?, 1)";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, q.getText());
        ps.setString(2, q.getCategory());
        ps.setInt(3, q.getOrdre());
        ps.executeUpdate();
    }

    public void update(Question q) throws SQLException {
        String sql = "UPDATE question SET text=?, category=? WHERE id=?";
        PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql);
        ps.setString(1, q.getText());
        ps.setString(2, q.getCategory());
        ps.setInt(3, q.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = DBConnection.getInstance()
                .prepareStatement("DELETE FROM question WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
