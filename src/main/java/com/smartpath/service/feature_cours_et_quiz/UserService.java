package com.smartpath.service.feature_cours_et_quiz;

import com.smartpath.model.feature_cours_et_quiz.User;
import com.smartpath.util.DBConnection;

import java.sql.*;
import java.util.Locale;

public class UserService {

    private static volatile Boolean usersHasCreatedAt;

    public void ensureUsersTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    full_name VARCHAR(120) NOT NULL,
                    email VARCHAR(180) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(40) NOT NULL DEFAULT 'ETUDIANT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement st = DBConnection.getInstance().createStatement()) {
            st.execute(sql);
        }
    }

    public void ensureDefaultUsers() throws SQLException {
        ensureUsersTable();

        // Development convenience: ensure these accounts can always log in.
        // We do NOT rely on UNIQUE(email) existing (some DBs may already have a users table).
        // Instead: update by email; if no row exists, insert.
        ensureUserCredentials("Admin SmartPath", "admin@smartpath.tn", "admin", "ADMIN");
        ensureUserCredentials("Prof SmartPath", "prof@smartpath.tn", "prof", "PROF");
        ensureUserCredentials("Etudiant SmartPath", "etudiant@smartpath.tn", "etudiant", "ETUDIANT");
    }

    private void ensureUserCredentials(String fullName, String email, String passwordPlain, String role) throws SQLException {
        String updateSql = "UPDATE users SET full_name=?, password_hash=?, role=? WHERE email=?";
        int updated;
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(updateSql)) {
            ps.setString(1, fullName);
            ps.setString(2, PasswordHasher.sha256Hex(passwordPlain));
            ps.setString(3, role == null || role.isBlank() ? "ETUDIANT" : role);
            ps.setString(4, email);
            updated = ps.executeUpdate();
        }

        if (updated == 0) {
            createUser(fullName, email, passwordPlain, role);
        }
    }

    public User findByEmail(String email) throws SQLException {
        ensureUsersTable();
        String sql = "SELECT id, full_name, email, role FROM users WHERE email=?";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role")
                );
            }
        }
    }

    public void createUser(String fullName, String email, String passwordPlain, String role) throws SQLException {
        ensureUsersTable();

        Connection conn = DBConnection.getInstance();
        boolean hasCreatedAt = tableHasColumn(conn, "users", "created_at");

        String sql;
        if (hasCreatedAt) {
            sql = "INSERT INTO users (full_name, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, PasswordHasher.sha256Hex(passwordPlain));
            ps.setString(4, role == null || role.isBlank() ? "ETUDIANT" : role);
            if (hasCreatedAt) {
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            }
            ps.executeUpdate();
        }
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        if ("users".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            Boolean cached = usersHasCreatedAt;
            if (cached != null) return cached;
        }

        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        boolean found = false;

        try (ResultSet rs = meta.getColumns(catalog, null, table, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name != null && name.toLowerCase(Locale.ROOT).equals(column.toLowerCase(Locale.ROOT))) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            try (ResultSet rs = meta.getColumns(catalog, null, table.toUpperCase(Locale.ROOT), null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null && name.toLowerCase(Locale.ROOT).equals(column.toLowerCase(Locale.ROOT))) {
                        found = true;
                        break;
                    }
                }
            }
        }

        if ("users".equalsIgnoreCase(table) && "created_at".equalsIgnoreCase(column)) {
            usersHasCreatedAt = found;
        }
        return found;
    }

    public AuthRow findAuthRowByEmail(String email) throws SQLException {
        ensureUsersTable();
        String sql = "SELECT id, full_name, email, role, password_hash FROM users WHERE email=? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = DBConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new AuthRow(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("password_hash")
                );
            }
        }
    }

    public record AuthRow(int id, String fullName, String email, String role, String passwordHash) {
    }
}
