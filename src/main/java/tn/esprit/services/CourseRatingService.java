package tn.esprit.services;

import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CourseRatingService {

    public static class FeedbackSummary {
        public final double average;
        public final int count;
        public final String latestComment;

        public FeedbackSummary(double average, int count, String latestComment) {
            this.average = average;
            this.count = count;
            this.latestComment = latestComment;
        }
    }

    private final Connection cnx;

    public CourseRatingService() {
        this.cnx = MyDatabase.getInstance().getConnection();
    }

    public void addRating(int courseId, int studentId, int stars, String comment) throws SQLException {
        String sql = "INSERT INTO course_ratings (course_id, student_id, stars, comment) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE stars = VALUES(stars), comment = VALUES(comment)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, studentId);
            ps.setInt(3, stars);
            ps.setString(4, comment);
            ps.executeUpdate();
        }
    }

    public double getAverageRating(int courseId) throws SQLException {
        String sql = "SELECT AVG(stars) FROM course_ratings WHERE course_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    public int getStudentRating(int courseId, int studentId) throws SQLException {
        String sql = "SELECT stars FROM course_ratings WHERE course_id = ? AND student_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stars");
                }
            }
        }
        return 0; // Return 0 if no rating found
    }

    public FeedbackSummary getFeedbackSummary(int courseId) throws SQLException {
        String statsSql = "SELECT COUNT(*) AS total_count, AVG(stars) AS average_stars FROM course_ratings WHERE course_id = ?";
        String latestSql = "SELECT comment FROM course_ratings WHERE course_id = ? AND comment IS NOT NULL AND comment <> '' ORDER BY id DESC LIMIT 1";

        double average = 0.0;
        int count = 0;
        String latestComment = null;

        try (PreparedStatement ps = cnx.prepareStatement(statsSql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("total_count");
                    average = rs.getDouble("average_stars");
                }
            }
        }

        if (count > 0) {
            try (PreparedStatement ps = cnx.prepareStatement(latestSql)) {
                ps.setInt(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        latestComment = rs.getString("comment");
                    }
                }
            }
        }

        return new FeedbackSummary(average, count, latestComment);
    }
}
