package com.smartpath.service.feature_cours_et_quiz;

import com.smartpath.model.feature_cours_et_quiz.User;
import com.smartpath.util.feature_cours_et_quiz.RoleUtils;

import java.sql.SQLException;

public class AuthService {
    private final UserService userService = new UserService();

    public User login(String email, String passwordPlain) throws SQLException {
        userService.ensureDefaultUsers();

        UserService.AuthRow row = userService.findAuthRowByEmail(email);
        if (row == null) return null;

        String stored = row.passwordHash();
        boolean ok;

        if (PasswordHasher.looksLikeSha256Hex(stored)) {
            ok = PasswordHasher.sha256Hex(passwordPlain).equalsIgnoreCase(stored);
        } else {
            // Backward compatible: accept plaintext if DB contains plaintext
            ok = passwordPlain.equals(stored);
        }

        if (!ok) return null;

        var user = new User(row.id(), row.fullName(), row.email(), row.role());
        user.setRole(RoleUtils.display(RoleUtils.normalize(row.role())));
        return user;
    }
}
